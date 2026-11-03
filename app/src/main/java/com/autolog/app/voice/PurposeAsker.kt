package com.autolog.app.voice

import com.autolog.app.trip.Trip
import com.autolog.app.trip.TripDao
import com.autolog.app.trip.TripType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 운행이 끝나면(기어 P) 목적을 음성으로 묻고, 답을 운행기록부 비고 칸(purpose)에 저장한다.
 * 답으로 업무 구분도 정한다. 구분이 이미 정해진 운행(화면에서 고른 것)은 목적만 받는다.
 * 주차한 뒤에만 묻기 때문에 운전 중 주의를 빼앗지 않는다.
 * 한 번에 한 운행만 묻고, 못 알아들으면 한 번 더 묻는다. 끝내 답이 없으면 비워 두고 기록 화면에서 다시 물을 수 있다.
 */
class PurposeAsker(private val dao: TripDao, private val voice: VoiceIO) {

    data class Asking(val tripId: Long, val listening: Boolean)

    private val mutex = Mutex()
    private val _asking = MutableStateFlow<Asking?>(null)
    val asking: StateFlow<Asking?> = _asking.asStateFlow()

    /** 운행이 끝나면 부른다. 집↔회사로 이미 출퇴근이 정해졌으면 묻지 않고 알려만 준다. */
    suspend fun onTripEnded(trip: Trip) {
        if (trip.type == TripType.COMMUTE) {
            mutex.withLock { voice.speak("${trip.purpose ?: TripType.COMMUTE.label} 운행으로 기록했어요.") }
        } else {
            ask(trip)
        }
    }

    suspend fun ask(trip: Trip) = mutex.withLock {
        try {
            repeat(ATTEMPTS) { attempt ->
                _asking.value = Asking(trip.id, listening = false)
                voice.speak(if (attempt == 0) QUESTION else "잘 못 들었어요. 한 번 더 말씀해 주세요.")
                _asking.value = Asking(trip.id, listening = true)
                val answer = voice.listen()?.trim()
                if (!answer.isNullOrEmpty()) {
                    dao.setPurpose(trip.id, answer)
                    val type = trip.type ?: TripType.fromPurpose(answer).also { dao.setType(trip.id, it) }
                    _asking.value = Asking(trip.id, listening = false)
                    voice.speak("$answer, ${type.label} 운행으로 기록했어요.")
                    return@withLock
                }
            }
            voice.speak("목적은 나중에 기록 화면에서 남길 수 있어요.")
        } finally {
            _asking.value = null
        }
    }

    private companion object {
        const val ATTEMPTS = 2

        // 목적만 물으면 장보기도 업무로 들어가므로, 개인 용무는 그렇다고 말해 달라고 함께 묻는다.
        const val QUESTION = "운행을 기록했어요. 이번 운행 목적을 말씀해 주세요. 개인 용무였다면 개인이라고 해 주세요."
    }
}

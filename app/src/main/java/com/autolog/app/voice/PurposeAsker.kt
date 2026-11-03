package com.autolog.app.voice

import com.autolog.app.trip.Trip
import com.autolog.app.trip.TripDao
import com.autolog.app.trip.distanceKm
import com.autolog.app.trip.savePurpose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 운행이 끝나면(기어 P) 목적을 음성으로 묻고, 답을 운행기록부 비고 칸(purpose)에 저장한다.
 * 답으로 업무 구분도 정한다. 구분이 이미 정해진 운행(화면에서 고른 것)은 목적만 받는다.
 * 주차한 뒤에만 묻기 때문에 운전 중 주의를 빼앗지 않는다.
 * 한 번에 한 운행만 묻고, 못 알아들으면 한 번 더 묻는다. 끝내 답이 없거나 "그만"·"나중에"라고 하면 비워 두고
 * 기록 화면에서 다시 물을 수 있다.
 */
class PurposeAsker(private val dao: TripDao, private val voice: VoiceIO) {

    data class Asking(val tripId: Long, val listening: Boolean)

    private val mutex = Mutex()
    private val _asking = MutableStateFlow<Asking?>(null)
    val asking: StateFlow<Asking?> = _asking.asStateFlow()

    /** 운행이 끝나면 부른다. 집↔회사로 출퇴근이 정해져 목적('출근'·'퇴근')이 이미 있으면 묻지 않고 알려만 준다. */
    suspend fun onTripEnded(trip: Trip) {
        mutex.withLock {
            if (trip.purpose != null) voice.speak("${trip.purpose} 운행으로 기록했어요.") else askLocked(trip, QUESTION)
        }
    }

    /** 화면에서 누른 운행의 목적을 묻는다. 목적이 있는 운행이면 고친다. 지난 운행일 수 있어 어느 운행인지 함께 말한다. */
    suspend fun ask(trip: Trip) {
        mutex.withLock { askLocked(trip, "${describe(trip)} 운행의 목적을 말씀해 주세요. 개인 용무였다면 개인이라고 해 주세요.") }
    }

    /** 목적이 빈 운행들을 차례로 묻는다. 답이 없거나 그만하라고 하면 거기서 멈춘다. */
    suspend fun askAll(trips: List<Trip>) = mutex.withLock {
        voice.speak("목적이 없는 운행 ${trips.size}건을 차례로 여쭤볼게요. 개인 용무였다면 개인, 그만하려면 그만이라고 해 주세요.")
        for (trip in trips) {
            // 여러 건을 물을 때는 어느 운행인지 알아야 답할 수 있다.
            if (!askLocked(trip, "${describe(trip)} 운행의 목적을 말씀해 주세요.")) break
        }
    }

    /** 답을 저장했으면 true. */
    private suspend fun askLocked(trip: Trip, question: String): Boolean {
        try {
            repeat(ATTEMPTS) { attempt ->
                _asking.value = Asking(trip.id, listening = false)
                voice.speak(if (attempt == 0) question else "잘 못 들었어요. 한 번 더 말씀해 주세요.")
                _asking.value = Asking(trip.id, listening = true)
                val answer = voice.listen()?.trim()
                if (answer.isNullOrEmpty()) return@repeat
                _asking.value = Asking(trip.id, listening = false)
                if (isStop(answer)) {
                    voice.speak(LATER)
                    return false
                }
                val type = dao.savePurpose(trip, answer)
                voice.speak("$answer, ${type.label} 운행으로 기록했어요.")
                return true
            }
            voice.speak(LATER)
            return false
        } finally {
            _asking.value = null
        }
    }

    private companion object {
        const val ATTEMPTS = 2
        const val LATER = "목적은 나중에 기록 화면에서 남길 수 있어요."

        // 목적만 물으면 장보기도 업무로 들어가므로, 개인 용무는 그렇다고 말해 달라고 함께 묻는다.
        const val QUESTION = "운행을 기록했어요. 이번 운행 목적을 말씀해 주세요. 개인 용무였다면 개인이라고 해 주세요."

        // "그만할래", "나중에 할게"처럼 끝이 붙어도 멈춘다. 목적으로 저장하지 않는다.
        val STOP_WORDS = listOf("그만", "나중에")

        fun isStop(answer: String) = answer.replace(" ", "").let { said -> STOP_WORDS.any { said.startsWith(it) } }

        // 묻기는 mutex 안에서 한 번에 하나씩만 하므로 형식 객체를 같이 써도 된다.
        val startFormat = SimpleDateFormat("M월 d일 a h시 m분", Locale.KOREA)
        val kmFormat = DecimalFormat("#,##0.#")

        // 소리 내어 읽기 좋게 쓴다. 예: "11월 3일 오전 10시 16분에 출발한 8킬로미터"
        fun describe(trip: Trip) =
            "${startFormat.format(Date(trip.startedAt))}에 출발한 ${kmFormat.format(trip.distanceKm ?: 0f)}킬로미터"
    }
}

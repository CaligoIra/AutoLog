package com.autolog.app.trip

import ai.pleos.playground.vehicle.constant.control.VehicleGear
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 기어 상태로 운행 구간을 자르는 상태 기계. 상태는 DB의 열린 운행(endedAt IS NULL)에 둔다.
 * P가 아닌 기어 + 계기판 값이 있으면 운행 시작, P로 돌아오면 운행 종료.
 * 계기판 값이 기어 변경보다 늦게 와도 다음 update()에서 처리된다.
 *
 * 신호 대기, 드라이브스루, 주차 자리 옮기기처럼 잠깐 P에 넣었다가 다시 출발하면 방금 끝낸 운행을 이어 간다.
 * 목적을 답하지 않았고 끝난 지 RESUME_WINDOW_MS가 안 지났을 때만이다. 목적을 답했으면 거기서 볼일을 본 것이라 새 운행으로 나눈다.
 *
 * 부팅 직후 계기판이 실제 값 대신 0을 먼저 보내는 경우가 있어(에뮬레이터에서 확인) 0은 "아직 못 읽음"으로 본다.
 * 누적거리는 줄지 않으므로, 열린 운행의 출발값보다 작은 값도 버리고 다음 값을 기다린다.
 */
class TripRecorder(private val dao: TripDao) {

    private val mutex = Mutex()

    // 운전 중에는 계기판 값이 자주 바뀐다. 그때마다 DB를 읽지 않도록 열린 운행을 들고 있는다.
    // 운행을 열고 닫는 것은 여기서만 하므로 DB와 어긋나지 않는다. 앱이 다시 뜨면 처음 한 번 DB에서 읽는다.
    private var loaded = false
    private var open: Trip? = null

    /** 이번 값으로 운행이 시작·재개되거나 끝났으면 알려준다(출발·도착 장소를 확인하고 목적을 물을 때 쓴다). */
    suspend fun update(gear: VehicleGear, odometerKm: Float?, now: Long = System.currentTimeMillis()): TripEvent? {
        if (odometerKm == null || odometerKm <= 0f) return null
        return mutex.withLock {
            val trip = openTrip()
            when {
                trip == null && gear.isMoving() -> start(odometerKm, now)
                trip != null && gear == VehicleGear.GEAR_PARK -> end(trip, odometerKm, now)
                else -> null
            }
        }
    }

    suspend fun onDestinationArrived(destination: String) = mutex.withLock {
        openTrip()?.let { dao.setDestination(it.id, destination) }
    }

    private suspend fun openTrip(): Trip? {
        if (!loaded) {
            open = dao.openTrip()
            loaded = true
        }
        return open
    }

    private suspend fun start(odometerKm: Float, now: Long): TripEvent {
        val last = dao.lastFinishedTrip()
        if (last != null && last.canResume(now)) {
            // 집·회사 도착으로 정했던 출퇴근은 아직 도착한 게 아니므로 지운다. 진짜 도착 때 다시 정한다.
            val resumed = if (last.isAutoCommute()) last.copy(type = null, purpose = null) else last
            dao.reopen(last.id, resumed.type, resumed.purpose)
            return TripEvent.Resumed(resumed.copy(endedAt = null, endKm = null, endPlace = null).also { open = it })
        }
        val trip = Trip(startedAt = now, startKm = odometerKm)
        return TripEvent.Started(trip.copy(id = dao.insert(trip)).also { open = it })
    }

    private suspend fun end(trip: Trip, odometerKm: Float, now: Long): TripEvent? = when {
        // 주차장에서 D→P만 한 것처럼 거리가 0이면 운행기록부에 남길 게 없어 지운다.
        odometerKm == trip.startKm -> {
            dao.delete(trip)
            open = null
            null
        }

        odometerKm > trip.startKm -> {
            dao.finish(trip.id, now, odometerKm)
            open = null
            TripEvent.Ended(trip.copy(endedAt = now, endKm = odometerKm))
        }

        else -> null
    }

    // 집·회사 도착으로 자동으로 정한 출퇴근. 운전자가 답한 목적이 아니다.
    private fun Trip.isAutoCommute() = type == TripType.COMMUTE && endPlace != null

    private fun Trip.canResume(now: Long) =
        endedAt != null && now - endedAt <= RESUME_WINDOW_MS && (purpose == null || isAutoCommute())

    private companion object {
        // 신호 대기, 드라이브스루, 셀프 주유가 대개 이 안에 끝난다.
        const val RESUME_WINDOW_MS = 5 * 60_000L
    }
}

/** P도 아니고 모르는 상태도 아니면 차가 움직일 수 있는 기어다(D·R·N). */
fun VehicleGear.isMoving() = this != VehicleGear.GEAR_PARK && this != VehicleGear.GEAR_UNKNOWN

sealed interface TripEvent {
    val trip: Trip

    data class Started(override val trip: Trip) : TripEvent
    data class Resumed(override val trip: Trip) : TripEvent
    data class Ended(override val trip: Trip) : TripEvent
}

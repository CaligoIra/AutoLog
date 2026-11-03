package com.autolog.app.trip

import ai.pleos.playground.vehicle.constant.control.VehicleGear
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 기어 상태로 운행 구간을 자르는 상태 기계. 상태는 DB의 열린 운행(endedAt IS NULL)에 둔다.
 * P가 아닌 기어 + 계기판 값이 있으면 운행 시작, P로 돌아오면 운행 종료.
 * 계기판 값이 기어 변경보다 늦게 와도 다음 update()에서 처리된다.
 *
 * 부팅 직후 계기판이 실제 값 대신 0을 먼저 보내는 경우가 있어(에뮬레이터에서 확인) 0은 "아직 못 읽음"으로 본다.
 * 누적거리는 줄지 않으므로, 열린 운행의 출발값보다 작은 값도 버리고 다음 값을 기다린다.
 */
class TripRecorder(private val dao: TripDao) {

    private val mutex = Mutex()

    /** 이번 값으로 운행이 시작되거나 끝났으면 알려준다(출발·도착 장소를 확인하고 목적을 물을 때 쓴다). */
    suspend fun update(gear: VehicleGear, odometerKm: Float?, now: Long = System.currentTimeMillis()): TripEvent? {
        if (odometerKm == null || odometerKm <= 0f) return null
        return mutex.withLock {
            val open = dao.openTrip()
            when {
                open == null && gear.isMoving() -> {
                    val trip = Trip(startedAt = now, startKm = odometerKm)
                    TripEvent.Started(trip.copy(id = dao.insert(trip)))
                }

                // 주차장에서 D→P만 한 것처럼 거리가 0이면 운행기록부에 남길 게 없어 지운다.
                open != null && gear == VehicleGear.GEAR_PARK && odometerKm == open.startKm -> {
                    dao.delete(open)
                    null
                }

                open != null && gear == VehicleGear.GEAR_PARK && odometerKm > open.startKm ->
                    TripEvent.Ended(open.copy(endedAt = now, endKm = odometerKm).also { dao.update(it) })

                else -> null
            }
        }
    }

    suspend fun onDestinationArrived(destination: String) = mutex.withLock {
        dao.openTrip()?.let { dao.update(it.copy(destination = destination)) }
    }

    private fun VehicleGear.isMoving() =
        this != VehicleGear.GEAR_PARK && this != VehicleGear.GEAR_UNKNOWN
}

sealed interface TripEvent {
    val trip: Trip

    data class Started(override val trip: Trip) : TripEvent
    data class Ended(override val trip: Trip) : TripEvent
}

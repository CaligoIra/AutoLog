package com.autolog.app.vehicle

import android.content.Context
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.DistanceDrivenListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pleos Vehicle SDK 래퍼. 계기판 누적거리(Odometer)와 기어(Gear) 상태를 Flow로 내보낸다.
 * SDK 콜백은 IPC 스레드에서 오므로 값은 StateFlow로만 넘긴다.
 */
class VehicleMonitor(context: Context) {

    private val vehicle = Vehicle(context.applicationContext)

    private val _odometerKm = MutableStateFlow<Float?>(null)
    val odometerKm: StateFlow<Float?> = _odometerKm.asStateFlow()

    private val _gear = MutableStateFlow(VehicleGear.GEAR_UNKNOWN)
    val gear: StateFlow<VehicleGear> = _gear.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val distanceListener = object : DistanceDrivenListener {
        override fun onDistanceDrivenUpdated(value: Float?) {
            if (value != null) _odometerKm.value = value
        }

        override fun onFailed(e: Exception) = report("odometer", e)
    }

    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) {
            if (gear != null) _gear.value = gear
        }

        override fun onFailed(e: Exception) = report("gear", e)
    }

    private var started = false

    @Synchronized
    fun start() {
        if (started) return
        started = true
        vehicle.initialize()

        val odometer = vehicle.getOdometer()
        odometer.getCurrentDistanceDriven({ km -> _odometerKm.value = km }, { e -> report("odometer", e) })
        odometer.registerDistanceDriven(distanceListener)

        val gear = vehicle.getGear()
        gear.getCurrentGearSelectionState({ g -> if (g != null) _gear.value = g }, { e -> report("gear", e) })
        gear.registerGearSelection(gearListener)
    }

    @Synchronized
    fun stop() {
        if (!started) return
        started = false
        vehicle.getOdometer().unregisterDistanceDriven(distanceListener)
        vehicle.getGear().unregisterGearSelection(gearListener)
        vehicle.release()
    }

    private fun report(source: String, e: Exception) {
        Log.w(TAG, "$source failed", e)
        _lastError.value = "$source: ${e.message ?: e::class.simpleName}"
    }

    private companion object {
        const val TAG = "VehicleMonitor"
    }
}

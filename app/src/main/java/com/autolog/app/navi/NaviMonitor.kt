package com.autolog.app.navi

import android.content.Context
import android.util.Log
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Pleos NaviHelper 래퍼. 목적지 도착 이벤트를 운행 기록의 도착지로 쓴다. */
class NaviMonitor(context: Context) {

    private val helper = NaviHelper(context.applicationContext)

    private val _arrivals = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val arrivals: SharedFlow<String> = _arrivals.asSharedFlow()

    private val listener = object : NaviHelperEventListener {
        override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) {
            Log.i(TAG, "destination arrived: $destinationArrivedInfo")
            _arrivals.tryEmit(destinationArrivedInfo.toString())
        }
    }

    private var started = false

    @Synchronized
    fun start() {
        if (started) return
        started = true
        helper.initialize(
            { helper.addListener(listener) },
            { t -> Log.w(TAG, "NaviHelper init failed", t) },
        )
    }

    @Synchronized
    fun stop() {
        if (!started) return
        started = false
        helper.removeListener(listener)
        helper.release()
    }

    private companion object {
        const val TAG = "NaviMonitor"
    }
}

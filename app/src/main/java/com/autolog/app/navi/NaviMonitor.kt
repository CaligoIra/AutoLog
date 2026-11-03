package com.autolog.app.navi

import android.content.Context
import android.location.Location
import android.util.Log
import ai.pleos.playground.navi.constants.BookmarkType
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.BookmarkInfo
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import com.autolog.app.trip.Place
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Pleos NaviHelper 래퍼. 목적지 도착 이벤트를 운행 기록의 도착지로 쓰고,
 * 즐겨찾기의 집·회사와 차의 현재 위치를 비교해 출퇴근 운행을 가린다.
 */
class NaviMonitor(context: Context) {

    private val helper = NaviHelper(context.applicationContext)

    private val _arrivals = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val arrivals: SharedFlow<String> = _arrivals.asSharedFlow()

    private val savedPlaces = MutableStateFlow<Map<Place, BookmarkInfo>>(emptyMap())
    private val locations = MutableSharedFlow<CurrentLocationInfo>(extraBufferCapacity = 1)

    private val listener = object : NaviHelperEventListener {
        override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) {
            Log.i(TAG, "destination arrived: $destinationArrivedInfo")
            _arrivals.tryEmit(destinationArrivedInfo.name)
        }

        override fun onBookmarkInfo(bookmarkInfoList: List<BookmarkInfo>) {
            // 집·회사를 등록하지 않아도 빈 칸(좌표 0, 0)이 온다(에뮬레이터에서 확인). 그런 칸은 없는 것으로 본다.
            savedPlaces.value = bookmarkInfoList
                .filter { it.latitude != 0.0 || it.longitude != 0.0 }
                .mapNotNull { b -> b.category.toPlace()?.let { it to b } }
                .toMap()
            Log.i(TAG, "saved places: ${savedPlaces.value.keys}")
        }

        override fun onBookMarkChanged() {
            requestSavedPlaces()
        }

        override fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo) {
            locations.tryEmit(currentLocationInfo)
        }

        override fun onError(errorCode: NaviErrorCode) {
            Log.w(TAG, "navi error $errorCode")
        }
    }

    private var started = false

    @Synchronized
    fun start() {
        if (started) return
        started = true
        helper.initialize(
            {
                helper.addListener(listener)
                requestSavedPlaces()
            },
            { t -> Log.w(TAG, "NaviHelper init failed", t) },
        )
    }

    // 내비 쪽 오류(권한 등)로 기록 서비스까지 죽지 않게 한다. 즐겨찾기를 못 받으면 출퇴근 자동 구분만 빠진다.
    private fun requestSavedPlaces() {
        runCatching { helper.getBookmarkInfo() }.onFailure { Log.w(TAG, "bookmark request failed", it) }
    }

    @Synchronized
    fun stop() {
        if (!started) return
        started = false
        helper.removeListener(listener)
        helper.release()
    }

    /** 차가 지금 즐겨찾기의 집이나 회사 근처면 그 장소. 아니거나, 즐겨찾기가 없거나, 내비가 답하지 않으면 null. */
    suspend fun currentPlace(): Place? {
        val saved = savedPlaces.value
        if (saved.isEmpty()) return null
        val here = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            locations.onSubscription { runCatching { helper.getCurrentLocationInfo() } }.first()
        } ?: return null
        val nearest = saved.mapValues { (_, b) -> distanceMeters(here.latitude, here.longitude, b.latitude, b.longitude) }
            .minByOrNull { it.value }
        // 좌표는 남기지 않고 가장 가까운 장소까지의 거리만 남긴다.
        Log.d(TAG, "nearest saved place $nearest")
        return nearest?.takeIf { it.value <= NEAR_METERS }?.key
    }

    private fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float =
        FloatArray(1).also { Location.distanceBetween(lat1, lng1, lat2, lng2, it) }[0]

    private fun BookmarkType.toPlace(): Place? = when (this) {
        BookmarkType.HOME -> Place.HOME
        BookmarkType.OFFICE -> Place.OFFICE
        else -> null
    }

    private companion object {
        const val TAG = "NaviMonitor"
        const val LOCATION_TIMEOUT_MS = 5_000L

        // 아파트 단지나 회사 주차장은 즐겨찾기 좌표에서 수백 m 떨어져 있을 수 있다.
        const val NEAR_METERS = 300f
    }
}

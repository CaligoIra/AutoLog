package com.autolog.app.navi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.autolog.app.trip.Place
import kotlinx.coroutines.channels.Channel

/**
 * 디버그 빌드 전용. 에뮬레이터의 내비 현재 위치는 한곳에 고정되어 있어(adb emu geo fix도 따르지 않음) 집→회사 운행을 만들 수 없다.
 * 그래서 adb로 미리 보낸 장소(HOME, OFFICE, 그 밖의 값은 집·회사가 아닌 곳)를 다음 위치 확인 결과로 쓴다.
 * 보낸 장소가 없으면 실제 내비에 묻는다.
 *
 *   adb shell am broadcast --user 10 -a com.autolog.app.debug.PLACE -p com.autolog.app --es place HOME
 */
fun createPlaceSource(context: Context, navi: NaviMonitor): suspend () -> Place? {
    val sent = Channel<String>(Channel.CONFLATED)
    ContextCompat.registerReceiver(
        context.applicationContext,
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                intent.getStringExtra("place")?.let { sent.trySend(it) }
            }
        },
        IntentFilter(ACTION_PLACE),
        ContextCompat.RECEIVER_EXPORTED,
    )
    return {
        when (val place = sent.tryReceive().getOrNull()) {
            null -> navi.currentPlace()
            else -> Place.entries.firstOrNull { it.name == place }
        }
    }
}

private const val ACTION_PLACE = "com.autolog.app.debug.PLACE"

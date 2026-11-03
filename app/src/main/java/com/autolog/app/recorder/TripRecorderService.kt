package com.autolog.app.recorder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.autolog.app.MainActivity
import com.autolog.app.R
import com.autolog.app.autoLogApp
import com.autolog.app.trip.TripEvent
import com.autolog.app.trip.isMoving
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * 화면을 열지 않아도 시동(부팅)부터 운행을 기록하는 포그라운드 서비스.
 * 기어·계기판 변화를 TripRecorder에 넘기고, 결과는 Room에 남는다. 운행이 끝나면 목적을 음성으로 묻는다.
 *
 * Android 14는 앱 화면이 보이지 않을 때(부팅 직후 등) 마이크를 쓰는 서비스로 바꾸는 것을 막는다.
 * 그래서 처음에는 기록만 하는 유형으로 시작하고, 앱 화면이 보일 때 마이크 유형을 더한다.
 * 한 번 더하면 화면이 뒤로 가도 이 서비스가 살아 있는 동안 주차 후 목적을 물을 수 있다.
 */
class TripRecorderService : LifecycleService() {

    private var micEnabled = false

    override fun onCreate() {
        super.onCreate()
        goForeground()

        val app = autoLogApp
        app.vehicle.start()
        app.navi.start()
        // 장소 확인 준비를 첫 운행 전에 해 둔다(디버그 빌드는 이때부터 adb로 보낸 장소를 받는다).
        val classifier = app.classifier

        lifecycleScope.launch {
            // 운행이 끝난 뒤 도착 장소를 확인하고 목적을 묻는 일. 위치를 확인하고 묻는 동안에도 기어·계기판 기록은 계속돼야 하므로 따로 띄운다.
            var settling: Job? = null
            combine(app.vehicle.gear, app.vehicle.odometerKm) { gear, km -> gear to km }
                .collect { (gear, km) ->
                    // 다시 출발하면 묻기를 멈춘다(운전 중에는 묻지 않는다). 다 멈춘 뒤에 운행을 이어 가야 이어 간 운행에 도착 장소가 남지 않는다.
                    if (gear.isMoving()) settling?.cancelAndJoin()
                    when (val event = app.recorder.update(gear, km)) {
                        is TripEvent.Started -> launch { classifier.onStarted(event.trip) }
                        is TripEvent.Resumed -> Log.i(TAG, "trip ${event.trip.id} resumed after a short stop")
                        is TripEvent.Ended -> settling = launch { app.purposeAsker.onTripEnded(classifier.onEnded(event.trip)) }
                        null -> Unit
                    }
                }
        }
        lifecycleScope.launch {
            app.navi.arrivals.collect { app.recorder.onDestinationArrived(it) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.getBooleanExtra(EXTRA_APP_VISIBLE, false) == true &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        ) {
            micEnabled = true
        }
        goForeground()
        return START_STICKY
    }

    private fun goForeground() {
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), foregroundType())
        } catch (e: SecurityException) {
            // 화면이 막 사라진 순간처럼 마이크를 더할 수 없으면 마이크 없이 기록만 이어간다.
            Log.w(TAG, "microphone foreground type refused", e)
            micEnabled = false
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), foregroundType())
        }
    }

    private fun foregroundType(): Int {
        var type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        if (micEnabled && Build.VERSION.SDK_INT >= 30) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        return type
    }

    override fun onDestroy() {
        autoLogApp.navi.stop()
        autoLogApp.vehicle.stop()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "운행 기록", NotificationManager.IMPORTANCE_LOW),
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("운행을 자동으로 기록하고 있어요")
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "TripRecorderService"
        private const val CHANNEL_ID = "trip_recorder"
        private const val NOTIFICATION_ID = 1
        private const val EXTRA_APP_VISIBLE = "app_visible"

        /** appVisible: 앱 화면이 보이는 지금 부른다면 true. 그때만 마이크를 쓸 수 있게 된다. */
        fun start(context: Context, appVisible: Boolean = false) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, TripRecorderService::class.java).putExtra(EXTRA_APP_VISIBLE, appVisible),
            )
        }
    }
}

/** 시동이 걸리면(= IVI 부팅) 기록 서비스를 띄운다. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) TripRecorderService.start(context)
    }
}

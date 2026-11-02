package com.autolog.app.recorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.autolog.app.MainActivity
import com.autolog.app.R
import com.autolog.app.autoLogApp
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * 화면을 열지 않아도 시동(부팅)부터 운행을 기록하는 포그라운드 서비스.
 * 기어·계기판 변화를 TripRecorder에 넘기고, 결과는 Room에 남는다.
 */
class TripRecorderService : LifecycleService() {

    override fun onCreate() {
        super.onCreate()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )

        val app = autoLogApp
        app.vehicle.start()
        app.navi.start()

        lifecycleScope.launch {
            combine(app.vehicle.gear, app.vehicle.odometerKm) { gear, km -> gear to km }
                .collect { (gear, km) -> app.recorder.update(gear, km) }
        }
        lifecycleScope.launch {
            app.navi.arrivals.collect { app.recorder.onDestinationArrived(it) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
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
        private const val CHANNEL_ID = "trip_recorder"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, TripRecorderService::class.java))
        }
    }
}

/** 시동이 걸리면(= IVI 부팅) 기록 서비스를 띄운다. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) TripRecorderService.start(context)
    }
}

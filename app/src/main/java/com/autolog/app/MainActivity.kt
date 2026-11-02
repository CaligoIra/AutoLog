package com.autolog.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.autolog.app.recorder.TripRecorderService
import com.autolog.app.ui.AutoLogScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    // 거절해도 기록은 계속된다. 상단 알림만 안 보일 뿐.
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 설치 직후 첫 실행처럼 부팅 리시버가 아직 안 돈 경우를 위해 여기서도 띄운다.
        TripRecorderService.start(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { AutoLogScreen(viewModel) }
    }
}

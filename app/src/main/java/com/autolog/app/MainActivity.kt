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

    // 거절해도 기록은 계속된다. 상단 알림이 안 보이거나 목적을 음성으로 묻지 못할 뿐.
    // 마이크를 방금 허락했다면 서비스가 바로 쓸 수 있게 다시 알린다.
    private val permissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            TripRecorderService.start(this, appVisible = true)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val missing = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) permissions.launch(missing.toTypedArray())
        setContent { AutoLogScreen(viewModel) }
    }

    override fun onStart() {
        super.onStart()
        // 설치 직후 첫 실행처럼 부팅 리시버가 아직 안 돈 경우를 위해 여기서도 띄운다.
        // 화면이 보이는 지금 알려야 서비스가 마이크를 쓸 수 있다.
        TripRecorderService.start(this, appVisible = true)
    }
}

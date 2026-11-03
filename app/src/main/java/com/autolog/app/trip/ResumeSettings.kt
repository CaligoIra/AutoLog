package com.autolog.app.trip

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 잠깐 P에 넣었다가 다시 출발했을 때 방금 끝낸 운행을 이어 갈지와, 몇 분 안이면 이어 갈지.
 * 신호 대기·드라이브스루가 잦으면 길게, 짧은 업무 방문이 잦으면 짧게 하거나 끈다.
 */
data class ResumeSetting(val enabled: Boolean = true, val minutes: Int = DEFAULT_MINUTES) {
    /** 이 안에 다시 출발하면 이어 간다. 끄면 0이라 늘 새 운행이다. */
    val windowMs: Long get() = if (enabled) minutes * 60_000L else 0L

    companion object {
        // 신호 대기, 드라이브스루, 셀프 주유가 대개 이 안에 끝난다.
        const val DEFAULT_MINUTES = 5
        val MINUTE_CHOICES = listOf(1, 3, 5, 10)
    }
}

class ResumeSettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("recorder_settings", Context.MODE_PRIVATE)

    private val _setting = MutableStateFlow(
        ResumeSetting(
            enabled = prefs.getBoolean(ENABLED, true),
            minutes = prefs.getInt(MINUTES, ResumeSetting.DEFAULT_MINUTES),
        ),
    )
    val setting: StateFlow<ResumeSetting> = _setting.asStateFlow()

    fun save(setting: ResumeSetting) {
        prefs.edit {
            putBoolean(ENABLED, setting.enabled)
            putInt(MINUTES, setting.minutes)
        }
        _setting.value = setting
    }

    private companion object {
        const val ENABLED = "resume_enabled"
        const val MINUTES = "resume_minutes"
    }
}

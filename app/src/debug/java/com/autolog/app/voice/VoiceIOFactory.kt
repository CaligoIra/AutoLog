package com.autolog.app.voice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.Channel

/**
 * 디버그 빌드 전용. 에뮬레이터에는 마이크 입력이 없는 경우가 많아,
 * adb로 미리 보낸 문장을 다음 음성 인식 결과로 쓴다. 보낸 문장이 없으면 실제 마이크로 듣는다.
 * 여러 번 보내면 보낸 순서대로 하나씩 쓴다(목적 없는 운행을 차례로 묻는 시험용).
 *
 *   adb shell am broadcast --user 10 -a com.autolog.app.debug.ANSWER -p com.autolog.app --es text "A사 납품 미팅"
 */
fun createVoiceIO(context: Context): VoiceIO = DebugVoiceIO(context, AndroidVoiceIO(context))

private class DebugVoiceIO(context: Context, private val real: VoiceIO) : VoiceIO by real {

    private val answers = Channel<String>(Channel.UNLIMITED)

    init {
        ContextCompat.registerReceiver(
            context.applicationContext,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    intent.getStringExtra("text")?.let { answers.trySend(it) }
                }
            },
            IntentFilter(ACTION_ANSWER),
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    override suspend fun listen(): String? = answers.tryReceive().getOrNull() ?: real.listen()
}

private const val ACTION_ANSWER = "com.autolog.app.debug.ANSWER"

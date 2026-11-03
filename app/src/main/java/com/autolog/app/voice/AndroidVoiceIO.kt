package com.autolog.app.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * 차량에 설치된 기본 음성 합성 엔진과 음성 인식 서비스를 쓴다
 * (Pleos Connect 에뮬레이터: ai.pleos.playground.caas / ai.umos.ambientai).
 * 소리 장치나 인식 서비스가 없어도 질문 흐름이 멈추지 않게 모든 대기에 시간 제한을 둔다.
 */
class AndroidVoiceIO(context: Context) : VoiceIO {

    private val context = context.applicationContext
    private val ready = CompletableDeferred<Boolean>()
    private val speaking = ConcurrentHashMap<String, CompletableDeferred<Unit>>()

    private val tts = TextToSpeech(this.context) { status -> ready.complete(status == TextToSpeech.SUCCESS) }

    // 엔진 연결이 끝난 뒤에만 설정할 수 있어 처음 읽을 때 한 번 한다.
    private val configured by lazy {
        tts.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        val result = tts.setLanguage(Locale.KOREAN)
        if (result < TextToSpeech.LANG_AVAILABLE) Log.w(TAG, "Korean TTS not available ($result)")
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) = finish(utteranceId)

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = finish(utteranceId)

            private fun finish(utteranceId: String?) {
                utteranceId?.let { speaking.remove(it) }?.complete(Unit)
            }
        })
    }

    override suspend fun speak(text: String) {
        if (!ready.await()) {
            Log.w(TAG, "TTS engine unavailable, skipped: $text")
            return
        }
        configured
        val id = UUID.randomUUID().toString()
        val done = CompletableDeferred<Unit>().also { speaking[id] = it }
        try {
            if (tts.speak(text, TextToSpeech.QUEUE_ADD, null, id) == TextToSpeech.SUCCESS) {
                withTimeoutOrNull(SPEAK_TIMEOUT_MS) { done.await() }
            }
        } catch (e: CancellationException) {
            // 출발해서 묻기를 멈추면 읽던 문장도 끊는다.
            tts.stop()
            throw e
        } finally {
            speaking.remove(id)
        }
    }

    // SpeechRecognizer는 메인 스레드에서만 만들고 써야 한다.
    override suspend fun listen(): String? = withContext(Dispatchers.Main) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "RECORD_AUDIO not granted")
            return@withContext null
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "no speech recognition service")
            return@withContext null
        }
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        try {
            withTimeoutOrNull(LISTEN_TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    recognizer.setRecognitionListener(OneAnswer { if (cont.isActive) cont.resume(it) })
                    recognizer.startListening(recognizeIntent())
                }
            }
        } finally {
            recognizer.destroy()
        }
    }

    private fun recognizeIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
        .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)

    /** 첫 결과나 오류 하나만 넘긴다. 나머지 콜백은 쓰지 않는다. */
    private class OneAnswer(private val onAnswer: (String?) -> Unit) : RecognitionListener {
        override fun onResults(results: Bundle?) =
            onAnswer(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull())

        override fun onError(error: Int) {
            Log.w(TAG, "speech recognition error $error")
            onAnswer(null)
        }

        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private companion object {
        const val TAG = "AndroidVoiceIO"
        const val SPEAK_TIMEOUT_MS = 15_000L
        const val LISTEN_TIMEOUT_MS = 15_000L
    }
}

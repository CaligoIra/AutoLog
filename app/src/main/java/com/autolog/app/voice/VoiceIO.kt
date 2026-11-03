package com.autolog.app.voice

/**
 * 음성 입출력. 지금은 Android 표준 TextToSpeech·SpeechRecognizer로 구현하고(AndroidVoiceIO),
 * Pleos TextToSpeech·SpeechToText SDK가 공개되면 이 인터페이스의 구현만 바꾼다.
 */
interface VoiceIO {
    /** 다 읽을 때까지 기다린다. 읽을 수 없으면 바로 돌아온다. */
    suspend fun speak(text: String)

    /** 한 번 듣고 가장 그럴듯한 문장을 돌려준다. 못 알아듣거나 들을 수 없으면 null. */
    suspend fun listen(): String?
}

package com.autolog.app.voice

import android.content.Context

fun createVoiceIO(context: Context): VoiceIO = AndroidVoiceIO(context)

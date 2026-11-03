package com.autolog.app

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.autolog.app.export.LogbookInfoStore
import com.autolog.app.navi.NaviMonitor
import com.autolog.app.navi.createPlaceSource
import com.autolog.app.trip.AutoLogDatabase
import com.autolog.app.trip.MIGRATION_1_2
import com.autolog.app.trip.MIGRATION_2_3
import com.autolog.app.trip.MIGRATION_3_4
import com.autolog.app.trip.TripClassifier
import com.autolog.app.trip.TripRecorder
import com.autolog.app.vehicle.VehicleMonitor
import com.autolog.app.voice.PurposeAsker
import com.autolog.app.voice.createVoiceIO

/** 서비스와 화면이 같은 차량 연결·DB를 쓰도록 프로세스 단위로 하나만 둔다. */
class AutoLogApp : Application() {

    val db by lazy {
        Room.databaseBuilder(this, AutoLogDatabase::class.java, "autolog.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }
    val recorder by lazy { TripRecorder(db.tripDao()) }
    val classifier by lazy { TripClassifier(db.tripDao(), createPlaceSource(this, navi)) }
    val purposeAsker by lazy { PurposeAsker(db.tripDao(), createVoiceIO(this)) }
    val vehicle by lazy { VehicleMonitor(this) }
    val navi by lazy { NaviMonitor(this) }
    val logbookInfo by lazy { LogbookInfoStore(this) }
}

val Context.autoLogApp: AutoLogApp get() = applicationContext as AutoLogApp

package com.autolog.app.trip

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 국세청 운행기록부 한 줄: 주행 전·후 계기판 거리.
 * endedAt == null 이면 아직 운행 중인 구간이다(앱이 죽었다 살아나도 이어서 닫는다).
 */
@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val startKm: Float,
    val endedAt: Long? = null,
    val endKm: Float? = null,
    val destination: String? = null,
)

val Trip.distanceKm: Float? get() = endKm?.let { it - startKm }

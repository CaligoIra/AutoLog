package com.autolog.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import com.autolog.app.MainViewModel
import com.autolog.app.trip.Trip
import com.autolog.app.trip.distanceKm
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Navy = Color(0xFF16324F)
private val Panel = Color(0xFF1E2A38)
private val Accent = Color(0xFF4FC3F7)

private val kmFormat = DecimalFormat("#,##0.0")
private val timeFormat = SimpleDateFormat("HH:mm", Locale.KOREA)

@Composable
fun AutoLogScreen(viewModel: MainViewModel) {
    val odometerKm by viewModel.vehicle.odometerKm.collectAsStateWithLifecycle()
    val gear by viewModel.vehicle.gear.collectAsStateWithLifecycle()
    val error by viewModel.vehicle.lastError.collectAsStateWithLifecycle()
    val current by viewModel.current.collectAsStateWithLifecycle()
    val trips by viewModel.trips.collectAsStateWithLifecycle()

    MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Navy, surface = Panel)) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Navy)
                .padding(48.dp),
            horizontalArrangement = Arrangement.spacedBy(48.dp),
        ) {
            StatusPanel(odometerKm, gear, current, error, Modifier.weight(1f))
            TripLog(trips, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatusPanel(
    odometerKm: Float?,
    gear: VehicleGear,
    current: Trip?,
    error: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(32.dp)) {
        Text("오토로그", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = Color.White)

        PanelCard {
            Text("계기판 누적거리", fontSize = 28.sp, color = Color.White.copy(alpha = 0.7f))
            Text(
                odometerKm?.let { "${kmFormat.format(it)} km" } ?: "읽는 중…",
                fontSize = 96.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text("기어  ${gear.label()}", fontSize = 36.sp, color = Accent)
        }

        PanelCard {
            if (current == null) {
                Text("주차 중", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "기어를 P에서 바꾸면 운행 기록이 자동으로 시작돼요.",
                    fontSize = 28.sp,
                    color = Color.White.copy(alpha = 0.7f),
                )
            } else {
                val sofar = odometerKm?.let { it - current.startKm } ?: 0f
                Text("운행 중", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Accent)
                Text(
                    "${timeFormat.format(Date(current.startedAt))} 출발 · 주행 전 ${kmFormat.format(current.startKm)} km · 지금까지 ${kmFormat.format(sofar)} km",
                    fontSize = 28.sp,
                    color = Color.White,
                )
            }
        }

        if (error != null) {
            Text("차량 연결 오류: $error", fontSize = 24.sp, color = Color(0xFFFF8A80))
        }
    }
}

@Composable
private fun TripLog(trips: List<Trip>, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text("운행 기록", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(24.dp))
        if (trips.isEmpty()) {
            Text("아직 기록이 없어요.", fontSize = 28.sp, color = Color.White.copy(alpha = 0.6f))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(trips, key = { it.id }) { TripRow(it) }
        }
    }
}

@Composable
private fun TripRow(trip: Trip) {
    PanelCard {
        Text(
            "${timeFormat.format(Date(trip.startedAt))} – ${trip.endedAt?.let { timeFormat.format(Date(it)) } ?: ""}" +
                "   ${kmFormat.format(trip.distanceKm ?: 0f)} km",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            "주행 전 ${kmFormat.format(trip.startKm)} km → 주행 후 ${trip.endKm?.let { kmFormat.format(it) } ?: "-"} km",
            fontSize = 26.sp,
            color = Color.White.copy(alpha = 0.8f),
        )
        trip.destination?.let { Text("도착: $it", fontSize = 24.sp, color = Accent) }
    }
}

@Composable
private fun PanelCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
    ) {
        Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
    }
}

private fun VehicleGear.label(): String = when (this) {
    VehicleGear.GEAR_PARK -> "P"
    VehicleGear.GEAR_REVERSE -> "R"
    VehicleGear.GEAR_NEUTRAL -> "N"
    VehicleGear.GEAR_DRIVE -> "D"
    VehicleGear.GEAR_UNKNOWN -> "-"
    else -> name.removePrefix("GEAR_")
}

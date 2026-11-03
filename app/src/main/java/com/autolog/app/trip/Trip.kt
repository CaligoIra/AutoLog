package com.autolog.app.trip

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.ZoneId

/**
 * 국세청 운행기록부 한 줄: 주행 전·후 계기판 거리.
 * endedAt == null 이면 아직 운행 중인 구간이다(앱이 죽었다 살아나도 이어서 닫는다).
 * purpose는 서식의 비고 칸에 들어갈 운행 목적으로, 운행이 끝난 뒤 음성으로 받는다.
 * startPlace·endPlace는 출발·도착 위치가 내비 즐겨찾기의 집·회사였는지(아니면 null), type은 서식의 업무 구분이다.
 * driverName·driverDepartment는 서식의 사용자(성명·부서) 칸으로, 운행기록부 화면에서 등록해 둔 운전자 중 고른다.
 * 고르지 않았으면 null이고 기본 운전자로 채운다. 운전자 목록이 바뀌어도 지난 기록이 그대로 남도록 이름을 그대로 적어 둔다.
 */
@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val startKm: Float,
    val endedAt: Long? = null,
    val endKm: Float? = null,
    val destination: String? = null,
    val purpose: String? = null,
    val startPlace: Place? = null,
    val endPlace: Place? = null,
    val type: TripType? = null,
    val driverName: String? = null,
    val driverDepartment: String? = null,
)

val Trip.distanceKm: Float? get() = endKm?.let { it - startKm }

/** 서식의 출·퇴근용·일반 업무용 칸 값. 이 구분의 운행이 아니면 빈칸(null)이다. */
fun Trip.kmIf(type: TripType): Float? = if (this.type == type) distanceKm ?: 0f else null

/** 운행을 시작한 해. 개인사업자의 과세기간(1월~12월)을 사업연도로 본다. */
val Trip.year: Int get() = Instant.ofEpochMilli(startedAt).atZone(ZoneId.systemDefault()).year

/** 그해 운행을 오래된 것부터. 운행기록부 서식과 합계가 쓴다. */
fun List<Trip>.ofYear(year: Int): List<Trip> = filter { it.year == year }.sortedBy { it.startedAt }

/** 내비 즐겨찾기에 등록된 장소 중 출퇴근을 가리는 데 쓰는 것. */
enum class Place(val label: String) { HOME("집"), OFFICE("회사") }

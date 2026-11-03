package com.autolog.app.export

import android.content.Context
import androidx.core.content.edit
import com.autolog.app.trip.Trip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** 서식의 사용자 칸(성명·부서)에 들어갈 운전자. */
data class Driver(val name: String, val department: String = "") {
    val label: String get() = if (department.isBlank()) name else "$name · $department"
}

/**
 * 운행기록부 서식 위쪽 칸(상호, 사업자등록번호, 차종, 자동차등록번호)과 차를 쓰는 운전자 목록.
 * 모두 적지 않아도 된다. 적어 두면 엑셀로 내보낼 때 채우고, 비워 두면 빈칸으로 둔다.
 * 자동차등록번호는 차량 SDK로 읽을 수 없어(CarInfo.getCRNInfo는 차대번호용 getVINInfo로 바뀜) 직접 적는다.
 * 운전자는 맨 앞이 기본 운전자로, 운행기록부 표에서 따로 고르지 않은 운행의 사용자 칸에 들어간다.
 */
data class LogbookInfo(
    val company: String = "",
    val businessNumber: String = "",
    val carModel: String = "",
    val plateNumber: String = "",
    val drivers: List<Driver> = emptyList(),
) {
    val isEmpty: Boolean get() = this == LogbookInfo()
    val defaultDriver: Driver? get() = drivers.firstOrNull()
}

/** 이 운행의 사용자. 표에서 고른 운전자가 없으면 기본 운전자다. */
fun Trip.driverOr(default: Driver?): Driver? = driverName?.let { Driver(it, driverDepartment.orEmpty()) } ?: default

/** 기록부 정보는 운행 기록처럼 차 안(앱 데이터)에만 둔다. */
class LogbookInfoStore(context: Context) {

    private val prefs = context.getSharedPreferences("logbook_info", Context.MODE_PRIVATE)

    private val _info = MutableStateFlow(
        LogbookInfo(
            company = prefs.getString(COMPANY, "").orEmpty(),
            businessNumber = prefs.getString(BUSINESS_NUMBER, "").orEmpty(),
            carModel = prefs.getString(CAR_MODEL, "").orEmpty(),
            plateNumber = prefs.getString(PLATE_NUMBER, "").orEmpty(),
            drivers = prefs.getString(DRIVERS, null)?.let(::parseDrivers).orEmpty(),
        ),
    )
    val info: StateFlow<LogbookInfo> = _info.asStateFlow()

    fun save(info: LogbookInfo) {
        prefs.edit {
            putString(COMPANY, info.company)
            putString(BUSINESS_NUMBER, info.businessNumber)
            putString(CAR_MODEL, info.carModel)
            putString(PLATE_NUMBER, info.plateNumber)
            putString(DRIVERS, JSONArray(info.drivers.map { JSONObject().put(NAME, it.name).put(DEPARTMENT, it.department) }).toString())
        }
        _info.value = info
    }

    private fun parseDrivers(json: String): List<Driver> = runCatching {
        val array = JSONArray(json)
        List(array.length()) { i -> array.getJSONObject(i).let { Driver(it.optString(NAME), it.optString(DEPARTMENT)) } }
    }.getOrDefault(emptyList())

    private companion object {
        const val COMPANY = "company"
        const val BUSINESS_NUMBER = "business_number"
        const val CAR_MODEL = "car_model"
        const val PLATE_NUMBER = "plate_number"
        const val DRIVERS = "drivers"
        const val NAME = "name"
        const val DEPARTMENT = "department"
    }
}

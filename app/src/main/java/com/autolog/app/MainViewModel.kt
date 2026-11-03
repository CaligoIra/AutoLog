package com.autolog.app

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import com.autolog.app.export.Driver
import com.autolog.app.export.LogbookInfo
import com.autolog.app.export.exportLogbook
import com.autolog.app.trip.ResumeSetting
import com.autolog.app.trip.Trip
import com.autolog.app.trip.TripType
import com.autolog.app.trip.UsageSummary
import com.autolog.app.trip.ofYear
import com.autolog.app.trip.savePurpose
import com.autolog.app.trip.summarize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** 화면은 보기만 한다. 기록은 TripRecorderService가 하고, 여기서는 그 결과(DB)를 관찰한다. */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val autoLog = app as AutoLogApp
    private val dao = autoLog.db.tripDao()

    val vehicle = autoLog.vehicle

    val current: StateFlow<Trip?> = dao.observeOpenTrip()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val trips: StateFlow<List<Trip>> = dao.observeFinishedTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 화면 위 합계는 올해 운행만 센다. 지난해 운행기록부는 기록부 화면에서 해를 골라 본다. */
    val summary: StateFlow<UsageSummary> = trips
        .map { it.ofYear(LocalDate.now().year).summarize() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Trip>().summarize())

    val asking = autoLog.purposeAsker.asking

    val logbookInfo: StateFlow<LogbookInfo> = autoLog.logbookInfo.info

    val resume: StateFlow<ResumeSetting> = autoLog.resumeSettings.setting

    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

    private var askJob: Job? = null

    init {
        // 화면에서 시작한 질문은 주차 중에만 한다. 출발하면 멈춘다.
        viewModelScope.launch {
            vehicle.gear.collect { if (it != VehicleGear.GEAR_PARK) askJob?.cancel() }
        }
    }

    /** 주차 후 음성 질문을 놓친 운행의 목적을 묻거나, 이미 있는 목적을 말로 고친다. */
    fun askPurpose(trip: Trip) {
        askJob = viewModelScope.launch { autoLog.purposeAsker.ask(trip) }
    }

    /** 목적이 빈 운행을 오래된 것부터 차례로 묻는다. */
    fun askMissingPurposes() {
        val missing = trips.value.filter { it.purpose == null }.sortedBy { it.startedAt }
        if (missing.isNotEmpty()) askJob = viewModelScope.launch { autoLog.purposeAsker.askAll(missing) }
    }

    /** 키보드로 적은 목적을 저장한다. */
    fun savePurpose(trip: Trip, purpose: String) {
        viewModelScope.launch { dao.savePurpose(trip, purpose.trim()) }
    }

    fun saveLogbookInfo(info: LogbookInfo) = autoLog.logbookInfo.save(info)

    /** 잠깐 P에 넣었다 다시 출발할 때 이어 기록할지와 몇 분 안이면 이어 갈지. 다음 출발부터 쓴다. */
    fun setResume(setting: ResumeSetting) = autoLog.resumeSettings.save(setting)

    /** 그해 운행기록부를 엑셀로 만들어 다운로드 폴더에 둔다. */
    fun exportLogbook(year: Int) {
        val yearTrips = trips.value.ofYear(year)
        val info = logbookInfo.value
        viewModelScope.launch {
            _exportMessage.value = "엑셀 파일을 만드는 중…"
            _exportMessage.value = runCatching {
                withContext(Dispatchers.IO) { exportLogbook(getApplication(), year, yearTrips, info) }
            }.fold(
                onSuccess = { "${it}에 저장했어요" },
                onFailure = {
                    Log.w(TAG, "logbook export failed", it)
                    "저장하지 못했어요: ${it.message}"
                },
            )
        }
    }

    fun clearExportMessage() {
        _exportMessage.value = null
    }

    /** 자동으로 정해진 구분이 틀렸거나 비어 있으면 화면에서 고친다. */
    fun setType(trip: Trip, type: TripType) {
        viewModelScope.launch { dao.setType(trip.id, type) }
    }

    /** 운행기록부 표에서 이 운행의 사용자를 고른다. null이면 기본 운전자로 되돌린다. */
    fun setDriver(trip: Trip, driver: Driver?) {
        viewModelScope.launch { dao.setDriver(trip.id, driver?.name, driver?.department) }
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}

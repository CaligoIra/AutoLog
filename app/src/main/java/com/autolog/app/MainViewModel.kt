package com.autolog.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autolog.app.trip.Trip
import com.autolog.app.trip.TripType
import com.autolog.app.trip.UsageSummary
import com.autolog.app.trip.summarize
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** 화면은 보기만 한다. 기록은 TripRecorderService가 하고, 여기서는 그 결과(DB)를 관찰한다. */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val autoLog = app as AutoLogApp
    private val dao = autoLog.db.tripDao()

    val vehicle = autoLog.vehicle

    val current: StateFlow<Trip?> = dao.observeOpenTrip()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val trips: StateFlow<List<Trip>> = dao.observeFinishedTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 올해 운행 합계. 개인사업자의 과세기간(1월~12월)에 맞춘다. */
    val summary: StateFlow<UsageSummary> = trips
        .map { list ->
            val yearStart = LocalDate.now().withDayOfYear(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            list.filter { it.startedAt >= yearStart }.summarize()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Trip>().summarize())

    val asking = autoLog.purposeAsker.asking

    /** 주차 후 음성 질문을 놓친 운행의 목적을 다시 묻는다. */
    fun askPurpose(trip: Trip) {
        viewModelScope.launch { autoLog.purposeAsker.ask(trip) }
    }

    /** 자동으로 정해진 구분이 틀렸거나 비어 있으면 화면에서 고친다. */
    fun setType(trip: Trip, type: TripType) {
        viewModelScope.launch { dao.setType(trip.id, type) }
    }
}

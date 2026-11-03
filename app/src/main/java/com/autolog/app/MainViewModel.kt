package com.autolog.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autolog.app.trip.Trip
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 화면은 보기만 한다. 기록은 TripRecorderService가 하고, 여기서는 그 결과(DB)를 관찰한다. */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val autoLog = app as AutoLogApp
    private val dao = autoLog.db.tripDao()

    val vehicle = autoLog.vehicle

    val current: StateFlow<Trip?> = dao.observeOpenTrip()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val trips: StateFlow<List<Trip>> = dao.observeFinishedTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val asking = autoLog.purposeAsker.asking

    /** 주차 후 음성 질문을 놓친 운행의 목적을 다시 묻는다. */
    fun askPurpose(trip: Trip) {
        viewModelScope.launch { autoLog.purposeAsker.ask(trip) }
    }
}

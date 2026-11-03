package com.autolog.app.navi

import android.content.Context
import com.autolog.app.trip.Place

fun createPlaceSource(context: Context, navi: NaviMonitor): suspend () -> Place? = navi::currentPlace

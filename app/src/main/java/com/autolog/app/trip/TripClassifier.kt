package com.autolog.app.trip

/**
 * 출발·도착 장소로 출퇴근 운행을 가린다. 집에서 회사로 가면 출근, 회사에서 집으로 가면 퇴근이다.
 * 집·회사는 내비 즐겨찾기에서 오고, currentPlace는 차가 지금 그 근처인지 알려준다(모르면 null).
 * 출퇴근이 아니면 구분은 비워 두고, 음성으로 받은 목적으로 정한다(PurposeAsker).
 */
class TripClassifier(
    private val dao: TripDao,
    private val currentPlace: suspend () -> Place?,
) {

    suspend fun onStarted(trip: Trip) {
        currentPlace()?.let { dao.setStartPlace(trip.id, it) }
    }

    /** 도착 장소를 남기고, 집↔회사면 출퇴근으로 정한다. 최신 운행을 돌려준다. */
    suspend fun onEnded(trip: Trip): Trip {
        val end = currentPlace()
        if (end != null) dao.setEndPlace(trip.id, end)
        // 출발 장소는 운행 시작 때 따로 저장되므로 다시 읽는다.
        // 시동 직후라 내비가 아직 답하지 못했으면, 앞 운행의 도착지가 곧 이번 출발지다.
        val start = dao.get(trip.id)?.startPlace ?: dao.previousTrip(trip.id)?.endPlace
        val commute = when {
            start == Place.HOME && end == Place.OFFICE -> "출근"
            start == Place.OFFICE && end == Place.HOME -> "퇴근"
            else -> null
        }
        if (commute != null) {
            dao.setType(trip.id, TripType.COMMUTE)
            dao.setPurpose(trip.id, commute)
        }
        return dao.get(trip.id) ?: trip
    }
}

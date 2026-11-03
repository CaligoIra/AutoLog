package com.autolog.app.trip

/**
 * 운행기록부의 업무 구분. 출퇴근과 일반업무를 합한 거리가 서식의 '업무용 사용거리'이고,
 * 비업무 운행은 총주행거리에만 들어가 업무사용비율을 낮춘다.
 */
enum class TripType(val label: String) {
    COMMUTE("출퇴근"),
    BUSINESS("일반업무"),
    PERSONAL("비업무");

    companion object {
        /** 말한 목적으로 구분을 정한다. 개인 용무라고 말하지 않았으면 일반업무로 본다. */
        fun fromPurpose(purpose: String): TripType {
            val said = purpose.replace(" ", "")
            return when {
                // 질문이 "개인 용무였다면 개인이라고" 하므로 '개인'으로 시작하면 비업무다("개인 장보기").
                // 다만 "개인 고객 방문"처럼 업무인 말도 '개인'으로 시작하므로 그런 말은 뺀다.
                said.startsWith("개인") && BUSINESS_AFTER_PERSONAL.none { said.startsWith("개인$it") } -> PERSONAL
                PERSONAL_WORDS.any { it in said } -> PERSONAL
                COMMUTE_WORDS.any { it in said } -> COMMUTE
                else -> BUSINESS
            }
        }

        private val BUSINESS_AFTER_PERSONAL = listOf("고객", "사업자", "회원")
        private val PERSONAL_WORDS = listOf("개인용무", "개인적", "사적", "비업무", "업무아님", "업무아니", "업무외")
        private val COMMUTE_WORDS = listOf("출근", "퇴근")
    }
}

/** 서식 아래쪽 합계 칸. 구분이 안 된 운행은 총주행거리에는 넣고 업무용 사용거리에는 넣지 않는다. */
data class UsageSummary(
    val totalKm: Float,
    val commuteKm: Float,
    val businessKm: Float,
    val unclassified: Int,
) {
    val businessUseKm: Float get() = commuteKm + businessKm
    val ratio: Float? get() = if (totalKm > 0f) businessUseKm / totalKm else null
}

fun List<Trip>.summarize(): UsageSummary {
    fun km(type: TripType) = filter { it.type == type }.sumOf { (it.distanceKm ?: 0f).toDouble() }.toFloat()
    return UsageSummary(
        totalKm = sumOf { (it.distanceKm ?: 0f).toDouble() }.toFloat(),
        commuteKm = km(TripType.COMMUTE),
        businessKm = km(TripType.BUSINESS),
        unclassified = count { it.type == null },
    )
}

package com.autolog.app.export

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.autolog.app.export.XlsxSheet.Companion.ref
import com.autolog.app.export.XlsxSheet.Style
import com.autolog.app.trip.Trip
import com.autolog.app.trip.TripType
import com.autolog.app.trip.kmIf
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToLong

private const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
private const val FOLDER = "AutoLog"

/**
 * 그해 운행기록부를 엑셀로 만들어 다운로드/AutoLog 폴더에 두고, 저장한 곳을 사람이 읽을 수 있게 돌려준다.
 * 차량에는 파일 저장 창이나 공유할 앱이 없어(에뮬레이터에서 확인) 정해진 폴더에 바로 저장한다.
 */
fun exportLogbook(context: Context, year: Int, trips: List<Trip>, info: LogbookInfo): String =
    saveToDownloads(context, "업무용승용차_운행기록부_$year.xlsx") { writeLogbook(it, year, trips, info) }

/**
 * 국세청 「업무용승용차 운행기록부」 서식과 같은 칸으로 쓴다.
 * 위쪽 사업연도·상호·사업자등록번호, 1. 기본정보(차종·자동차등록번호), 2. 업무용 사용비율 계산(운행 한 줄씩), 맨 아래 ⑪~⑬.
 * 주행거리·업무용 거리·합계·비율은 수식으로 넣어, 엑셀에서 숫자를 고쳐도 합계가 따라간다.
 */
internal fun writeLogbook(out: OutputStream, year: Int, trips: List<Trip>, info: LogbookInfo) {
    val sheet = XlsxSheet("운행기록부")
    sheet.columns(*WIDTHS)

    sheet.text(1, 1, "사업연도\n$year.01.01 ~ $year.12.31", Style.CENTER, toRow = 2, toCol = 2)
    sheet.text(1, 3, "업무용승용차 운행기록부", Style.TITLE, toRow = 2, toCol = 6)
    sheet.text(1, 7, "상호명\n(법인명)", Style.HEADER)
    sheet.text(1, 8, info.company, Style.CENTER, toCol = 9)
    sheet.text(2, 7, "사업자\n등록번호", Style.HEADER)
    sheet.text(2, 8, info.businessNumber, Style.CENTER, toCol = 9)
    sheet.height(1, 27.0)
    sheet.height(2, 27.0)

    sheet.text(4, 1, "1. 기본정보", Style.SECTION)
    sheet.text(5, 1, "① 차 종", Style.HEADER, toCol = 4)
    sheet.text(5, 5, "② 자동차등록번호", Style.HEADER, toCol = 9)
    sheet.text(6, 1, info.carModel, Style.CENTER, toCol = 4)
    sheet.text(6, 5, info.plateNumber, Style.CENTER, toCol = 9)

    sheet.text(8, 1, "2. 업무용 사용비율 계산", Style.SECTION)
    sheet.text(9, 1, "③ 사용일자\n(요일)", Style.HEADER, toRow = 10)
    sheet.text(9, 2, "④ 사용자", Style.HEADER, toCol = 3)
    sheet.text(9, 4, "운행내역", Style.HEADER, toCol = 6)
    sheet.text(9, 7, "업무용 사용거리(km)", Style.HEADER, toCol = 8)
    sheet.text(9, 9, "⑩ 비고", Style.HEADER, toRow = 10)
    listOf(
        "부서", "성명", "⑤ 주행 전\n계기판의 거리(km)", "⑥ 주행 후\n계기판의 거리(km)",
        "⑦ 주행거리\n(km)", "⑧ 출·퇴근용\n(km)", "⑨ 일반 업무용\n(km)",
    ).forEachIndexed { i, label -> sheet.text(10, i + 2, label, Style.HEADER) }
    sheet.height(10, 42.0)
    sheet.repeatOnEachPage(9..10)

    // 칸 값은 소수 첫째 자리까지만 쓴다. 합계도 이 값으로 더해야 엑셀이 다시 계산한 값과 같다.
    val first = 11
    var total = 0.0
    var businessUse = 0.0
    trips.forEachIndexed { i, trip ->
        val r = first + i
        val start = km(trip.startKm)
        val distance = km(trip.endKm ?: trip.startKm) - start
        total += distance
        val driver = trip.driverOr(info.defaultDriver)
        sheet.text(r, 1, dateFormat.format(Date(trip.startedAt)), Style.CENTER)
        sheet.text(r, 2, driver?.department.orEmpty(), Style.CENTER)
        sheet.text(r, 3, driver?.name.orEmpty(), Style.CENTER)
        sheet.number(r, 4, start, Style.NUMBER)
        sheet.number(r, 5, start + distance, Style.NUMBER)
        sheet.number(r, 6, distance, Style.NUMBER, formula = "${ref(r, 5)}-${ref(r, 4)}")
        listOf(TripType.COMMUTE to 7, TripType.BUSINESS to 8).forEach { (type, col) ->
            // 구분이 다르거나 아직 없으면 빈칸이다. 비업무 운행은 총주행거리에만 들어간다.
            if (trip.kmIf(type) == null) {
                sheet.blank(r, col, Style.NUMBER)
            } else {
                businessUse += distance
                sheet.number(r, col, distance, Style.NUMBER, formula = ref(r, 6))
            }
        }
        sheet.text(r, 9, trip.purpose.orEmpty(), Style.TEXT)
        // 엑셀도 LibreOffice도 열 때 행 높이를 다시 맞추지 않는다. 칸보다 긴 글은 줄바꿈되므로 그만큼 높인다.
        val lines = maxOf(lines(driver?.department.orEmpty(), WIDTHS[1]), lines(driver?.name.orEmpty(), WIDTHS[2]), lines(trip.purpose.orEmpty(), WIDTHS[8]))
        if (lines > 1) sheet.height(r, LINE_PT * lines)
    }

    val last = first + trips.size - 1
    val label = last + 1
    val value = last + 2
    sheet.text(label, 1, "⑪ 사업연도 총주행 거리(km)", Style.HEADER, toCol = 3)
    sheet.text(label, 4, "⑫ 사업연도 업무용 사용거리(km)", Style.HEADER, toCol = 6)
    sheet.text(label, 7, "⑬ 업무사용비율(⑫/⑪)", Style.HEADER, toCol = 9)
    val rows = trips.isNotEmpty()
    sheet.number(value, 1, total, Style.TOTAL, formula = "SUM(F$first:F$last)".takeIf { rows }, toCol = 3)
    sheet.number(value, 4, businessUse, Style.TOTAL, formula = "SUM(G$first:H$last)".takeIf { rows }, toCol = 6)
    if (total > 0) {
        sheet.number(value, 7, businessUse / total, Style.PERCENT, formula = "${ref(value, 4)}/${ref(value, 1)}", toCol = 9)
    } else {
        sheet.blank(value, 7, Style.PERCENT, toCol = 9)
    }
    sheet.height(value, 24.0)

    sheet.write(out)
}

private val dateFormat = SimpleDateFormat("M/d(E)", Locale.KOREA)

// 열 너비. 단위는 숫자 '0' 한 글자 폭이다.
private val WIDTHS = doubleArrayOf(12.0, 10.0, 10.0, 15.0, 15.0, 11.0, 11.0, 11.0, 36.0)
private const val LINE_PT = 15.0

private fun km(value: Float) = (value * 10.0).roundToLong() / 10.0

// 한글은 '0'의 두 배쯤 넓다. 칸 양옆 여백으로 한 글자를 뺀다.
private fun lines(text: String, width: Double): Int =
    maxOf(1, ceil(text.sumOf { if (it.code >= 0x1100) 1.8 else 1.0 } / (width - 1)).toInt())

/** 다운로드/AutoLog 폴더에 저장한다. 같은 이름이 있으면 시스템이 번호를 붙이므로 실제 이름을 다시 읽어 돌려준다. */
private fun saveToDownloads(context: Context, fileName: String, write: (OutputStream) -> Unit): String {
    if (Build.VERSION.SDK_INT < 29) {
        // Android 9에는 다운로드 폴더에 권한 없이 쓰는 방법이 없어 앱 전용 폴더에 둔다.
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        file.outputStream().use(write)
        return file.path
    }
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
        put(MediaStore.Downloads.MIME_TYPE, XLSX_MIME)
        put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER")
        put(MediaStore.Downloads.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("다운로드 폴더에 파일을 만들 수 없어요")
    try {
        resolver.openOutputStream(uri)?.use(write) ?: error("파일을 열 수 없어요")
        resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
    } catch (e: Exception) {
        resolver.delete(uri, null, null)
        throw e
    }
    val saved = resolver.query(uri, arrayOf(MediaStore.Downloads.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getString(0) else null
    }
    return "다운로드/$FOLDER/${saved ?: fileName}"
}

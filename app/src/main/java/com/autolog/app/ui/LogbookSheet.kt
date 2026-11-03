package com.autolog.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.autolog.app.export.Driver
import com.autolog.app.export.LogbookInfo
import com.autolog.app.export.driverOr
import com.autolog.app.trip.Trip
import com.autolog.app.trip.TripType
import com.autolog.app.trip.distanceKm
import com.autolog.app.trip.kmIf
import com.autolog.app.trip.ofYear
import com.autolog.app.trip.summarize
import com.autolog.app.trip.year
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("M/d(E)", Locale.KOREA)
private val ratioFormat = DecimalFormat("0.0")

// 국세청 「업무용승용차 운행기록부」의 칸 순서와 같다. 사용일자, 사용자(부서·성명), 주행 전·후 계기판 거리, 주행거리, 출·퇴근용, 일반 업무용, 비고
private val widths = listOf(1.2f, 1f, 1f, 1.3f, 1.3f, 1.1f, 1.1f, 1.1f, 2.6f)

/**
 * 한 해 운행을 국세청 운행기록부 서식과 같은 표로 보여주고 엑셀로 내보낸다. 내보내기 전에 빈 칸을 확인하는 화면이다.
 * 신고는 다음 해에 하므로 지난해도 고를 수 있다. 처음에는 운행이 있는 가장 최근 해를 보인다.
 */
@Composable
fun LogbookSheet(
    trips: List<Trip>,
    info: LogbookInfo,
    exportMessage: String?,
    onSaveInfo: (LogbookInfo) -> Unit,
    onSetDriver: (Trip, Driver?) -> Unit,
    onSetType: (Trip, TripType) -> Unit,
    onEditPurpose: (Trip) -> Unit,
    onExport: (year: Int) -> Unit,
    onClose: () -> Unit,
) {
    val years = remember(trips) { trips.map { it.year }.distinct().sortedDescending() }
    var year by rememberSaveable { mutableIntStateOf(years.firstOrNull() ?: LocalDate.now().year) }
    val yearTrips = remember(trips, year) { trips.ofYear(year) }
    val summary = remember(yearTrips) { yearTrips.summarize() }
    var editingInfo by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Navy)
            .padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text("업무용승용차 운행기록부", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("사업연도 $year.01.01 ~ $year.12.31", fontSize = 24.sp, color = Color.White.copy(alpha = 0.7f))
                    if (years.size > 1) {
                        years.forEach {
                            FilterChip(selected = it == year, onClick = { year = it }, label = { Text("$it", fontSize = 22.sp) })
                        }
                    }
                }
            }
            OutlinedButton(onClick = { editingInfo = true }) { Text("기록부 정보", fontSize = 24.sp) }
            Button(onClick = { onExport(year) }, enabled = yearTrips.isNotEmpty()) { Text("엑셀로 내보내기", fontSize = 24.sp) }
            OutlinedButton(onClick = onClose) { Text("닫기", fontSize = 24.sp) }
        }
        Text(
            if (info.isEmpty) {
                "기록부 정보(상호·차량번호·운전자 등)를 적어 두면 엑셀 서식 위쪽 칸과 사용자 칸이 채워져요. 적지 않아도 돼요."
            } else {
                listOf(
                    "상호" to info.company, "사업자등록번호" to info.businessNumber, "차종" to info.carModel,
                    "차량번호" to info.plateNumber,
                    "운전자" to info.defaultDriver?.let { "${info.drivers.size}명(기본 ${it.label})" }.orEmpty(),
                ).filter { it.second.isNotBlank() }.joinToString(" · ") { "${it.first} ${it.second}" } +
                    if (info.drivers.size > 1) " · 사용자 칸을 누르면 운행마다 운전자를 고를 수 있어요" else ""
            },
            fontSize = 22.sp,
            color = Color.White.copy(alpha = 0.7f),
        )
        exportMessage?.let { Text(it, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Accent) }
        // 그대로 내면 업무용 거리가 빠지거나 비고가 빈다. 고칠 것을 먼저 알린다.
        val missing = yearTrips.count { it.purpose == null }
        if (missing > 0 || summary.unclassified > 0) {
            Text(
                listOfNotNull(
                    missing.takeIf { it > 0 }?.let { "목적 없는 운행 ${it}건" },
                    summary.unclassified.takeIf { it > 0 }?.let { "구분 안 된 운행 ${it}건" },
                ).joinToString(" · ") + "은 표에서 ‘?’나 ‘목적 없음’을 눌러 채울 수 있어요",
                fontSize = 24.sp,
                color = Warn,
            )
        }
        PanelCard(Modifier.weight(1f)) {
            HeaderRow(
                listOf("", "사용자", "운행 내역", "업무용 사용거리", ""),
                listOf(widths[0], widths[1] + widths[2], widths[3] + widths[4] + widths[5], widths[6] + widths[7], widths[8]),
            )
            HeaderRow(
                listOf(
                    "사용일자\n(요일)", "부서", "성명", "주행 전\n계기판 거리(km)", "주행 후\n계기판 거리(km)",
                    "주행거리\n(km)", "출·퇴근용\n(km)", "일반 업무용\n(km)", "비고",
                ),
                widths,
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
            LazyColumn(Modifier.weight(1f)) {
                items(yearTrips, key = { it.id }) { trip ->
                    LogRow(
                        trip,
                        info,
                        onPickDriver = { onSetDriver(trip, it) },
                        onManageDrivers = { editingInfo = true },
                        onSetType = { onSetType(trip, it) },
                        onEditPurpose = { onEditPurpose(trip) },
                    )
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
            Row {
                Cell("합계", widths.take(5).sum(), bold = true)
                Cell(kmFormat.format(summary.totalKm), widths[5], bold = true)
                Cell(kmFormat.format(summary.commuteKm), widths[6], bold = true)
                Cell(kmFormat.format(summary.businessKm), widths[7], bold = true)
                Cell("", widths[8])
            }
            Text(
                "사업연도 총주행거리 ${kmFormat.format(summary.totalKm)} km · 업무용 사용거리 ${kmFormat.format(summary.businessUseKm)} km · " +
                    "업무사용비율 ${summary.ratio?.let { "${ratioFormat.format(it * 100)}%" } ?: "-"}",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Accent,
            )
        }
    }
    if (editingInfo) {
        LogbookInfoDialog(
            info,
            onSave = {
                onSaveInfo(it)
                editingInfo = false
            },
            onDismiss = { editingInfo = false },
        )
    }
}

@Composable
private fun HeaderRow(labels: List<String>, weights: List<Float>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        labels.zip(weights).forEach { (label, weight) ->
            Cell(label, weight, color = Color.White.copy(alpha = 0.7f), align = TextAlign.Center)
        }
    }
}

/** 운행 한 줄. 사용자·구분·비고 칸은 눌러서 고칠 수 있어, 내보내기 전에 빈칸을 이 화면에서 채운다. */
@Composable
private fun LogRow(
    trip: Trip,
    info: LogbookInfo,
    onPickDriver: (Driver?) -> Unit,
    onManageDrivers: () -> Unit,
    onSetType: (TripType) -> Unit,
    onEditPurpose: () -> Unit,
) {
    val km = trip.distanceKm ?: 0f
    // 누를 수 있는 칸들의 바탕 높이를 줄에서 가장 높은 칸에 맞춘다.
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Cell(dateFormat.format(Date(trip.startedAt)), widths[0])
        DriverCell(trip, info, onPickDriver, onManageDrivers)
        Cell(kmFormat.format(trip.startKm), widths[3])
        Cell(trip.endKm?.let { kmFormat.format(it) }.orEmpty(), widths[4])
        Cell(kmFormat.format(km), widths[5])
        TypeCells(trip, onSetType)
        Row(Modifier.weight(widths[8]).fillMaxHeight().tappable(onEditPurpose), verticalAlignment = Alignment.CenterVertically) {
            Cell(trip.purpose ?: "목적 없음", 1f, color = if (trip.purpose == null) Warn else Color.White, align = TextAlign.Start)
        }
    }
}

// 누를 수 있는 칸은 옅은 바탕을 깔아 버튼처럼 보이게 한다.
private fun Modifier.tappable(onClick: () -> Unit) = this
    .padding(horizontal = 4.dp)
    .clip(RoundedCornerShape(8.dp))
    .background(Color.White.copy(alpha = 0.06f))
    .clickable(onClick = onClick)
    .padding(vertical = 8.dp)

/** 서식의 출·퇴근용·일반 업무용 칸. 누르면 운행 구분을 고른다. */
@Composable
private fun RowScope.TypeCells(trip: Trip, onPick: (TripType) -> Unit) {
    var open by remember { mutableStateOf(false) }
    // 구분이 없으면 업무용 칸을 채울 수 없으므로 물음표로 표시한다. 비업무는 서식대로 두 칸 모두 비워 둔다.
    val color = if (trip.type == null) Warn else Color.White
    fun cell(type: TripType) = if (trip.type == null) "?" else trip.kmIf(type)?.let { kmFormat.format(it) }.orEmpty()
    Box(Modifier.weight(widths[6] + widths[7]).fillMaxHeight()) {
        Row(Modifier.fillMaxSize().tappable { open = true }, verticalAlignment = Alignment.CenterVertically) {
            Cell(cell(TripType.COMMUTE), widths[6], color = color)
            Cell(cell(TripType.BUSINESS), widths[7], color = color)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            TripType.entries.forEach { type ->
                MenuItem(if (type == trip.type) "${type.label}  ✓" else type.label) {
                    open = false
                    onPick(type)
                }
            }
        }
    }
}

/** 서식의 사용자(부서·성명) 칸. 누르면 등록해 둔 운전자 중에서 고른다. 고르지 않은 운행은 기본 운전자를 흐리게 보인다. */
@Composable
private fun RowScope.DriverCell(trip: Trip, info: LogbookInfo, onPick: (Driver?) -> Unit, onManage: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val driver = trip.driverOr(info.defaultDriver)
    val color = if (trip.driverName != null) Color.White else Color.White.copy(alpha = 0.6f)
    Box(Modifier.weight(widths[1] + widths[2]).fillMaxHeight()) {
        Row(Modifier.fillMaxSize().tappable { open = true }, verticalAlignment = Alignment.CenterVertically) {
            Cell(driver?.department.orEmpty(), widths[1], color = color, align = TextAlign.Center)
            Cell(driver?.name ?: "선택", widths[2], color = if (driver == null) Accent else color, align = TextAlign.Center)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            fun pick(action: () -> Unit) {
                open = false
                action()
            }
            info.drivers.forEachIndexed { i, d ->
                MenuItem(if (i == 0) "${d.label} (기본)" else d.label) { pick { onPick(d) } }
            }
            if (trip.driverName != null) MenuItem("기본 운전자로 되돌리기") { pick { onPick(null) } }
            MenuItem("운전자 등록·편집", Accent) { pick(onManage) }
        }
    }
}

// 메뉴 항목은 기본 폭(최대 280dp)이 좁아 큰 글씨가 두 줄로 겹친다. 폭과 높이를 차에 맞게 정한다.
@Composable
private fun MenuItem(text: String, color: Color = Color.Unspecified, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, fontSize = 24.sp, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        onClick = onClick,
        modifier = Modifier.width(480.dp).heightIn(min = 64.dp),
    )
}

@Composable
private fun RowScope.Cell(
    text: String,
    weight: Float,
    color: Color = Color.White,
    align: TextAlign = TextAlign.End,
    bold: Boolean = false,
) {
    Text(
        text,
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 8.dp),
        fontSize = 22.sp,
        color = color,
        textAlign = align,
        fontWeight = if (bold) FontWeight.Bold else null,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

/** 서식 위쪽 칸과 운전자 목록. 모두 적지 않아도 된다. */
@Composable
private fun LogbookInfoDialog(info: LogbookInfo, onSave: (LogbookInfo) -> Unit, onDismiss: () -> Unit) {
    var draft by remember { mutableStateOf(info) }
    var name by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    val nameFocus = remember { FocusRequester() }
    // 적고 있는 운전자를 목록에 더한 정보. 이름이 없거나 이미 있는 사람이면 그대로다.
    fun withTyped(): LogbookInfo {
        val driver = Driver(name.trim(), department.trim())
        return if (driver.name.isEmpty() || driver in draft.drivers) draft else draft.copy(drivers = draft.drivers + driver)
    }
    fun addDriver() {
        draft = withTyped()
        name = ""
        department = ""
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        // 바깥을 잘못 눌러 적던 것을 잃지 않도록 취소 버튼으로만 닫는다.
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
        // AlertDialog는 안쪽에서 폭을 560dp로 묶는다. 바깥에서 폭을 정해 두면 그 제한이 이 폭 안으로 맞춰진다.
        modifier = Modifier.padding(48.dp).widthIn(max = 1000.dp).fillMaxWidth(),
        title = { Text("기록부 정보 (선택)", fontSize = 32.sp) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("적어 두면 엑셀로 내보낼 때 서식에 채워요. 비워 둔 칸은 빈칸으로 둡니다.", fontSize = 22.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    InfoField("상호(법인명)", draft.company, { draft = draft.copy(company = it) })
                    InfoField("사업자등록번호", draft.businessNumber, { draft = draft.copy(businessNumber = it) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    InfoField("차종", draft.carModel, { draft = draft.copy(carModel = it) })
                    InfoField("자동차등록번호", draft.plateNumber, { draft = draft.copy(plateNumber = it) })
                }
                Text("운전자", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(
                    "맨 위가 기본 운전자로, 따로 고르지 않은 운행의 사용자 칸에 들어가요. 운행기록부 표의 사용자 칸을 누르면 운행마다 고를 수 있어요.",
                    fontSize = 22.sp,
                )
                draft.drivers.forEachIndexed { i, driver ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${i + 1}. ${driver.label}" + if (i == 0) "  (기본)" else "",
                            fontSize = 24.sp,
                            modifier = Modifier.weight(1f),
                        )
                        if (i > 0) {
                            TextButton(onClick = { draft = draft.copy(drivers = listOf(driver) + (draft.drivers - driver)) }) {
                                Text("기본으로", fontSize = 22.sp)
                            }
                        }
                        TextButton(onClick = { draft = draft.copy(drivers = draft.drivers - driver) }) { Text("삭제", fontSize = 22.sp) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    InfoField("성명", name, { name = it }, focusRequester = nameFocus)
                    // 키보드의 완료로 더하면 다음 사람을 바로 적도록 성명 칸으로 돌아간다.
                    InfoField("부서", department, { department = it }, ImeAction.Done, onDone = {
                        addDriver()
                        nameFocus.requestFocus()
                    })
                    OutlinedButton(onClick = { addDriver() }, enabled = name.isNotBlank()) { Text("추가", fontSize = 22.sp) }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                // 적어 놓고 추가를 누르지 않은 운전자도 넣는다.
                val saved = withTyped()
                onSave(
                    saved.copy(
                        company = saved.company.trim(),
                        businessNumber = saved.businessNumber.trim(),
                        carModel = saved.carModel.trim(),
                        plateNumber = saved.plateNumber.trim(),
                    ),
                )
            }) { Text("저장", fontSize = 24.sp) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소", fontSize = 24.sp) } },
    )
}

@Composable
private fun RowScope.InfoField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 20.sp) },
        singleLine = true,
        textStyle = TextStyle(fontSize = 26.sp),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() ?: defaultKeyboardAction(ImeAction.Done) }),
        modifier = Modifier
            .weight(1f)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
    )
}

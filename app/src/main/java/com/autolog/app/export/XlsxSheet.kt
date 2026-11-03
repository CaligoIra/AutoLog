package com.autolog.app.export

import java.io.OutputStream
import java.util.SortedMap
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 시트 한 장짜리 엑셀(xlsx) 파일을 만든다. 라이브러리 없이 xlsx를 이루는 XML 몇 개를 zip으로 묶는다.
 * 운행기록부 서식을 맞추는 데 필요한 것만 있다: 글자·숫자·수식 칸, 칸 합치기, 열 너비·행 높이,
 * 가로 A4 한 장 너비로 인쇄, 쪽마다 되풀이할 머리글 행. 행·열 번호는 엑셀처럼 1부터 센다.
 */
internal class XlsxSheet(private val name: String) {

    /** STYLES의 cellXfs 순서와 같다. */
    enum class Style { DEFAULT, TITLE, SECTION, HEADER, TEXT, CENTER, NUMBER, TOTAL, PERCENT }

    private sealed interface Value
    private class Text(val text: String) : Value
    private class Num(val value: Double, val formula: String?) : Value
    private class Cell(val value: Value?, val style: Style)

    private val rows: SortedMap<Int, SortedMap<Int, Cell>> = sortedMapOf()
    private val merged = mutableListOf<String>()
    private val heights = mutableMapOf<Int, Double>()
    private var widths = listOf<Double>()
    private var printTitles: IntRange? = null

    fun columns(vararg widths: Double) {
        this.widths = widths.toList()
    }

    fun height(row: Int, points: Double) {
        heights[row] = points
    }

    /** 인쇄할 때 쪽마다 맨 위에 다시 찍을 행(표 머리글). */
    fun repeatOnEachPage(rows: IntRange) {
        printTitles = rows
    }

    /** toRow·toCol을 주면 그 칸까지 합친다. */
    fun text(row: Int, col: Int, text: String, style: Style, toRow: Int = row, toCol: Int = col) =
        put(row, col, toRow, toCol, Cell(Text(text), style))

    /** formula가 있으면 엑셀이 다시 계산하고, value는 계산 전에 보일 값이다. */
    fun number(row: Int, col: Int, value: Double, style: Style, formula: String? = null, toRow: Int = row, toCol: Int = col) =
        put(row, col, toRow, toCol, Cell(Num(value, formula), style))

    /** 테두리만 있는 빈칸. */
    fun blank(row: Int, col: Int, style: Style, toCol: Int = col) = put(row, col, row, toCol, Cell(null, style))

    // 합친 칸의 테두리가 다 보이도록 나머지 칸에도 같은 스타일을 준다. 값은 왼쪽 위 칸에만 넣는다.
    private fun put(row: Int, col: Int, toRow: Int, toCol: Int, cell: Cell) {
        for (r in row..toRow) for (c in col..toCol) {
            rows.getOrPut(r) { sortedMapOf() }[c] = if (r == row && c == col) cell else Cell(null, cell.style)
        }
        if (toRow != row || toCol != col) merged += "${ref(row, col)}:${ref(toRow, toCol)}"
    }

    fun write(out: OutputStream) {
        ZipOutputStream(out).use { zip ->
            fun entry(path: String, xml: String) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(xml.toByteArray())
                zip.closeEntry()
            }
            entry("[Content_Types].xml", CONTENT_TYPES)
            entry("_rels/.rels", ROOT_RELS)
            entry("xl/workbook.xml", workbookXml())
            entry("xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
            entry("xl/styles.xml", STYLES)
            entry("xl/worksheets/sheet1.xml", sheetXml())
        }
    }

    private fun workbookXml() = buildString {
        append(XML_HEADER)
        append("""<workbook xmlns="$MAIN_NS" xmlns:r="$REL_NS"><sheets><sheet name="${escape(name)}" sheetId="1" r:id="rId1"/></sheets>""")
        printTitles?.let {
            append("""<definedNames><definedName name="_xlnm.Print_Titles" localSheetId="0">'${escape(name)}'!${'$'}${it.first}:${'$'}${it.last}</definedName></definedNames>""")
        }
        // 수식 칸은 열 때 다시 계산한다.
        append("""<calcPr calcId="191029" fullCalcOnLoad="1"/></workbook>""")
    }

    private fun sheetXml() = buildString {
        append(XML_HEADER)
        append("""<worksheet xmlns="$MAIN_NS" xmlns:r="$REL_NS"><sheetPr><pageSetUpPr fitToPage="1"/></sheetPr>""")
        if (widths.isNotEmpty()) {
            append("<cols>")
            widths.forEachIndexed { i, w -> append("""<col min="${i + 1}" max="${i + 1}" width="$w" customWidth="1"/>""") }
            append("</cols>")
        }
        append("<sheetData>")
        for ((r, cells) in rows) {
            append("""<row r="$r"""")
            heights[r]?.let { append(""" ht="$it" customHeight="1"""") }
            append(">")
            for ((c, cell) in cells) {
                append("""<c r="${ref(r, c)}" s="${cell.style.ordinal}"""")
                when (val v = cell.value) {
                    null -> append("/>")
                    is Text -> append(""" t="inlineStr"><is><t xml:space="preserve">${escape(v.text)}</t></is></c>""")
                    is Num -> {
                        append(">")
                        v.formula?.let { append("<f>${escape(it)}</f>") }
                        append("<v>${v.value}</v></c>")
                    }
                }
            }
            append("</row>")
        }
        append("</sheetData>")
        if (merged.isNotEmpty()) {
            append("""<mergeCells count="${merged.size}">""")
            merged.forEach { append("""<mergeCell ref="$it"/>""") }
            append("</mergeCells>")
        }
        append("""<pageMargins left="0.4" right="0.4" top="0.5" bottom="0.5" header="0.3" footer="0.3"/>""")
        append("""<pageSetup paperSize="9" orientation="landscape" fitToWidth="1" fitToHeight="0"/></worksheet>""")
    }

    companion object {
        /** 엑셀 칸 주소. ref(11, 6) = "F11" */
        fun ref(row: Int, col: Int): String {
            var n = col
            val letters = StringBuilder()
            while (n > 0) {
                letters.insert(0, 'A' + (n - 1) % 26)
                n = (n - 1) / 26
            }
            return "$letters$row"
        }

        // XML에 쓸 수 없는 제어 문자는 버린다.
        private fun escape(s: String) = buildString {
            for (ch in s) when {
                ch == '&' -> append("&amp;")
                ch == '<' -> append("&lt;")
                ch == '>' -> append("&gt;")
                ch == '"' -> append("&quot;")
                ch < ' ' && ch != '\n' && ch != '\t' -> Unit
                else -> append(ch)
            }
        }

        private const val XML_HEADER = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>"""
        private const val MAIN_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
        private const val REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"

        private const val CONTENT_TYPES = XML_HEADER +
            """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""" +
            """<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""" +
            """<Default Extension="xml" ContentType="application/xml"/>""" +
            """<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""" +
            """<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""" +
            """<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""" +
            "</Types>"

        private const val ROOT_RELS = XML_HEADER +
            """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
            """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>""" +
            "</Relationships>"

        private const val WORKBOOK_RELS = XML_HEADER +
            """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
            """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>""" +
            """<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""" +
            "</Relationships>"

        // 글꼴: 0 보통, 1 굵게, 2 제목. 채우기: 2 머리글 회색. 테두리: 1 가는 실선. 숫자 형식: 164 거리, 165 비율.
        private const val FONT = """<name val="맑은 고딕"/><family val="3"/><charset val="129"/>"""
        private const val THIN = """style="thin"><color auto="1"/>"""
        private const val MIDDLE = """vertical="center" wrapText="1""""
        private const val STYLES = XML_HEADER +
            """<styleSheet xmlns="$MAIN_NS">""" +
            """<numFmts count="2"><numFmt numFmtId="164" formatCode="#,##0.0"/><numFmt numFmtId="165" formatCode="0.0%"/></numFmts>""" +
            """<fonts count="3"><font><sz val="10"/>$FONT</font><font><b/><sz val="10"/>$FONT</font><font><b/><sz val="16"/>$FONT</font></fonts>""" +
            """<fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill>""" +
            """<fill><patternFill patternType="solid"><fgColor rgb="FFEDEDED"/><bgColor indexed="64"/></patternFill></fill></fills>""" +
            """<borders count="2"><border><left/><right/><top/><bottom/><diagonal/></border>""" +
            """<border><left $THIN</left><right $THIN</right><top $THIN</top><bottom $THIN</bottom><diagonal/></border></borders>""" +
            """<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""" +
            """<cellXfs count="9">""" +
            """<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>""" +
            """<xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment horizontal="center" $MIDDLE/></xf>""" +
            """<xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>""" +
            """<xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" $MIDDLE/></xf>""" +
            """<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment horizontal="left" indent="1" $MIDDLE/></xf>""" +
            """<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment horizontal="center" $MIDDLE/></xf>""" +
            """<xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" indent="1" vertical="center"/></xf>""" +
            """<xf numFmtId="164" fontId="1" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>""" +
            """<xf numFmtId="165" fontId="1" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>""" +
            "</cellXfs>" +
            """<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles></styleSheet>"""
    }
}

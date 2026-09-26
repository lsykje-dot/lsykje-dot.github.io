package com.lsykje.diary.pdf

import java.time.LocalDate

/**
 * PDF에서 추출한 글자를 일기 항목(날짜·제목·본문)으로 나눈다.
 *
 * 인식 규칙(설계 문서 7-4/7-4-1절): 줄이 "YYYY년 M월 D일 요일" 형식과 일치하면 그 줄을
 * 새 일기의 시작으로 보고, 바로 다음 줄을 제목, 그 뒤를 본문으로 취급한다. 우리 앱이 만든
 * PDF와 "나의일상" 앱이 만든 PDF가 정확히 이 순서를 따르므로 ①②번 경우는 자동으로 전부
 * 인식된다. 이 규칙에 맞는 날짜 줄을 하나도 찾지 못하면(③ 처음 보는 형식) 문서 전체를
 * date = null인 블록 하나로 돌려주고, 화면에서 사용자가 직접 날짜·제목·본문을 나눠 쓰게 한다.
 */
object PdfDiaryParser {

    data class Block(
        val date: LocalDate?,
        val title: String,
        val body: String,
    )

    // 줄 맨 앞에서 시작할 때만 "날짜 줄"로 인정 — 본문 중간에 "지난 9월 24일 화요일에" 같은
    // 문장이 있어도 날짜 줄로 잘못 인식하지 않도록 함
    private val DATE_LINE = Regex("""^(\d{4})년\s*(\d{1,2})월\s*(\d{1,2})일\s*(?:월|화|수|목|금|토|일)요일""")
    private val PAGE_FOOTER = Regex("""^-\s*\d+\s*-$""")
    private const val CONTINUED_PREFIX = "(이어서) "

    fun parse(rawText: String): List<Block> {
        val lines = rawText.lines().map { it.trim() }

        val blocks = mutableListOf<Building>()
        val unrecognized = StringBuilder()
        var current: Building? = null

        for (raw in lines) {
            if (raw.isBlank() || PAGE_FOOTER.matches(raw)) continue

            val isContinuedHeader = raw.startsWith(CONTINUED_PREFIX)
            val forMatch = if (isContinuedHeader) raw.removePrefix(CONTINUED_PREFIX) else raw
            val dateMatch = DATE_LINE.find(forMatch)

            when {
                dateMatch != null && isContinuedHeader -> {
                    // "(이어서) 날짜" — 같은 항목의 본문이 다음 페이지로 이어진다는 표시일 뿐,
                    // 새 일기가 아니므로 그냥 건너뛴다(현재 진행 중인 블록에 이어서 본문이 쌓임)
                }
                dateMatch != null -> {
                    current = Building(date = parseDate(dateMatch))
                    blocks += current
                }
                current == null -> {
                    // 아직 날짜를 하나도 못 찾은 상태 — 인식 실패 구간(③번 경우)
                    unrecognized.appendLine(raw)
                }
                current.title == null -> {
                    current.title = raw
                }
                else -> {
                    current.body.appendLine(raw)
                }
            }
        }

        val recognized = blocks.map {
            Block(date = it.date, title = it.title.orEmpty(), body = it.body.toString().trim())
        }

        val leftover = unrecognized.toString().trim()
        return if (leftover.isEmpty()) {
            recognized
        } else {
            // 인식하지 못한 부분은 맨 앞에 date=null 블록으로 붙여서, 사용자가 검토 화면에서
            // 직접 날짜를 지정하고 제목/본문을 나눠 쓸 수 있게 한다
            listOf(Block(date = null, title = "", body = leftover)) + recognized
        }
    }

    private fun parseDate(match: MatchResult): LocalDate? {
        val (year, month, day) = match.destructured
        return runCatching { LocalDate.of(year.toInt(), month.toInt(), day.toInt()) }.getOrNull()
    }

    private class Building(val date: LocalDate?) {
        var title: String? = null
        val body = StringBuilder()
    }
}

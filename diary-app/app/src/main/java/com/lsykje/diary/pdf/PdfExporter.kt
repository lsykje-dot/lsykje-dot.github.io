package com.lsykje.diary.pdf

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.lsykje.diary.data.EntryWithPhotos
import java.io.File
import java.io.FileOutputStream
import java.time.format.TextStyle
import java.util.Locale

/**
 * 설계 문서 7-3절: Android 표준 PdfDocument로 제목·본문·사진을 페이지에 그려 PDF를 만든다.
 *
 * 날짜 줄(1행) → 제목 줄(굵게) → 본문 순서로 그리는데, 이는 7-4-1절에서 분석한 "나의일상"
 * 앱 PDF와 같은 순서다. 그래야 나중에 "PDF 불러오기" 기능을 만들 때 우리 앱이 만든 PDF도
 * 같은 규칙(날짜 줄 → 굵은 제목 줄 → 본문)으로 자동 인식할 수 있다.
 *
 * 사진은 본문 뒤에 각각 한 장씩 새 페이지로 붙인다 — 본문 중간에 끼워 넣는 정교한 레이아웃
 * 대신, 페이지 넘김 계산이 훨씬 단순하고 안정적인 방식을 택함.
 */
object PdfExporter {
    private const val PAGE_WIDTH = 595 // A4, 72dpi 기준 pt
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f
    private val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2
    private val CONTENT_BOTTOM = PAGE_HEIGHT - MARGIN - 24f // 하단 쪽수 자리 확보

    private const val INK = 0xFF263029.toInt()
    private const val INK_SOFT = 0xFF5C6B60.toInt()

    private val datePaint = TextPaint().apply { isAntiAlias = true; color = INK; textSize = 12f }
    private val titlePaint = TextPaint().apply { isAntiAlias = true; color = INK; textSize = 16f; isFakeBoldText = true }
    private val bodyPaint = TextPaint().apply { isAntiAlias = true; color = INK; textSize = 11f }
    private val continuedPaint = TextPaint().apply { isAntiAlias = true; color = INK_SOFT; textSize = 10f }
    private val photoCaptionPaint = TextPaint().apply { isAntiAlias = true; color = INK_SOFT; textSize = 10f }
    private val footerPaint = Paint().apply { isAntiAlias = true; color = INK_SOFT; textSize = 9f; textAlign = Paint.Align.CENTER }

    fun exportDay(context: Context, entry: EntryWithPhotos): File =
        export(context, listOf(entry), fileName = "일기_${entry.entry.date}.pdf")

    fun exportRange(context: Context, entries: List<EntryWithPhotos>): File {
        val sorted = entries.sortedBy { it.entry.date }
        val first = sorted.first().entry.date
        val last = sorted.last().entry.date
        return export(context, sorted, fileName = "일기_${first}_${last}.pdf")
    }

    private fun export(context: Context, entries: List<EntryWithPhotos>, fileName: String): File {
        val document = PdfDocument()
        val cursor = Cursor(document)

        entries.forEach { item -> drawEntry(cursor, item) }
        cursor.finish()

        val dir = File(context.cacheDir, "pdf_exports").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()
        return file
    }

    private fun drawEntry(cursor: Cursor, item: EntryWithPhotos) {
        val weekday = item.entry.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)
        val dateLine = "${item.entry.date.year}년 ${item.entry.date.monthValue}월 ${item.entry.date.dayOfMonth}일 $weekday"

        cursor.page.canvas.drawText(dateLine, MARGIN, cursor.y + datePaint.textSize, datePaint)
        cursor.y += datePaint.textSize + 18f

        val titleLayout = buildLayout(item.entry.title.ifBlank { "(제목 없음)" }, titlePaint, CONTENT_WIDTH.toInt())
        drawLayout(cursor.page, titleLayout, MARGIN, cursor.y)
        cursor.y += titleLayout.height + 18f

        drawBody(cursor, item.entry.content, dateLine)
        drawPhotos(cursor, item, dateLine)
    }

    /** 본문이 남은 페이지 공간보다 길면 "(이어서) 날짜" 헤더와 함께 새 페이지로 이어서 그린다 */
    private fun drawBody(cursor: Cursor, content: String, dateLine: String) {
        var remaining = content
        while (remaining.isNotEmpty()) {
            val availableHeight = (CONTENT_BOTTOM - cursor.y).coerceAtLeast(0f)
            val fitLength = if (availableHeight < bodyPaint.textSize * 2) {
                0
            } else {
                findFitLength(remaining, bodyPaint, CONTENT_WIDTH.toInt(), availableHeight.toInt())
            }

            if (fitLength <= 0) {
                cursor.newPage(continuedLabel = "(이어서) $dateLine")
                continue
            }

            val chunk = remaining.substring(0, fitLength)
            val chunkLayout = buildLayout(chunk, bodyPaint, CONTENT_WIDTH.toInt())
            drawLayout(cursor.page, chunkLayout, MARGIN, cursor.y)
            cursor.y += chunkLayout.height

            remaining = remaining.substring(fitLength)
            if (remaining.isNotEmpty()) {
                cursor.newPage(continuedLabel = "(이어서) $dateLine")
            }
        }
    }

    private fun drawPhotos(cursor: Cursor, item: EntryWithPhotos, dateLine: String) {
        item.photos.forEach { photo ->
            val bitmap = runCatching { BitmapFactory.decodeFile(photo.filePath) }.getOrNull() ?: return@forEach

            cursor.newPage()
            cursor.page.canvas.drawText(dateLine, MARGIN, cursor.y + photoCaptionPaint.textSize, photoCaptionPaint)
            val photoTop = cursor.y + photoCaptionPaint.textSize + 16f

            val maxWidth = CONTENT_WIDTH
            val maxHeight = CONTENT_BOTTOM - photoTop
            val scale = minOf(maxWidth / bitmap.width, maxHeight / bitmap.height, 1f)
            val destWidth = bitmap.width * scale
            val destHeight = bitmap.height * scale
            val left = MARGIN + (maxWidth - destWidth) / 2f
            val rect = RectF(left, photoTop, left + destWidth, photoTop + destHeight)
            cursor.page.canvas.drawBitmap(bitmap, null, rect, Paint(Paint.ANTI_ALIAS_FLAG))
            bitmap.recycle()

            cursor.y = photoTop + destHeight
        }
    }

    private fun buildLayout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(4f, 1f)
            .build()

    private fun drawLayout(page: PdfDocument.Page, layout: StaticLayout, x: Float, y: Float) {
        page.canvas.save()
        page.canvas.translate(x, y)
        layout.draw(page.canvas)
        page.canvas.restore()
    }

    /** 주어진 높이(maxHeight) 안에 몇 글자까지 들어가는지 계산 */
    private fun findFitLength(text: String, paint: TextPaint, width: Int, maxHeight: Int): Int {
        val layout = buildLayout(text, paint, width)
        if (layout.height <= maxHeight) return text.length
        for (line in 0 until layout.lineCount) {
            if (layout.getLineBottom(line) > maxHeight) {
                return if (line == 0) 0 else layout.getLineEnd(line - 1)
            }
        }
        return text.length
    }

    private fun newPageInfo(pageNumber: Int) =
        PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()

    /** 문서 전체에 걸친 "지금 그리고 있는 페이지"를 들고 다니는 작은 상태 홀더 */
    private class Cursor(private val document: PdfDocument) {
        var pageNumber = 1
            private set
        var page: PdfDocument.Page = document.startPage(newPageInfo(pageNumber))
            private set
        var y = MARGIN

        fun newPage(continuedLabel: String? = null) {
            page.canvas.drawText("- $pageNumber -", PAGE_WIDTH / 2f, PAGE_HEIGHT - 28f, footerPaint)
            document.finishPage(page)

            pageNumber += 1
            page = document.startPage(newPageInfo(pageNumber))
            y = MARGIN

            if (continuedLabel != null) {
                page.canvas.drawText(continuedLabel, MARGIN, y + continuedPaint.textSize, continuedPaint)
                y += continuedPaint.textSize + 18f
            }
        }

        fun finish() {
            page.canvas.drawText("- $pageNumber -", PAGE_WIDTH / 2f, PAGE_HEIGHT - 28f, footerPaint)
            document.finishPage(page)
        }
    }
}

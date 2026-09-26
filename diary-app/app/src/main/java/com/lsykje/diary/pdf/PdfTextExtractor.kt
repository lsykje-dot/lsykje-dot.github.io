package com.lsykje.diary.pdf

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

/** PdfBox-Android로 PDF 파일의 글자를 그대로 추출한다(레이아웃 인식은 PdfDiaryParser가 담당) */
object PdfTextExtractor {

    @Volatile
    private var initialized = false

    fun extractText(context: Context, uri: Uri): String {
        ensureInitialized(context)

        val resolver = context.contentResolver
        val document = resolver.openInputStream(uri)?.use { input -> PDDocument.load(input) }
            ?: error("PDF 파일을 열 수 없습니다: $uri")

        return document.use {
            val stripper = PDFTextStripper()
            stripper.sortByPosition = true
            stripper.getText(it)
        }
    }

    private fun ensureInitialized(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (!initialized) {
                PDFBoxResourceLoader.init(context.applicationContext)
                initialized = true
            }
        }
    }
}

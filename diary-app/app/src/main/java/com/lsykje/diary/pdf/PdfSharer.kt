package com.lsykje.diary.pdf

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** PDF 파일을 공유 시트(카카오톡·이메일·"파일로 저장" 등)로 넘긴다. 별도 저장 권한이 필요 없음 */
object PdfSharer {
    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "PDF 공유 또는 저장"))
    }
}

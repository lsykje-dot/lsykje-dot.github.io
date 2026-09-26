package com.lsykje.diary.backup

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** 백업 zip 파일을 공유 시트(파일로 저장·다른 앱으로 보내기 등)로 넘긴다 */
object BackupSharer {
    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "백업 파일 저장 또는 공유"))
    }
}

package com.lsykje.diary.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.lsykje.diary.data.AppDatabase
import com.lsykje.diary.data.DiaryRepository
import com.lsykje.diary.data.PhotoStorage
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 설계 문서 7-5절: 일기 DB 파일 + 사진 파일 전체를 zip 하나로 백업하고, 다시 불러올 수 있게 한다.
 * 복원은 "덮어쓰기"(백업 내용으로 통째로 교체)와 "합치기"(지금 데이터는 그대로 두고, 없는
 * 날짜만 백업에서 채워 넣음) 두 가지를 지원한다.
 */
object BackupManager {

    data class RestoreResult(val importedCount: Int, val skippedCount: Int)

    fun exportBackup(context: Context): File {
        checkpointDatabase(context)

        val dir = File(context.cacheDir, "backups").apply { mkdirs() }
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
        val zipFile = File(dir, "나의일기장_백업_$timestamp.zip")

        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
            addFileToZip(zip, AppDatabase.databaseFile(context), "diary.db")
            PhotoStorage.photosDir(context).listFiles()?.forEach { photo ->
                addFileToZip(zip, photo, "photos/${photo.name}")
            }
        }
        return zipFile
    }

    /** 백업 내용으로 지금 데이터를 통째로 교체한다. 호출 후에는 앱 화면을 처음부터 다시 시작해야 한다 */
    suspend fun restoreOverwrite(context: Context, uri: Uri): Int {
        val staging = extractToStaging(context, uri)
        try {
            AppDatabase.closeAndReset()

            val dbFile = AppDatabase.databaseFile(context)
            dbFile.parentFile?.mkdirs()
            File(staging, "diary.db").copyTo(dbFile, overwrite = true)

            val photosDir = PhotoStorage.photosDir(context)
            photosDir.listFiles()?.forEach { it.delete() }
            File(staging, "photos").listFiles()?.forEach { photo ->
                photo.copyTo(File(photosDir, photo.name), overwrite = true)
            }

            remapPhotoPathsToThisDevice(context)

            return DiaryRepository(context).countEntries()
        } finally {
            staging.deleteRecursively()
        }
    }

    /** 지금 있는 일기는 그대로 두고, 백업에만 있는 날짜(지금 데이터에 없는 날짜)만 추가한다 */
    suspend fun restoreMerge(context: Context, uri: Uri): RestoreResult {
        val staging = extractToStaging(context, uri)
        try {
            val repository = DiaryRepository(context)
            val stagedEntries = readStagedEntries(File(staging, "diary.db"))

            var imported = 0
            var skipped = 0
            stagedEntries.forEach { entry ->
                if (repository.findByDate(entry.date) != null) {
                    skipped++
                } else {
                    val photoPaths = entry.photoFileNames.mapNotNull { name ->
                        val source = File(staging, "photos/$name")
                        if (source.exists()) copyPhotoToAppStorage(context, source) else null
                    }
                    repository.importEntry(entry.date, entry.title, entry.content, photoPaths)
                    imported++
                }
            }
            return RestoreResult(imported, skipped)
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun checkpointDatabase(context: Context) {
        val db = AppDatabase.get(context).openHelper.writableDatabase
        db.query("PRAGMA wal_checkpoint(FULL)").use { }
    }

    private fun addFileToZip(zip: ZipOutputStream, file: File, entryName: String) {
        if (!file.exists()) return
        zip.putNextEntry(ZipEntry(entryName))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    /** 백업 zip을 캐시 폴더 아래 임시 폴더에 풀어놓는다. zip slip(경로 조작) 항목은 거부한다 */
    private fun extractToStaging(context: Context, uri: Uri): File {
        val staging = File(context.cacheDir, "restore_staging").apply {
            deleteRecursively()
            mkdirs()
        }
        val stagingRoot = staging.canonicalPath + File.separator

        val input = context.contentResolver.openInputStream(uri) ?: error("백업 파일을 열 수 없습니다")
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val outFile = File(staging, entry.name)
                    if (!outFile.canonicalPath.startsWith(stagingRoot)) {
                        error("올바르지 않은 백업 파일이에요")
                    }
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { out -> zip.copyTo(out) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        if (!File(staging, "diary.db").exists()) {
            staging.deleteRecursively()
            error("올바른 백업 파일이 아니에요")
        }
        return staging
    }

    private fun copyPhotoToAppStorage(context: Context, source: File): String {
        val dest = File(PhotoStorage.photosDir(context), "${UUID.randomUUID()}.jpg")
        source.copyTo(dest, overwrite = true)
        return dest.absolutePath
    }

    /**
     * 덮어쓰기로 옮겨온 DB 안의 filePath는 백업을 만든 기기 기준 절대경로라 이 기기와
     * 다를 수 있으므로, 실제로 사진을 복사해 둔 이 기기의 경로로 다시 맞춰준다
     */
    private fun remapPhotoPathsToThisDevice(context: Context) {
        val photosDir = PhotoStorage.photosDir(context)
        val db = AppDatabase.get(context).openHelper.writableDatabase

        val updates = mutableListOf<Pair<Long, String>>()
        db.query("SELECT id, filePath FROM diary_photo").use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val oldPath = cursor.getString(1)
                val newPath = File(photosDir, File(oldPath).name).absolutePath
                if (newPath != oldPath) updates += id to newPath
            }
        }
        updates.forEach { (id, newPath) ->
            db.execSQL("UPDATE diary_photo SET filePath = ? WHERE id = ?", arrayOf(newPath, id))
        }
    }

    private data class StagedEntry(
        val date: LocalDate,
        val title: String,
        val content: String,
        val photoFileNames: List<String>,
    )

    /** Room을 거치지 않고, 압축을 푼 백업 DB 파일을 직접 읽기 전용으로 열어서 내용을 읽는다 */
    private fun readStagedEntries(dbFile: File): List<StagedEntry> {
        val database = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
        try {
            val entries = mutableListOf<StagedEntry>()
            database.rawQuery("SELECT id, date, title, content FROM diary_entry", null).use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val date = LocalDate.ofEpochDay(cursor.getLong(1))
                    val title = cursor.getString(2) ?: ""
                    val content = cursor.getString(3) ?: ""

                    val photoNames = mutableListOf<String>()
                    database.rawQuery(
                        "SELECT filePath FROM diary_photo WHERE entryId = ? ORDER BY orderIndex",
                        arrayOf(id.toString()),
                    ).use { photoCursor ->
                        while (photoCursor.moveToNext()) {
                            photoNames += File(photoCursor.getString(0)).name
                        }
                    }
                    entries += StagedEntry(date, title, content, photoNames)
                }
            }
            return entries
        } finally {
            database.close()
        }
    }
}

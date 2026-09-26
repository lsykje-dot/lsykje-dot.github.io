package com.lsykje.diary.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

class DiaryRepository(private val context: Context) {
    private val dao = AppDatabase.get(context).diaryDao()

    fun observeByDate(date: LocalDate): Flow<EntryWithPhotos?> = dao.observeByDate(date)

    fun observeDatesInMonth(month: YearMonth): Flow<List<LocalDate>> =
        dao.observeDatesInRange(month.atDay(1), month.atEndOfMonth())

    fun observeRecent(limit: Int = 5): Flow<List<EntryWithPhotos>> = dao.observeRecent(limit)

    fun observeEntriesInRange(from: LocalDate, to: LocalDate): Flow<List<EntryWithPhotos>> =
        dao.observeEntriesInRange(from, to)

    suspend fun findByDate(date: LocalDate): DiaryEntry? = dao.findByDate(date)

    suspend fun countEntries(): Int = dao.countEntries()

    /**
     * 백업 복원(합치기)용: 이미 이 날짜에 일기가 없는 것이 확인된 상태에서, 사진이 이미
     * 앱 저장소 안에 자리 잡은 상태로 호출한다(리사이즈·복사는 이미 끝난 경로만 받음).
     */
    suspend fun importEntry(date: LocalDate, title: String, content: String, photoPaths: List<String>) {
        val now = System.currentTimeMillis()
        val entryId = dao.upsertEntry(
            DiaryEntry(date = date, title = title, content = content, createdAt = now, updatedAt = now),
        )
        photoPaths.forEachIndexed { index, path ->
            dao.insertPhoto(DiaryPhoto(entryId = entryId, filePath = path, orderIndex = index))
        }
    }

    /**
     * 제목/본문과, 새로 추가된 사진 Uri 목록, 남겨둘 기존 사진 목록을 받아 하루 일기를 저장한다.
     * 사진은 PhotoStorage로 리사이즈·압축해 내부 저장소에 복사한 뒤 경로만 DB에 저장한다.
     */
    suspend fun saveEntry(
        date: LocalDate,
        title: String,
        content: String,
        keepPhotos: List<DiaryPhoto>,
        newPhotoUris: List<Uri>,
    ) {
        val now = System.currentTimeMillis()
        val existing = dao.findByDate(date)
        val entry = DiaryEntry(
            id = existing?.id ?: 0,
            date = date,
            title = title,
            content = content,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        val entryId = dao.upsertEntry(entry)

        val removedPhotos = existing?.let { dao.photosForEntry(it.id) }
            ?.filterNot { old -> keepPhotos.any { it.id == old.id } }
            ?: emptyList()
        removedPhotos.forEach { PhotoStorage.delete(it.filePath) }
        dao.deletePhotosForEntry(entryId)

        keepPhotos.forEachIndexed { index, photo ->
            dao.insertPhoto(photo.copy(id = 0, entryId = entryId, orderIndex = index))
        }
        val startIndex = keepPhotos.size
        newPhotoUris.forEachIndexed { offset, uri ->
            val path = PhotoStorage.importAndResize(context, uri)
            dao.insertPhoto(DiaryPhoto(entryId = entryId, filePath = path, orderIndex = startIndex + offset))
        }
    }

    suspend fun deleteEntry(entryWithPhotos: EntryWithPhotos) {
        entryWithPhotos.photos.forEach { PhotoStorage.delete(it.filePath) }
        dao.deleteEntry(entryWithPhotos.entry)
    }
}

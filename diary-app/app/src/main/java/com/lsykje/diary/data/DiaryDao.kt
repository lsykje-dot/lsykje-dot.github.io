package com.lsykje.diary.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DiaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entry: DiaryEntry): Long

    @Update
    suspend fun updateEntry(entry: DiaryEntry)

    @Delete
    suspend fun deleteEntry(entry: DiaryEntry)

    @Query("SELECT * FROM diary_entry WHERE date = :date LIMIT 1")
    suspend fun findByDate(date: LocalDate): DiaryEntry?

    @Transaction
    @Query("SELECT * FROM diary_entry WHERE date = :date LIMIT 1")
    fun observeByDate(date: LocalDate): Flow<EntryWithPhotos?>

    /** 달력 화면에서 "일기가 있는 날짜"에 점을 표시하기 위한 날짜 목록 조회 */
    @Query(
        "SELECT date FROM diary_entry WHERE date BETWEEN :from AND :to"
    )
    fun observeDatesInRange(from: LocalDate, to: LocalDate): Flow<List<LocalDate>>

    @Transaction
    @Query("SELECT * FROM diary_entry ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int = 5): Flow<List<EntryWithPhotos>>

    /** PDF 기간 내보내기용: 지정한 기간의 일기를 날짜 오름차순으로 조회 */
    @Transaction
    @Query("SELECT * FROM diary_entry WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun observeEntriesInRange(from: LocalDate, to: LocalDate): Flow<List<EntryWithPhotos>>

    @Insert
    suspend fun insertPhoto(photo: DiaryPhoto): Long

    @Query("SELECT * FROM diary_photo WHERE entryId = :entryId ORDER BY orderIndex")
    suspend fun photosForEntry(entryId: Long): List<DiaryPhoto>

    @Query("DELETE FROM diary_photo WHERE entryId = :entryId")
    suspend fun deletePhotosForEntry(entryId: Long)
}

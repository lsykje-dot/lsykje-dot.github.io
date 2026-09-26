package com.lsykje.diary.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * 설계 문서 6-1절 "일기 표(diary_entry)"에 대응.
 * date는 하루에 일기 1개만 허용하는 기준 키라서 unique index를 둠.
 */
@Entity(tableName = "diary_entry", indices = [Index(value = ["date"], unique = true)])
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
)

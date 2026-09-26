package com.lsykje.diary.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/** 설계 문서 6-2절 "사진 표(diary_photo)"에 대응. filePath는 앱 내부 보관함에 복사된 실제 파일 경로 */
@Entity(
    tableName = "diary_photo",
    foreignKeys = [
        ForeignKey(
            entity = DiaryEntry::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
)
data class DiaryPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val filePath: String,
    val orderIndex: Int,
)

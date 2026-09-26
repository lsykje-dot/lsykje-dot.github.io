package com.lsykje.diary.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import java.io.File

@Database(entities = [DiaryEntry::class, DiaryPhoto::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao

    companion object {
        private const val DB_NAME = "diary.db"

        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        fun databaseFile(context: Context): File = context.getDatabasePath(DB_NAME)

        /**
         * 백업 복원(덮어쓰기)처럼 DB 파일 자체를 통째로 교체해야 할 때 쓴다.
         * 열려 있는 연결을 닫고 인스턴스를 비워서, 다음 get() 호출에서 교체된 파일로
         * 새로 연결하게 만든다. 이 함수를 호출한 뒤에는 그 전에 만들어둔 Repository/DAO를
         * 더 이상 쓰면 안 되므로, 호출 쪽에서 화면을 다시 시작해야 한다.
         */
        fun closeAndReset() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }

        private fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME).build()
    }
}

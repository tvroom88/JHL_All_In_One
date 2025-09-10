package com.aio.jhl_all_in_one.utils.room

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.aio.jhl_all_in_one.data.BookData

@Database(
    version = 3,
    entities = [BookData::class],
    exportSchema = true, // ✅ 여기에 설정
    autoMigrations = [
        AutoMigration(from = 2, to = 3)
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
}
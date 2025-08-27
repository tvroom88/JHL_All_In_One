package com.aio.jhl_all_in_one.utils.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aio.jhl_all_in_one.data.BookData

@Database(entities = [BookData::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
}
package com.aio.jhl_all_in_one.utils

import android.content.Context
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.aio.jhl_all_in_one.data.BookData
import com.aio.jhl_all_in_one.utils.room.DatabaseProvider

class RoomUtils(context: Context) {
    private val bookDao = DatabaseProvider.getDatabase(context).bookDao()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(book: BookData) = bookDao.insert(book)
    suspend fun delete(book: BookData) = bookDao.delete(book)
    suspend fun getAll() = bookDao.getAll()
}
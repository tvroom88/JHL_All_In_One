package com.aio.jhl_all_in_one.utils

import android.content.Context
import com.aio.jhl_all_in_one.data.BookData
import com.aio.jhl_all_in_one.utils.room.DatabaseProvider

class RoomUtils(context: Context) {
    private val bookDao = DatabaseProvider.getDatabase(context).bookDao()
    suspend fun insert(book: BookData) = bookDao.insert(book)
    suspend fun delete(book: BookData) = bookDao.delete(book)
    suspend fun getAll() = bookDao.getAll()
}
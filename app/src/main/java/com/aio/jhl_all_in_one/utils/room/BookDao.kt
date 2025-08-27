package com.aio.jhl_all_in_one.utils.room

import androidx.room.*
import com.aio.jhl_all_in_one.data.BookData

@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: BookData)

    @Query("SELECT * FROM book_table")
    suspend fun getAll(): List<BookData>

    @Delete
    suspend fun delete(book: BookData)
}
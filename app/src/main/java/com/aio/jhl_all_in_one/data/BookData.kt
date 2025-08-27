package com.aio.jhl_all_in_one.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "book_table")
data class BookData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String
)
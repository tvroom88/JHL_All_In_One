package com.aio.jhl_all_in_one.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "book_table")
data class BookData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val author: String = "", // 기존 데이터 호환을 위해 기본값 추가
    @ColumnInfo(defaultValue = "")  // 반드시 추가
    val content: String = ""   // 새로 추가한 컬럼, 기본값 필수
)
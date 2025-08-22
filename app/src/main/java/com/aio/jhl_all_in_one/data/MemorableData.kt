package com.aio.jhl_all_in_one.data

data class MemorableData(
    val id: String = "",   // Firestore documentId
    val bookName: String = "",
    val author: String = "",
    val sentence: String = "",
    val page: String = ""
)
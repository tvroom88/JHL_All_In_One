package com.aio.jhl_all_in_one.utils

import android.util.Log
import com.aio.jhl_all_in_one.Const
import com.aio.jhl_all_in_one.data.MemorableData
import com.google.firebase.firestore.FirebaseFirestore

class FireStoreUtils {

    fun saveDataToFirebase(
        collectionName: String,
        documentName: String,
        data: Any
    ) {
        FirebaseFirestore.getInstance()
            .collection(collectionName)
            .document(documentName)
            .set(data)
            .addOnSuccessListener {
                Log.d("sendDataToServer", "success")
            }
            .addOnFailureListener {
                Log.d("sendDataToServer", "fail : $it")
            }
    }


    fun fetchMemorableData(
        onResult: (List<MemorableData>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        db.collection(Const.FireBaseKeyWord.GoodSentenceFromBook)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val dataList = querySnapshot.documents.mapNotNull { doc ->
                    val map = doc.data
                    map?.let {
                        MemorableData(
                            bookName = it["bookName"] as? String ?: "",
                            author = it["author"] as? String ?: "",
                            sentence = it["sentence"] as? String ?: "",
                            page = it["page"] as? String ?: ""
                        )
                    }
                }
                onResult(dataList)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }
}
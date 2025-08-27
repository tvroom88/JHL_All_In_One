package com.aio.jhl_all_in_one.ui.imagecapture

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aio.jhl_all_in_one.Const
import com.aio.jhl_all_in_one.data.BookData
import com.aio.jhl_all_in_one.data.MemorableData
import com.aio.jhl_all_in_one.utils.FireStoreUtils
import com.aio.jhl_all_in_one.utils.RoomUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImageCaptureScreenViewModel : ViewModel() {

    // 처음 선택모드로 갈지 안갈지 선택하는 부분
    var goToChoosePictureSource by mutableStateOf(false)

    // 선택 모드
    var chooseGetPictureMode by mutableStateOf<PictureSource?>(null)

    // 캡처된 이미지
    var capturedBitmap by mutableStateOf<Bitmap?>(null)
    var capturedBitmapUri by mutableStateOf<Uri?>(null)

    // 크롭된 이미지
    var croppedBitmap by mutableStateOf<Bitmap?>(null)

    // OCR 결과 텍스트
    var textFromOcr by mutableStateOf<String?>(null)

    // 등록된 내용을 저장하기 위한 부분
    var currentMode = CurrentMode.SENTENCE
    var sentence by mutableStateOf<String>("")
    var book by mutableStateOf<String>("")
    var author by mutableStateOf<String>("")
    var page by mutableStateOf<String>("")

    val fireStoreUtils = FireStoreUtils()

    fun ocrFromImage(croppedBitmap: Bitmap, result: (String) -> Unit) {
        // When using Korean script library
        val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

        val image: InputImage
        try {
            image = InputImage.fromBitmap(croppedBitmap, 0)
            val result = recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    // Task completed successfully
                    // ...
                    Log.d("ImageCaptureScreenViewModel", "visionText : ${visionText.text}")
                    result(visionText.text)
                }
                .addOnFailureListener { e ->
                    // Task failed with an exception
                    // ...
                    Log.d("ImageCaptureScreenViewModel", "error : $e")
                    e.message?.let { result(it) }
                }
        } catch (e: IOException) {
            Log.d("ImageCaptureScreenViewModel", "e : ${e.message}")
            e.message?.let { result(it) }
        }
    }

    fun sendDataToServer(data: MemorableData) {
        fireStoreUtils.saveDataToFirebase(
            Const.FireBaseKeyWord.GoodSentenceFromBook,
            getTimeData(),
            data
        )
    }

    fun getTimeData(): String {
        val currentDate = Date()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedDate = sdf.format(currentDate)

        return formattedDate
    }

    fun addBook(book: BookData, context: Context) {
        viewModelScope.launch {
            RoomUtils(context).insert(book)
        }
    }
}

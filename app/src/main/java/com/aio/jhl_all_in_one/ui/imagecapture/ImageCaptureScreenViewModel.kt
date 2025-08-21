package com.aio.jhl_all_in_one.ui.imagecapture

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import java.io.IOException

class ImageCaptureScreenViewModel : ViewModel() {

    // 선택 모드
    var chooseGetPictureMode by mutableStateOf<PictureSource?>(null)

    // 캡처된 이미지
    var capturedBitmap by mutableStateOf<Bitmap?>(null)
    var capturedBitmapUri by mutableStateOf<Uri?>(null)

    // 크롭된 이미지
    var croppedBitmap by mutableStateOf<Bitmap?>(null)

    // OCR 결과 텍스트
    var textFromOcr by mutableStateOf<String?>(null)


//    @Composable
//    fun getImageFromGallery(context: Context) {
//        // 갤러리 런처
//        val galleryLauncher = rememberLauncherForActivityResult(
//            contract = ActivityResultContracts.GetContent()
//        ) { uri: Uri? ->
//            uri?.let {
//                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
////                onImageSelected(bitmap, it)
////                capturedBitmap = bitmap
////                capturedUri = it
//            }
//        }
//
//        LaunchedEffect(Unit) {
//            galleryLauncher.launch("image/*")
//        }
//    }

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
}

package com.aio.jhl_all_in_one.ui.imagecapture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ImageCaptureScreen() {
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Accompanist 권한 상태
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    // 권한 요청 시작
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    when {
        cameraPermissionState.status.isGranted -> {
            // 권한 있음 → 기존 로직
            if (capturedBitmap == null) {
                Camera(
                    onPhotoCaptured = { bitmap ->
                        capturedBitmap = bitmap
                    }
                )
            } else {
                ShowImageScreen(
                    bitmap = capturedBitmap!!,
                    onRetake = { capturedBitmap = null }
                )
            }
        }

        cameraPermissionState.status.shouldShowRationale -> {
            // 권한 거부했지만 재요청 가능
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("카메라 권한이 필요합니다. 권한을 허용해주세요.")
            }
        }

        else -> {
            // 권한 요청 거부 & 재요청 불가
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("카메라 권한이 거부되었습니다. 앱 설정에서 권한을 허용해주세요.")
            }
        }
    }
}

/**
 * 카메라 프리뷰 화면을 보여주고, 촬영 버튼을 통해 사진을 캡처하는 Composable
 *
 * @param onPhotoCaptured 캡처된 사진(Bitmap)을 외부로 전달하는 콜백
 */
@Composable
fun Camera(onPhotoCaptured: (Bitmap) -> Unit){

    val context = LocalContext.current // context: 안드로이드 시스템 Context (카메라 Provider 실행 시 필요)
    val lifecycleOwner = LocalLifecycleOwner.current // lifecycleOwner: 카메라가 Activity/Fragment 라이프사이클에 맞춰 동작하도록 연결

    // 사진 촬영용 CameraX UseCase (미리보기와 함께 바인딩)
    val imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
        .setTargetRotation(context.display?.rotation ?: Surface.ROTATION_0)
        .build()

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            lifecycleOwner = lifecycleOwner,
            imageCapture = imageCapture
        )

        // 캡처 버튼
        IconButton(
            onClick = {
                takePhoto(context, imageCapture, onPhotoCaptured)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
                .size(72.dp)
                .background(Color.White, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Capture",
                tint = Color.Black
            )
        }
    }
}

/**
 * CameraX의 [ImageCapture]로 사진을 촬영해 임시 파일(cacheDir)에 저장한 뒤, 해당 파일을 [Bitmap]으로 디코딩하여 [onPhotoCaptured] 콜백으로 전달합니다.
 *
 * - 촬영과 저장은 비동기로 수행되며, 콜백은 [ContextCompat.getMainExecutor]를 사용하므로 메인 스레드에서 호출됩니다.
 * - 파일은 앱의 캐시 디렉터리에 생성됩니다(일시 저장 용도). 영구 보관이 필요하면 MediaStore나 앱 전용 저장소로 옮기세요.
 *
 * @param context        메인 실행자와 캐시 디렉터리 경로를 얻기 위한 [Context]
 * @param imageCapture   이미 라이프사이클에 바인딩되어 있는 CameraX [ImageCapture] UseCase
 * @param onPhotoCaptured 저장/디코딩에 성공했을 때 호출되는 콜백(촬영된 [Bitmap] 전달)
 *
 * 참고:
 * - 실패 시 [ImageCapture.OnImageSavedCallback.onError]가 호출됩니다(예: 저장 실패, 카메라 오류).
 * - 고해상도 이미지를 그대로 디코딩하면 메모리 사용량이 커질 수 있습니다(다운샘플링 고려).
 */

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onPhotoCaptured: (Bitmap) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(context)

    // 캐시 디렉터리에 임시 파일 생성
    val file = File(
        context.cacheDir,
        "captured-${System.currentTimeMillis()}.jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                try {
                    // BitmapFactory 옵션 설정: inScaled=false → 원본 크기 유지
                    val options = BitmapFactory.Options().apply {
                        inScaled = false
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }

                    // 파일을 Bitmap으로 디코딩
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)

                    // EXIF 정보 읽기
                    val exif = ExifInterface(file.absolutePath)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )

                    val rotation = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }

                    // 회전 보정 (Matrix scale=false로 원본 크기 유지)
                    val rotatedBitmap = if (rotation != 0f) {
                        val matrix = Matrix().apply { postRotate(rotation) }
                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
                    } else {
                        bitmap
                    }

                    // 콜백 전달
                    onPhotoCaptured(rotatedBitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onError(exc: ImageCaptureException) {
                exc.printStackTrace()
            }
        }
    )
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner,
    imageCapture: ImageCapture
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

@Composable
fun ShowImageScreen(bitmap: Bitmap, onRetake: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Captured Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 다시 찍기 버튼
        IconButton(
            onClick = onRetake,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Retake",
                tint = Color.White
            )
        }
    }
}

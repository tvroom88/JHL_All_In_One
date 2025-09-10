package com.aio.jhl_all_in_one.ui.imagecapture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aio.jhl_all_in_one.data.BookData
import com.aio.jhl_all_in_one.data.MemorableData
import com.aio.jhl_all_in_one.utils.RoomUtils
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import java.io.File

/**
 * 순서 :
 * (1) 카메라 -> (2) crop -> (3) 사진 확인 -> (4) mlkit
 */

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ImageCaptureScreen(viewModel: ImageCaptureScreenViewModel = viewModel()) {
    // Accompanist 권한 상태
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    var galleryLauncher: ManagedActivityResultLauncher<String, Uri?>? = null

    val mContext = LocalContext.current

    // 권한 요청 시작
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    fun initSettings() {
        viewModel.capturedBitmap = null
        viewModel.croppedBitmap = null
        viewModel.capturedBitmapUri = null
        viewModel.textFromOcr = null
        viewModel.chooseGetPictureMode = null
    }

    when {
        cameraPermissionState.status.isGranted -> {
            // 권한 있음 → 기존 로직

            if (!viewModel.goToChoosePictureSource) {
                ResultScreen(
                    viewModel = viewModel,
                    goToOcr = { mode ->
                        viewModel.goToChoosePictureSource = true
                        viewModel.currentMode = mode
                    },
                    onChange = { changedTxt -> viewModel.textFromOcr = changedTxt },
                    onBack = { initSettings() },
                    onSaveInLocal = { viewModel.addBook(it, mContext) },
                    onSaveInRemote = { memorableData -> viewModel.sendDataToServer(memorableData) }
                )
            } else if (viewModel.chooseGetPictureMode == null) {
                chooseGetPictureScreen(
                    pictureSource = { source -> viewModel.chooseGetPictureMode = source },
                    onBack = { viewModel.goToChoosePictureSource = false }
                )
            } else if (viewModel.capturedBitmap == null) {
                if (viewModel.chooseGetPictureMode == PictureSource.GALLERY) {

                    galleryLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        uri?.let {
                            viewModel.capturedBitmapUri = it
                            viewModel.capturedBitmap =
                                MediaStore.Images.Media.getBitmap(mContext.contentResolver, it)
                        }
                    }
                    LaunchedEffect(Unit) {
                        galleryLauncher.launch("image/*")
                    }

                } else if (viewModel.chooseGetPictureMode == PictureSource.CAMERA) {
                    Camera(
                        onPhotoCaptured = { bitmap, uri ->
                            viewModel.capturedBitmap = bitmap
                            viewModel.capturedBitmapUri = uri
                        }
                    )
                }

            } else if (viewModel.croppedBitmap == null) {
                ImageCropperScreen(
                    capturedBitmapUri = viewModel.capturedBitmapUri,
                    onCropCaptureSuccess = { tempCroppedBitmap ->
                        viewModel.croppedBitmap = tempCroppedBitmap
                    },
                    onCropCaptureFail = {
                        initSettings()
                    }
                )
            } else if (viewModel.textFromOcr == null) {
                // 크롭 완료 후 이미지 화면 표시
                ShowImageScreen(
                    bitmap = viewModel.croppedBitmap!!,
                    onRetake = {
                        initSettings()
                    },
                    onConfirm = {
                        Log.d("ImageCaptureScreenViewModel", "HereHereHere")
                        viewModel.croppedBitmap?.let {
                            Log.d("ImageCaptureScreenViewModel", "HereHereHere - inside")
                            viewModel.ocrFromImage(
                                croppedBitmap = it,
                                result = { res ->
                                    when (viewModel.currentMode) {
                                        CurrentMode.SENTENCE -> viewModel.sentence = res
                                        CurrentMode.BOOK -> viewModel.book = res
                                        CurrentMode.AUTHOR -> viewModel.author = res
                                        else -> viewModel.page = res
                                    }
                                }
                            )
                            viewModel.goToChoosePictureSource = false
                            initSettings()
                        }
                    }
                )
            } else {
//                viewModel.textFromOcr?.let {
//                    ResultScreen(
//                        text = it,
//                        onChange = { changedTxt -> viewModel.textFromOcr = changedTxt },
//                        onBack = { initSettings() },
//                        onSaveInLocal = {},
//                        onSaveInRemote = { memorableData -> viewModel.sendDataToServer(memorableData) }
//                    )
//                }
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

@Composable
fun chooseGetPictureScreen(pictureSource: (PictureSource) -> Unit, onBack: () -> Unit) {
    BackHandler {
        onBack()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { pictureSource(PictureSource.GALLERY) },
                modifier = Modifier.size(width = 120.dp, height = 50.dp) // 고정 크기
            ) {
                Text("사진 선택")
            }

            Button(
                onClick = { pictureSource(PictureSource.CAMERA) },
                modifier = Modifier.size(width = 120.dp, height = 50.dp)
            ) {
                Text("카메라")
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
fun Camera(onPhotoCaptured: (Bitmap, Uri) -> Unit) {

    val context = LocalContext.current // context: 안드로이드 시스템 Context (카메라 Provider 실행 시 필요)
    val lifecycleOwner =
        LocalLifecycleOwner.current // lifecycleOwner: 카메라가 Activity/Fragment 라이프사이클에 맞춰 동작하도록 연결

    // 사진 촬영용 CameraX UseCase (미리보기와 함께 바인딩)
    val imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
        .setTargetRotation(context.display.rotation)
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
    onPhotoCaptured: (Bitmap, Uri) -> Unit
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
                        Bitmap.createBitmap(
                            bitmap,
                            0,
                            0,
                            bitmap.width,
                            bitmap.height,
                            matrix,
                            false
                        )
                    } else {
                        bitmap
                    }

                    // 콜백 전달
                    onPhotoCaptured(rotatedBitmap, file.toUri())
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
fun ImageCropperScreen(
    capturedBitmapUri: Uri?,
    onCropCaptureSuccess: (Bitmap) -> Unit,
    onCropCaptureFail: () -> Unit
) {
    val context = LocalContext.current
    var imageUri by remember {
        mutableStateOf<Uri?>(null)
    }
    val imageCropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            // use the cropped image
            imageUri = result.uriContent

            imageUri?.let {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                val tempBitmap = ImageDecoder.decodeBitmap(source)
                onCropCaptureSuccess(tempBitmap)
            }

            if (imageUri == null) {
                Log.d("CheckCheckCheck", "imageUri is null")
            } else {
                Log.d("CheckCheckCheck", "imageUri : $imageUri")
            }

        } else {
            val exception = result.error
            Log.d("CheckCheckCheck", "exception = result : $exception")
            onCropCaptureFail()
        }
    }

    // Composition 끝난 뒤 실행
    LaunchedEffect(capturedBitmapUri) {
        capturedBitmapUri?.let {
            val options = CropImageOptions().apply {
                // 크롭 박스 가이드라인 보이기
                guidelines = CropImageView.Guidelines.ON

                // 크롭 박스 초기 위치 비율 (0f ~ 1f)
                initialCropWindowPaddingRatio = 0.1f // 위아래 여백을 조금 두도록
            }
            val cropOptions = CropImageContractOptions(it, options)
            imageCropLauncher.launch(cropOptions)
        }
    }
}

@Composable
fun ShowImageScreen(bitmap: Bitmap, onRetake: () -> Unit, onConfirm: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Captured Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
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

        // 확인 버튼
        IconButton(
            onClick = onConfirm,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Confirm",
                tint = Color.White
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ResultScreen(
    viewModel: ImageCaptureScreenViewModel,
    goToOcr: (CurrentMode) -> Unit,
    onChange: (String) -> Unit,
    onBack: () -> Unit,
    onSaveInLocal: (BookData) -> Unit,
    onSaveInRemote: (MemorableData) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var isTitleEditing by remember { mutableStateOf(false) }
    var isAuthorEditing by remember { mutableStateOf(false) }
    var isPageEditing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // OCR 결과 영역

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(
                        width = 1.dp,
                        color = Color.Gray, // 테두리 색상
                        shape = RoundedCornerShape(1.dp) // 모서리 둥글기
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "문장",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight() // Row 높이에 맞춰서 세로선 표시
                        .padding(horizontal = 4.dp),
                    thickness = 1.dp,
                    color = Color.LightGray
                )

                if (isEditing) {
                    TextField(
                        value = viewModel.sentence,
                        onValueChange = { viewModel.sentence = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(5f),
                        singleLine = false,
                        maxLines = Int.MAX_VALUE
                    )
                } else {
                    Text(
                        text = viewModel.sentence,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(5f)
                            .heightIn(min = 56.dp)
                            .combinedClickable(
                                onClick = { /* 필요시 처리 */ },
                                onLongClick = { isEditing = true }
                            )
                    )
                }

                // 오른쪽 끝 물음표 아이콘
                IconButton(
                    onClick = { goToOcr(CurrentMode.SENTENCE) },
                    modifier = Modifier.size(24.dp) // 아이콘 크기 조절 가능
                ) {
                    Icon(
                        imageVector = Icons.Default.Add, // 기본 물음표 아이콘
                        contentDescription = "도움말",
                        tint = Color.Gray
                    )
                }
            }

            Row(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(
                        width = 1.dp,
                        color = Color.Gray, // 테두리 색상
                        shape = RoundedCornerShape(8.dp) // 모서리 둥글기
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "책 이름",
                    modifier = Modifier.weight(1f),
                    // 텍스트 자체 중앙 정렬
                    textAlign = TextAlign.Center
                )

                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight() // Row 높이에 맞춰서 세로선 표시
                        .padding(horizontal = 4.dp),
                    thickness = 1.dp,
                    color = Color.LightGray
                )

                if (isTitleEditing) {
                    TextField(
                        value = viewModel.book,
                        onValueChange = { viewModel.book = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(5f),
                        singleLine = false,
                        maxLines = Int.MAX_VALUE
                    )
                } else {
                    Text(
                        text = viewModel.book,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(5f)
                            .heightIn(min = 56.dp)
                            .combinedClickable(
                                onClick = { /* 필요시 처리 */ },
                                onLongClick = { isTitleEditing = true }
                            )
                    )
                }
                // 오른쪽 끝 물음표 아이콘
                IconButton(
                    onClick = { goToOcr(CurrentMode.BOOK) },
                    modifier = Modifier.size(24.dp) // 아이콘 크기 조절 가능
                ) {
                    Icon(
                        imageVector = Icons.Default.Add, // 기본 물음표 아이콘
                        contentDescription = "도움말",
                        tint = Color.Gray
                    )
                }
            }

            Row(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(
                        width = 1.dp,
                        color = Color.Gray, // 테두리 색상
                        shape = RoundedCornerShape(8.dp) // 모서리 둥글기
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "저자",
                    modifier = Modifier.weight(1f),
                    // 텍스트 자체 중앙 정렬
                    textAlign = TextAlign.Center
                )

                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight() // Row 높이에 맞춰서 세로선 표시
                        .padding(horizontal = 4.dp),
                    thickness = 1.dp,
                    color = Color.LightGray
                )


                if (isAuthorEditing) {
                    TextField(
                        value = viewModel.author,
                        onValueChange = { viewModel.author = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(5f),
                        singleLine = false,
                        maxLines = Int.MAX_VALUE
                    )
                } else {
                    Text(
                        text = viewModel.author,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(5f)
                            .heightIn(min = 56.dp)
                            .combinedClickable(
                                onClick = { /* 필요시 처리 */ },
                                onLongClick = { isAuthorEditing = true }
                            )
                    )
                }

                IconButton(
                    onClick = { goToOcr(CurrentMode.AUTHOR) },
                    modifier = Modifier.size(24.dp) // 아이콘 크기 조절 가능
                ) {
                    Icon(
                        imageVector = Icons.Default.Add, // 기본 물음표 아이콘
                        contentDescription = "도움말",
                        tint = Color.Gray
                    )
                }
            }
            Row(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(
                        width = 1.dp,
                        color = Color.Gray, // 테두리 색상
                        shape = RoundedCornerShape(8.dp) // 모서리 둥글기
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "페이지",
                    modifier = Modifier.weight(1f),
                    // 텍스트 자체 중앙 정렬
                    textAlign = TextAlign.Center
                )

                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight() // Row 높이에 맞춰서 세로선 표시
                        .padding(horizontal = 4.dp),
                    thickness = 1.dp,
                    color = Color.LightGray
                )

                if (isPageEditing) {
                    TextField(
                        value = viewModel.page,
                        onValueChange = { viewModel.page = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(5f),
                        singleLine = false,
                        maxLines = Int.MAX_VALUE
                    )
                } else {
                    Text(
                        text = viewModel.page,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(5f)
                            .heightIn(min = 56.dp)
                            .combinedClickable(
                                onClick = { /* 필요시 처리 */ },
                                onLongClick = { isPageEditing = true }
                            )
                    )
                }

                IconButton(
                    onClick = { goToOcr(CurrentMode.PAGE) },
                    modifier = Modifier.size(24.dp) // 아이콘 크기 조절 가능
                ) {
                    Icon(
                        imageVector = Icons.Default.Add, // 기본 물음표 아이콘
                        contentDescription = "도움말",
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    onChange(viewModel.sentence)
                    isEditing = false
                    isTitleEditing = false
                    isAuthorEditing = false
                    isPageEditing = false
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("확인")
            }
        }

        // 하단 되돌아가기 버튼
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(8.dp) // 버튼 사이 간격
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("되돌아가기")
            }

            Button(
                onClick = { onSaveInLocal(BookData(title = viewModel.book, author = viewModel.author)) },
                modifier = Modifier.weight(1f)
            ) {
                Text("로컬에 저장")
            }

            Button(
                onClick = {
                    onSaveInRemote(
                        MemorableData(
                            bookName = viewModel.book,
                            author = viewModel.author,
                            sentence = viewModel.sentence,
                            page = viewModel.page
                        )
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("서버에 저장")
            }
        }
    }
}

enum class PictureSource {
    GALLERY,
    CAMERA
}

enum class CurrentMode {
    SENTENCE,
    BOOK,
    AUTHOR,
    PAGE
}
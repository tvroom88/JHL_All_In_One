package com.aio.jhl_all_in_one.ui.visit

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aio.jhl_all_in_one.data.MemorableData
import com.aio.jhl_all_in_one.utils.FireStoreUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


@Composable
fun VisitGoodContentScreen() {
    val context = LocalContext.current
    var dataList by remember { mutableStateOf<List<MemorableData>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val fireStoreUtils = FireStoreUtils()

    // Firestore 불러오기
    LaunchedEffect(Unit) {
        fireStoreUtils.fetchMemorableData(
            onResult = { result -> dataList = result },
            onError = { e -> errorMessage = e.message }
        )
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("책에서 기억할 문장들", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(12.dp))

        if (errorMessage != null) {
            Text("에러 발생: $errorMessage", color = Color.Red)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dataList) { item ->
                    MemorableItem(
                        data = item,
                        onDelete = { target ->
                            fireStoreUtils.deleteMemorableData(target.id,
                                onSuccess = {
                                    dataList = dataList.filter { it.id != target.id } // UI 갱신
                                },
                                onError = { e ->
                                    // 에러 처리
                                    Log.e("DeleteError", "삭제 실패: ${e.message}")
                                }
                            )
                        },
                        onEdit = { target ->
                            // 수정 다이얼로그 열거나 Edit 화면 이동
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MemorableItem(
    data: MemorableData,
    onDelete: (MemorableData) -> Unit,
    onEdit: (MemorableData) -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    var maxSwipe by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.LightGray)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        // 스와이프 끝났을 때 버튼 영역 노출 혹은 원위치
                        if (offsetX.value < -maxSwipe / 2) {
                            scope.launch {
                                offsetX.animateTo(-maxSwipe) // ✅ coroutine 안에서 호출
                            }
                        } else {
                            scope.launch {
                                offsetX.animateTo(0f)
                            }
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        val newOffset = (offsetX.value + dragAmount).coerceIn(-maxSwipe, 0f)
                        scope.launch {
                            offsetX.snapTo(newOffset)
                        }
                    }
                )
            }
    ) {
        // Edit/Delete 버튼
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .onGloballyPositioned { coordinates ->
                    maxSwipe = coordinates.size.width.toFloat() // 버튼 영역 폭
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { onEdit(data) }) { Text("Edit") }
            Button(
                onClick = { onDelete(data) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) { Text("Delete") }
        }

        // 카드 내용
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) },
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("책이름: ${data.bookName}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.heightIn(max = 50.dp))
                Spacer(modifier = Modifier.height(6.dp))
                Text("내용: \"${data.sentence}\"", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.heightIn(max = 80.dp))
            }
        }
    }
}

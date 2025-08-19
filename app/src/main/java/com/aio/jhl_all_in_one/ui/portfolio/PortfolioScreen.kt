package com.aio.jhl_all_in_one.ui.portfolio

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aio.jhl_all_in_one.data.AssetData
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.sheets.v4.SheetsScopes

@Composable
fun PortfolioScreen(viewModel: PortfolioViewModel = viewModel()) {
    val context = LocalContext.current
    val sheetData by viewModel.sheetData
    val isLoading by viewModel.isLoading
    var query by remember { mutableStateOf("") }

    // GoogleSignIn 설정
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(SheetsScopes.SPREADSHEETS_READONLY))
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }


    val sheetRange = "A1:F30"

    // ActivityResultLauncher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                viewModel.fetchSheetDataWithCredential(
                    context = context,
                    account = account,
                    spreadsheetId = "16w5vNXCCqPqhDyH4SLt-ZGosKkTZhunTueZUqwhxOh4",
                    range = sheetRange
                )
            }
        } catch (e: ApiException) {
            Log.e("GoogleSignIn", "Sign in failed: ${e.statusCode}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 로그인 & 데이터 가져오기 버튼
        Button(onClick = { launcher.launch(googleSignInClient.signInIntent) }) {
            Text("구글 시트 데이터 가져오기")
        }
        Spacer(Modifier.height(8.dp))

        // 로딩 상태
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val filteredData = sheetData.filter { row ->
                row.values.any { it.contains(query, ignoreCase = true) }
            }

            if (filteredData.isEmpty()) {
                Text("데이터 없음", style = MaterialTheme.typography.bodyMedium)
            } else {
                val horizontalScrollState = rememberScrollState() // 모든 행에 공유
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredData) { row ->
                        Row(
                            modifier = Modifier
                                .horizontalScroll(horizontalScrollState) // ← 모든 행 동일 스크롤
                                .clickable {
                                    Toast.makeText(
                                        context,
                                        "선택: ${row.values.joinToString()}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            row.values.forEach { cell ->
                                Text(
                                    text = cell,
                                    modifier = Modifier
                                        .width(120.dp) // 셀 폭 고정
                                        .padding(4.dp)
                                )
                            }
                        }
                        Divider()
                    }
                }
        }
    }
}

@Composable
fun DynamicTable(assetData: List<AssetData>) {
    Column {
        // 헤더
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("이름", "나이", "직업").forEach { header ->
                Text(
                    text = header,
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                )
            }
        }

        HorizontalDivider(thickness = 0.3.dp, color = Color.Gray)

        // 데이터
        assetData.forEach { person ->
            Row(modifier = Modifier.fillMaxWidth()) {
                VerticalDivider(thickness = 0.5.dp, color = Color.LightGray)
                Text(
                    person.subtitle, modifier = Modifier
                        .weight(1f)
                        .padding(2.dp)
                )
                VerticalDivider(thickness = 0.5.dp, color = Color.LightGray)
                Text(
                    person.title, modifier = Modifier
                        .weight(1f)
                        .padding(2.dp)
                )
                VerticalDivider(thickness = 0.5.dp, color = Color.LightGray)
                Text(
                    person.price.toString(), modifier = Modifier
                        .weight(1f)
                        .padding(2.dp)
                )
                VerticalDivider(thickness = 0.5.dp, color = Color.LightGray)
            }
            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
        }
    }
}
//    @Composable
//    fun TableCell(text: String) {
//        Text(
//            text = text,
//            modifier = Modifier
//                .weight(1f)
//                .padding(2.dp)
//        )
//
//
//    }

    @Composable
    fun DividerVertical() {
        Box(
            modifier = Modifier
                .width(0.5.dp)
                .fillMaxHeight() // 부모 Row의 높이에 맞추기
                .background(Color.LightGray)
        )
    }
}
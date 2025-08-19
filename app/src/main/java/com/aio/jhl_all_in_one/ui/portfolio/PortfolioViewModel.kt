package com.aio.jhl_all_in_one.ui.portfolio

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


data class SheetRow(val values: List<String>)

class PortfolioViewModel : ViewModel() {
    val sheetData = mutableStateOf<List<SheetRow>>(emptyList())
    val isLoading = mutableStateOf(false)

    fun fetchSheetDataWithCredential(
        context: Context,
        account: GoogleSignInAccount,
        spreadsheetId: String,
        range: String
    ) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                // 1. Credential 생성
                val credential = GoogleAccountCredential.usingOAuth2(
                    context,
                    listOf(SheetsScopes.SPREADSHEETS_READONLY)
                )
                credential.selectedAccount = account.account

                // 2. Sheets API 서비스 생성
                val service = Sheets.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
                )
                    .setApplicationName("MyApp")
                    .build()

                // 3. API 호출 (Coroutine + IO)
                val response = withContext(Dispatchers.IO) {
                    service.spreadsheets().values()
                        .get(spreadsheetId, range)
                        .execute()
                }

                // 4. 결과 처리
                val values = response.getValues() ?: emptyList<List<Any>>()
                sheetData.value = values.map { SheetRow(it.map { cell -> cell.toString() }) }

            } catch (e: Exception) {
                Log.e("SheetsAPI", "Error fetching data", e)
            } finally {
                isLoading.value = false
            }
        }
    }
}
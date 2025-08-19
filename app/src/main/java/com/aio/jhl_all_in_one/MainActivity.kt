package com.aio.jhl_all_in_one

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aio.jhl_all_in_one.ui.main.MainScreen
import com.aio.jhl_all_in_one.ui.theme.JHL_All_In_OneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JHL_All_In_OneTheme {
                MainScreen()
            }
        }
    }
}




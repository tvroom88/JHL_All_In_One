package com.aio.jhl_all_in_one.ui.main

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aio.jhl_all_in_one.data.AssetData
import com.aio.jhl_all_in_one.ui.imagecapture.ImageCaptureScreen
import com.aio.jhl_all_in_one.ui.portfolio.PortfolioScreen

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val items = listOf("Portfolio", "Search", "Profile")
    val tempAssetData = listOf(
        AssetData("1", "1", 1),
        AssetData("2", "2", 2),
        AssetData("3", "3", 3)
    )

    Scaffold(
        topBar = { JhlTopAppBarScreen() },
        bottomBar = {
            JhlBottomNavigation(
                items = items,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> PortfolioScreen()
                1 -> ImageCaptureScreen()
                2 -> Text("Profile Screen")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JhlTopAppBarScreen() {
    Column {
        TopAppBar(
            title = { Text("All In One App") },
            modifier = Modifier.height(65.dp) // 높이를 조절 가능
        )
        HorizontalDivider(
            thickness = 0.3.dp,      // 두께
            color = Color.Gray  // 밑줄 색
        )
    }
}

@Composable
fun JhlBottomNavigation(
    items: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = { Icon(Icons.Default.Home, contentDescription = item) }, // 아이콘은 상황에 맞게 교체
                label = { Text(item) }
            )
        }
    }
}

@Preview
@Composable
fun showMainView() {
    MainScreen()
}
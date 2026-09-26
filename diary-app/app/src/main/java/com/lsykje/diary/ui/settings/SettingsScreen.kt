package com.lsykje.diary.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.lsykje.diary.ui.nav.Routes
import com.lsykje.diary.ui.theme.DiaryColors
import com.lsykje.diary.ui.theme.DiaryType

/** 설계 문서 5-8절: 설정 화면 */
@Composable
fun SettingsScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize().background(DiaryColors.Surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 56.dp, start = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로", tint = DiaryColors.InkSoft)
            }
            Text("설정", style = DiaryType.Title, color = DiaryColors.Ink)
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SettingsRow("잠금 방식 관리", "지문·PIN·패턴") {
                navController.navigate(Routes.lockSetup(firstRun = false))
            }
            SettingsRow("PDF 내보내기", "하루 단위 / 기간 단위") {
                navController.navigate(Routes.PdfExport)
            }
            SettingsRow("PDF 불러오기", "추후 업데이트 예정") {
                navController.navigate(Routes.comingSoon("PDF 불러오기"))
            }
            SettingsRow("백업 / 복원", "추후 업데이트 예정") {
                navController.navigate(Routes.comingSoon("백업 / 복원"))
            }
            SettingsRow("앱 정보", "나의 일기장 v0.1.0") {
                navController.navigate(Routes.comingSoon("앱 정보"))
            }
        }
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DiaryColors.Card, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(title, style = DiaryType.BodyStrong, color = DiaryColors.Ink)
            Text(subtitle, style = DiaryType.CaptionRegular, color = DiaryColors.InkSoft)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = DiaryColors.InkSoft)
    }
}

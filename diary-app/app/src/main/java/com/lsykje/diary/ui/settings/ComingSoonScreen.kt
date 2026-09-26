package com.lsykje.diary.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.lsykje.diary.ui.theme.DiaryColors
import com.lsykje.diary.ui.theme.DiaryType
import com.lsykje.diary.ui.theme.GhostButton

/**
 * 설계 문서 로드맵 3~5단계(PDF 내보내기/불러오기, 백업/복원)는 1차 범위 밖이라 아직 구현 전.
 * 자리만 잡아두고, 실제 기능은 다음 단계에서 채워 넣는다.
 */
@Composable
fun ComingSoonScreen(navController: NavController, title: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiaryColors.Surface)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = DiaryType.Title, color = DiaryColors.Ink)
        Text(
            "이 기능은 다음 단계 업데이트에서 제공될 예정이에요",
            style = DiaryType.CaptionRegular,
            color = DiaryColors.InkSoft,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        GhostButton(text = "돌아가기", onClick = { navController.popBackStack() })
    }
}

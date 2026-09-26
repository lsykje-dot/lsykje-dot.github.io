package com.lsykje.diary.ui.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.lsykje.diary.data.DiaryRepository
import com.lsykje.diary.ui.nav.Routes
import com.lsykje.diary.ui.theme.DiaryColors
import com.lsykje.diary.ui.theme.DiaryType
import com.lsykje.diary.ui.theme.GhostButton
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** 설계 문서 5-4절: 일기 상세 보기 화면 */
@Composable
fun DiaryDetailScreen(navController: NavController, date: LocalDate) {
    val context = LocalContext.current
    val repository = remember { DiaryRepository(context) }
    val scope = rememberCoroutineScope()

    val entry by remember(date) { repository.observeByDate(date) }.collectAsState(initial = null)

    Column(modifier = Modifier.fillMaxSize().background(DiaryColors.Surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 56.dp, start = 8.dp, end = 20.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로", tint = DiaryColors.InkSoft)
            }
            val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)
            Text("${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일 $weekday", style = DiaryType.Secondary, color = DiaryColors.Ink)
        }

        val current = entry
        if (current == null) {
            Text(
                "이 날짜의 일기가 없어요",
                style = DiaryType.Body,
                color = DiaryColors.InkSoft,
                modifier = Modifier.padding(20.dp),
            )
            return@Column
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(current.entry.title, style = DiaryType.Title, color = DiaryColors.Ink)

            if (current.photos.isNotEmpty()) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f, fill = false)) {
                    items(current.photos) { photo ->
                        AsyncImage(
                            model = File(photo.filePath),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DiaryColors.PrimaryPastel),
                        )
                    }
                }
            }

            Text(current.entry.content, style = DiaryType.Body, color = DiaryColors.Ink)

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                GhostButton(text = "수정", onClick = { navController.navigate(Routes.diaryEdit(date)) })
                GhostButton(text = "PDF로 내보내기", onClick = { navController.navigate(Routes.comingSoon("PDF 내보내기")) })
                GhostButton(
                    text = "삭제",
                    onClick = {
                        scope.launch {
                            repository.deleteEntry(current)
                            navController.popBackStack()
                        }
                    },
                )
            }
        }
    }
}

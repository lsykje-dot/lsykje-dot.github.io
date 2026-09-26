package com.lsykje.diary.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.lsykje.diary.data.DiaryRepository
import com.lsykje.diary.data.EntryWithPhotos
import com.lsykje.diary.ui.nav.Routes
import com.lsykje.diary.ui.theme.DiaryColors
import com.lsykje.diary.ui.theme.DiaryType
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { DiaryRepository(context) }
    val today = remember { LocalDate.now() }

    var currentMonth by remember { mutableStateOf(YearMonth.from(today)) }

    val datesWithEntries by remember(currentMonth) { repository.observeDatesInMonth(currentMonth) }
        .collectAsState(initial = emptyList())

    val recentEntries by remember { repository.observeRecent(5) }
        .collectAsState(initial = emptyList())

    Box(modifier = Modifier.fillMaxSize().background(DiaryColors.Surface)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 56.dp, start = 20.dp, end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("나의 일기장", style = DiaryType.Title, color = DiaryColors.Ink)
                Row {
                    IconButton(onClick = { navController.navigate(Routes.comingSoon("검색")) }) {
                        Icon(Icons.Filled.Search, contentDescription = "검색", tint = DiaryColors.InkSoft)
                    }
                    IconButton(onClick = { navController.navigate(Routes.Settings) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "설정", tint = DiaryColors.InkSoft)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "이전 달", tint = DiaryColors.InkSoft)
                }
                Text(
                    "${currentMonth.year}년 ${currentMonth.monthValue}월",
                    style = DiaryType.Section,
                    color = DiaryColors.Ink,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "다음 달", tint = DiaryColors.InkSoft)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DiaryColors.Card, RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("일", "월", "화", "수", "목", "금", "토").forEach { label ->
                        Text(label, style = DiaryType.Caption, color = DiaryColors.InkSoft, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }

                val leadingBlanks = currentMonth.atDay(1).dayOfWeek.value % 7
                val totalDays = currentMonth.lengthOfMonth()
                val cells: List<LocalDate?> = List(leadingBlanks) { null } +
                    (1..totalDays).map { currentMonth.atDay(it) }

                val rowCount = (cells.size + 6) / 7
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.height((rowCount * 44).dp),
                ) {
                    items(cells) { date ->
                        DayCell(
                            date = date,
                            isToday = date == today,
                            hasEntry = date != null && date in datesWithEntries,
                            onClick = {
                                if (date != null) {
                                    if (date in datesWithEntries) {
                                        navController.navigate(Routes.diaryDetail(date))
                                    } else {
                                        navController.navigate(Routes.diaryEdit(date))
                                    }
                                }
                            },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("최근 일기", style = DiaryType.Section, color = DiaryColors.Ink)
                recentEntries.forEach { item ->
                    RecentEntryRow(item) { navController.navigate(Routes.diaryDetail(item.entry.date)) }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        IconButton(
            onClick = { navController.navigate(Routes.diaryEdit(today)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 32.dp)
                .size(56.dp)
                .background(DiaryColors.PrimaryStrong, CircleShape),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "새 일기 쓰기", tint = DiaryColors.OnPrimary)
        }
    }
}

@Composable
private fun DayCell(date: LocalDate?, isToday: Boolean, hasEntry: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(2.dp)
            .size(44.dp)
            .clip(CircleShape)
            .background(if (isToday) DiaryColors.PrimaryStrong else DiaryColors.Card)
            .then(if (date != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (date != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${date.dayOfMonth}",
                    style = DiaryType.Secondary,
                    color = if (isToday) DiaryColors.OnPrimary else DiaryColors.Ink,
                )
                if (hasEntry) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(4.dp)
                            .background(if (isToday) DiaryColors.OnPrimary else DiaryColors.PrimaryStrong, CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentEntryRow(item: EntryWithPhotos, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DiaryColors.Card, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val thumbnail = item.photos.firstOrNull()
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DiaryColors.PrimaryPastel),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbnail != null) {
                AsyncImage(
                    model = File(thumbnail.filePath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    item.entry.title.take(1).ifBlank { "일" },
                    style = DiaryType.Section,
                    color = DiaryColors.PrimaryStrong,
                )
            }
        }
        Column {
            val weekday = item.entry.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)
            Text(
                "${item.entry.date.monthValue}월 ${item.entry.date.dayOfMonth}일 $weekday · ${item.entry.title}",
                style = DiaryType.CaptionRegular,
                color = DiaryColors.InkSoft,
            )
            Text(
                item.entry.content.lineSequence().firstOrNull().orEmpty(),
                style = DiaryType.BodyStrong,
                color = DiaryColors.Ink,
                maxLines = 1,
            )
        }
    }
}

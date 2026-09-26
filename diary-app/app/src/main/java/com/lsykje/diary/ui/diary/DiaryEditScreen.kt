package com.lsykje.diary.ui.diary

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.lsykje.diary.data.DiaryPhoto
import com.lsykje.diary.data.DiaryRepository
import com.lsykje.diary.ui.theme.Chip
import com.lsykje.diary.ui.theme.DiaryColors
import com.lsykje.diary.ui.theme.DiaryType
import com.lsykje.diary.ui.theme.GhostButton
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** 설계 문서 5-3절: 일기 작성/수정 화면 */
@Composable
fun DiaryEditScreen(navController: NavController, date: LocalDate) {
    val context = LocalContext.current
    val repository = remember { DiaryRepository(context) }
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var existingPhotos by remember { mutableStateOf(listOf<DiaryPhoto>()) }
    var removedIds by remember { mutableStateOf(setOf<Long>()) }
    var newUris by remember { mutableStateOf(listOf<Uri>()) }

    LaunchedEffect(date) {
        val existing = repository.observeByDate(date).firstOrNull()
        if (existing != null) {
            title = existing.entry.title
            content = existing.entry.content
            existingPhotos = existing.photos
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris -> newUris = newUris + uris }

    val keptPhotos = existingPhotos.filterNot { it.id in removedIds }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiaryColors.Surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 56.dp, start = 8.dp, end = 20.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로", tint = DiaryColors.InkSoft)
            }
            val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)
            Text("${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일 $weekday", style = DiaryType.Secondary, color = DiaryColors.Ink)
            Chip(
                text = "저장",
                background = DiaryColors.PrimaryStrong,
                contentColor = DiaryColors.OnPrimary,
                modifier = Modifier.clickable {
                    scope.launch {
                        repository.saveEntry(date, title, content, keptPhotos, newUris)
                        navController.popBackStack()
                    }
                },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("제목", style = DiaryType.Caption, color = DiaryColors.InkSoft)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("제목을 입력하세요") },
                    singleLine = true,
                    textStyle = DiaryType.Title,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DiaryColors.PrimaryStrong,
                        unfocusedBorderColor = DiaryColors.Border,
                    ),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                GhostButton(
                    text = "사진 추가",
                    onClick = { photoPicker.launch(androidx.activity.result.PickVisualMediaRequest()) },
                    leadingIcon = { Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = DiaryColors.Ink, modifier = Modifier.size(18.dp)) },
                )
                VoiceInputButton(
                    onTextRecognized = { text ->
                        content = if (content.isBlank()) text else "$content\n$text"
                    },
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(DiaryColors.Card, RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (keptPhotos.isNotEmpty() || newUris.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(keptPhotos) { photo ->
                            PhotoThumb(model = File(photo.filePath)) {
                                removedIds = removedIds + photo.id
                            }
                        }
                        items(newUris) { uri ->
                            PhotoThumb(model = uri) {
                                newUris = newUris - uri
                            }
                        }
                    }
                }

                Text("본문", style = DiaryType.Caption, color = DiaryColors.InkSoft)
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("오늘 하루를 자유롭게 적어보세요") },
                    textStyle = DiaryType.Body,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DiaryColors.Card,
                        unfocusedBorderColor = DiaryColors.Card,
                    ),
                )
            }
        }
    }
}

@Composable
private fun PhotoThumb(model: Any, onRemove: () -> Unit) {
    Box(modifier = Modifier.size(96.dp)) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DiaryColors.PrimaryPastel),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(22.dp)
                .background(DiaryColors.Ink.copy(alpha = 0.55f), CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, contentDescription = "사진 삭제", tint = DiaryColors.Card, modifier = Modifier.size(14.dp))
        }
    }
}

package com.lsykje.diary.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.lsykje.diary.data.DiaryRepository
import com.lsykje.diary.pdf.PdfDiaryParser
import com.lsykje.diary.pdf.PdfTextExtractor
import com.lsykje.diary.ui.theme.Chip
import com.lsykje.diary.ui.theme.DiaryColors
import com.lsykje.diary.ui.theme.DiaryType
import com.lsykje.diary.ui.theme.GhostButton
import com.lsykje.diary.ui.theme.PrimaryButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private data class ReviewItem(
    val id: Int,
    val date: LocalDate?,
    val title: String,
    val body: String,
    val selected: Boolean = true,
    val hasExisting: Boolean = false,
)

/**
 * 설계 문서 5-6 / 7-4절: PDF를 불러와 글자를 추출하고, 날짜 줄 규칙으로 자동 인식되는 부분은
 * 미리 채워서, 인식되지 않은 부분(date == null)은 사용자가 날짜·제목·본문을 직접 정리한 뒤
 * 가져오도록 한다. 우리 앱 PDF·"나의일상" PDF는 규칙이 맞아떨어져 거의 그대로 가져와지고,
 * 그 외 PDF는 이 화면에서 사람이 나눠주는 몫이 커진다.
 */
@Composable
fun PdfImportScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { DiaryRepository(context) }
    val scope = rememberCoroutineScope()
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy년 M월 d일") }

    var items by remember { mutableStateOf(listOf<ReviewItem>()) }
    var isLoading by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var datePickerForId by remember { mutableStateOf<Int?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isLoading = true
            message = null
            items = emptyList()
            runCatching {
                val text = withContext(Dispatchers.IO) { PdfTextExtractor.extractText(context, uri) }
                val blocks = PdfDiaryParser.parse(text)
                blocks.mapIndexed { index, block ->
                    val existing = block.date?.let { repository.observeByDate(it).first() }
                    ReviewItem(
                        id = index,
                        date = block.date,
                        title = block.title,
                        body = block.body,
                        hasExisting = existing != null,
                    )
                }
            }.onSuccess { parsed ->
                items = parsed
                if (parsed.isEmpty()) message = "이 PDF에서 내용을 찾지 못했어요"
            }.onFailure {
                message = "PDF를 읽는 중 문제가 발생했어요"
            }
            isLoading = false
        }
    }

    fun updateItem(id: Int, transform: (ReviewItem) -> ReviewItem) {
        items = items.map { if (it.id == id) transform(it) else it }
    }

    Column(modifier = Modifier.fillMaxSize().background(DiaryColors.Surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 56.dp, start = 8.dp, end = 20.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로", tint = DiaryColors.InkSoft)
                }
                Text("PDF 불러오기", style = DiaryType.Title, color = DiaryColors.Ink)
            }
            GhostButton(text = "파일 선택", onClick = { filePicker.launch(arrayOf("application/pdf")) })
        }

        if (isLoading) {
            Text(
                "PDF를 읽는 중이에요…",
                style = DiaryType.CaptionRegular,
                color = DiaryColors.InkSoft,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        message?.let {
            Text(it, style = DiaryType.CaptionRegular, color = DiaryColors.InkSoft, modifier = Modifier.padding(horizontal = 20.dp))
        }

        if (items.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    ReviewItemCard(
                        item = item,
                        formatter = formatter,
                        onToggleSelected = { updateItem(item.id) { it.copy(selected = !it.selected) } },
                        onTitleChange = { newTitle -> updateItem(item.id) { it.copy(title = newTitle) } },
                        onBodyChange = { newBody -> updateItem(item.id) { it.copy(body = newBody) } },
                        onPickDate = { datePickerForId = item.id },
                    )
                }
            }

            val selectedCount = items.count { it.selected }
            val readyCount = items.count { it.selected && it.date != null }
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selectedCount != readyCount) {
                    Text(
                        "날짜가 지정되지 않은 항목은 가져올 수 없어요 — 위에서 날짜를 먼저 선택해주세요",
                        style = DiaryType.CaptionRegular,
                        color = DiaryColors.InkSoft,
                    )
                }
                PrimaryButton(
                    text = "선택한 항목 가져오기 ($readyCount)",
                    enabled = !isImporting && readyCount > 0,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            isImporting = true
                            val toImport = items.filter { it.selected && it.date != null }
                            toImport.forEach { item ->
                                repository.saveEntry(
                                    date = item.date!!,
                                    title = item.title,
                                    content = item.body,
                                    keepPhotos = emptyList(),
                                    newPhotoUris = emptyList(),
                                )
                            }
                            message = "${toImport.size}개 일기를 가져왔어요"
                            items = emptyList()
                            isImporting = false
                        }
                    },
                )
            }
        }
    }

    datePickerForId?.let { id ->
        val target = items.firstOrNull { it.id == id }
        SimpleDatePickerDialog(
            initial = target?.date ?: LocalDate.now(),
            onDismiss = { datePickerForId = null },
            onConfirm = { date ->
                updateItem(id) { it.copy(date = date) }
                datePickerForId = null
            },
        )
    }
}

@Composable
private fun ReviewItemCard(
    item: ReviewItem,
    formatter: DateTimeFormatter,
    onToggleSelected: () -> Unit,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onPickDate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DiaryColors.Card, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onPickDate),
            ) {
                if (item.date != null) {
                    Text(item.date.format(formatter), style = DiaryType.BodyStrong, color = DiaryColors.Ink)
                } else {
                    Chip(text = "날짜 선택 필요", background = DiaryColors.Surface, contentColor = DiaryColors.InkSoft)
                }
            }
            Checkbox(
                checked = item.selected,
                onCheckedChange = { onToggleSelected() },
                colors = CheckboxDefaults.colors(checkedColor = DiaryColors.PrimaryStrong),
            )
        }
        if (item.hasExisting) {
            Text("이미 이 날짜에 일기가 있어요 — 가져오면 덮어써요", style = DiaryType.CaptionRegular, color = DiaryColors.InkSoft)
        }

        OutlinedTextField(
            value = item.title,
            onValueChange = onTitleChange,
            placeholder = { Text("제목") },
            singleLine = true,
            textStyle = DiaryType.BodyStrong,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DiaryColors.PrimaryStrong,
                unfocusedBorderColor = DiaryColors.Border,
            ),
        )
        OutlinedTextField(
            value = item.body,
            onValueChange = onBodyChange,
            placeholder = { Text("본문") },
            textStyle = DiaryType.Body,
            modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DiaryColors.PrimaryStrong,
                unfocusedBorderColor = DiaryColors.Border,
            ),
        )
    }
}

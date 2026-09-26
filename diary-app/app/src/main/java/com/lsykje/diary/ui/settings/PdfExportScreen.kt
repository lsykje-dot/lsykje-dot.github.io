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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.lsykje.diary.pdf.PdfExporter
import com.lsykje.diary.pdf.PdfSharer
import com.lsykje.diary.ui.theme.DiaryColors
import com.lsykje.diary.ui.theme.DiaryType
import com.lsykje.diary.ui.theme.PrimaryButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 설계 문서 5-5절: 하루 단위 / 기간 단위 PDF 내보내기 화면 */
@Composable
fun PdfExportScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { DiaryRepository(context) }
    val scope = rememberCoroutineScope()
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy년 M월 d일") }

    var singleDate by remember { mutableStateOf(LocalDate.now()) }
    var rangeStart by remember { mutableStateOf(LocalDate.now().minusDays(6)) }
    var rangeEnd by remember { mutableStateOf(LocalDate.now()) }
    var isExporting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    var pickerTarget by remember { mutableStateOf<PickerTarget?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(DiaryColors.Surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 56.dp, start = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로", tint = DiaryColors.InkSoft)
            }
            Text("PDF 내보내기", style = DiaryType.Title, color = DiaryColors.Ink)
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ExportSection(title = "하루 일기 내보내기") {
                DateRow(label = singleDate.format(formatter)) { pickerTarget = PickerTarget.Single }
                PrimaryButton(
                    text = "이 날짜 내보내기",
                    enabled = !isExporting,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            isExporting = true
                            val entry = repository.observeByDate(singleDate).first()
                            if (entry == null) {
                                message = "이 날짜에는 일기가 없어요"
                            } else {
                                val file = withContext(Dispatchers.IO) { PdfExporter.exportDay(context, entry) }
                                PdfSharer.share(context, file)
                                message = null
                            }
                            isExporting = false
                        }
                    },
                )
            }

            ExportSection(title = "기간 선택해서 내보내기") {
                DateRow(label = "시작일 · ${rangeStart.format(formatter)}") { pickerTarget = PickerTarget.RangeStart }
                DateRow(label = "종료일 · ${rangeEnd.format(formatter)}") { pickerTarget = PickerTarget.RangeEnd }
                PrimaryButton(
                    text = "이 기간 내보내기",
                    enabled = !isExporting && !rangeStart.isAfter(rangeEnd),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            isExporting = true
                            val entries = repository.observeEntriesInRange(rangeStart, rangeEnd).first()
                            if (entries.isEmpty()) {
                                message = "이 기간에는 일기가 없어요"
                            } else {
                                val file = withContext(Dispatchers.IO) { PdfExporter.exportRange(context, entries) }
                                PdfSharer.share(context, file)
                                message = null
                            }
                            isExporting = false
                        }
                    },
                )
                if (rangeStart.isAfter(rangeEnd)) {
                    Text("시작일은 종료일보다 늦을 수 없어요", style = DiaryType.CaptionRegular, color = DiaryColors.InkSoft)
                }
            }

            message?.let { Text(it, style = DiaryType.CaptionRegular, color = DiaryColors.InkSoft) }
        }
    }

    pickerTarget?.let { target ->
        val initial = when (target) {
            PickerTarget.Single -> singleDate
            PickerTarget.RangeStart -> rangeStart
            PickerTarget.RangeEnd -> rangeEnd
        }
        SimpleDatePickerDialog(
            initial = initial,
            onDismiss = { pickerTarget = null },
            onConfirm = { date ->
                when (target) {
                    PickerTarget.Single -> singleDate = date
                    PickerTarget.RangeStart -> rangeStart = date
                    PickerTarget.RangeEnd -> rangeEnd = date
                }
                pickerTarget = null
            },
        )
    }
}

private enum class PickerTarget { Single, RangeStart, RangeEnd }

@Composable
private fun ExportSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DiaryColors.Card, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = DiaryType.Section, color = DiaryColors.Ink)
        content()
    }
}

@Composable
private fun DateRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DiaryColors.PrimaryPastel, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = DiaryType.BodyStrong, color = DiaryColors.Ink)
        Text("변경", style = DiaryType.Secondary, color = DiaryColors.PrimaryStrong)
    }
}

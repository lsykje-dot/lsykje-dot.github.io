package com.lsykje.diary.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.lsykje.diary.backup.BackupManager
import com.lsykje.diary.backup.BackupSharer
import com.lsykje.diary.ui.nav.Routes
import com.lsykje.diary.ui.theme.DiaryColors
import com.lsykje.diary.ui.theme.DiaryType
import com.lsykje.diary.ui.theme.GhostButton
import com.lsykje.diary.ui.theme.PrimaryButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 설계 문서 7-5절: 백업(zip) 내보내기 / 불러오기 화면 */
@Composable
fun BackupScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isWorking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            message = null
            pendingRestoreUri = uri
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(DiaryColors.Surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 56.dp, start = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로", tint = DiaryColors.InkSoft)
            }
            Text("백업 / 복원", style = DiaryType.Title, color = DiaryColors.Ink)
        }

        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            BackupSection(title = "백업 내보내기") {
                Text(
                    "일기 전체(글 + 사진)를 zip 파일 하나로 만들어요. USB, SD카드, 개인 클라우드 등 원하는 곳에 옮겨두면 기기를 바꿔도 복원할 수 있어요.",
                    style = DiaryType.CaptionRegular,
                    color = DiaryColors.InkSoft,
                )
                PrimaryButton(
                    text = "백업 파일 만들기",
                    enabled = !isWorking,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            isWorking = true
                            runCatching { withContext(Dispatchers.IO) { BackupManager.exportBackup(context) } }
                                .onSuccess { file -> BackupSharer.share(context, file) }
                                .onFailure { message = "백업 파일을 만드는 중 문제가 발생했어요" }
                            isWorking = false
                        }
                    },
                )
            }

            BackupSection(title = "백업 불러오기") {
                Text(
                    "이전에 만든 백업 zip 파일을 선택하면, 지금 데이터를 통째로 바꿀지(덮어쓰기) 없는 날짜만 채워 넣을지(합치기) 고를 수 있어요.",
                    style = DiaryType.CaptionRegular,
                    color = DiaryColors.InkSoft,
                )
                GhostButton(
                    text = "백업 파일 선택",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { filePicker.launch(arrayOf("application/zip", "application/octet-stream")) },
                )
            }

            if (isWorking) {
                Text("처리 중이에요…", style = DiaryType.CaptionRegular, color = DiaryColors.InkSoft)
            }
            message?.let { Text(it, style = DiaryType.CaptionRegular, color = DiaryColors.InkSoft) }
        }
    }

    val uri = pendingRestoreUri
    if (uri != null) {
        RestoreChoiceDialog(
            onDismiss = { pendingRestoreUri = null },
            onOverwrite = {
                pendingRestoreUri = null
                scope.launch {
                    isWorking = true
                    runCatching { withContext(Dispatchers.IO) { BackupManager.restoreOverwrite(context, uri) } }
                        .onSuccess {
                            // 기존 화면들이 이미 닫힌(교체된) DB 연결을 들고 있을 수 있으므로,
                            // 잠금 화면으로 완전히 다시 시작한다
                            navController.navigate(Routes.Lock) { popUpTo(0) }
                        }
                        .onFailure { message = it.message ?: "복원 중 문제가 발생했어요" }
                    isWorking = false
                }
            },
            onMerge = {
                pendingRestoreUri = null
                scope.launch {
                    isWorking = true
                    runCatching { withContext(Dispatchers.IO) { BackupManager.restoreMerge(context, uri) } }
                        .onSuccess { result ->
                            message = "${result.importedCount}개 가져왔어요" +
                                if (result.skippedCount > 0) " (이미 있는 ${result.skippedCount}개는 건너뜀)" else ""
                        }
                        .onFailure { message = it.message ?: "복원 중 문제가 발생했어요" }
                    isWorking = false
                }
            },
        )
    }
}

@Composable
private fun BackupSection(title: String, content: @Composable () -> Unit) {
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
private fun RestoreChoiceDialog(onDismiss: () -> Unit, onOverwrite: () -> Unit, onMerge: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("어떻게 복원할까요?", style = DiaryType.Title, color = DiaryColors.Ink) },
        text = {
            Text(
                "덮어쓰기: 지금 있는 일기를 모두 지우고 백업 내용으로 바꿔요.\n" +
                    "합치기: 지금 일기는 그대로 두고, 없는 날짜만 백업에서 채워 넣어요.",
                style = DiaryType.CaptionRegular,
                color = DiaryColors.InkSoft,
            )
        },
        confirmButton = {
            TextButton(onClick = onOverwrite) { Text("덮어쓰기", color = DiaryColors.PrimaryStrong) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onMerge) { Text("합치기", color = DiaryColors.PrimaryStrong) }
                TextButton(onClick = onDismiss) { Text("취소", color = DiaryColors.InkSoft) }
            }
        },
    )
}

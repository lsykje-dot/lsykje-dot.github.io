package com.lsykje.diary.ui.diary

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.lsykje.diary.ui.theme.DiaryColors
import com.lsykje.diary.ui.theme.DiaryType
import com.lsykje.diary.voice.VoiceInputRecognizer

/**
 * 설계 원칙: 온디바이스 음성 인식을 지원하는 기기에서만 마이크 버튼이 나타난다.
 * 지원하지 않는 기기에서는 이 컴포저블이 아무것도 그리지 않는다(기능이 통째로 꺼진 상태).
 */
@Composable
fun VoiceInputButton(onTextRecognized: (String) -> Unit) {
    val context = LocalContext.current
    val supported = remember { VoiceInputRecognizer.isSupported(context) }
    if (!supported) return

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var isListening by remember { mutableStateOf(false) }
    var partialText by remember { mutableStateOf("") }

    val recognizer = remember {
        VoiceInputRecognizer.create(
            context = context,
            onPartialResult = { partialText = it },
            onFinalResult = { text ->
                partialText = ""
                onTextRecognized(text)
            },
            onListeningChanged = { isListening = it },
            onErrorText = { partialText = "" },
        )
    }

    DisposableEffect(Unit) {
        onDispose { recognizer?.destroy() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        if (granted) recognizer?.startListening(VoiceInputRecognizer.buildIntent())
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isListening) DiaryColors.PrimaryStrong else DiaryColors.PrimaryPastel,
                    CircleShape,
                )
                .clickable {
                    when {
                        !hasPermission -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        isListening -> recognizer?.stopListening()
                        else -> recognizer?.startListening(VoiceInputRecognizer.buildIntent())
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription = if (isListening) "음성 입력 중지" else "음성으로 일기 쓰기",
                tint = if (isListening) DiaryColors.OnPrimary else DiaryColors.PrimaryStrong,
            )
        }
        if (isListening) {
            Text(
                partialText.ifBlank { "듣고 있어요…" },
                style = DiaryType.CaptionRegular,
                color = DiaryColors.InkSoft,
            )
        }
    }
}

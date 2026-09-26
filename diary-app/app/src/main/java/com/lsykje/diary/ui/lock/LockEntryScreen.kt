package com.lsykje.diary.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.lsykje.diary.auth.LockPrefs
import com.lsykje.diary.ui.nav.Routes
import com.lsykje.diary.ui.theme.DiaryColors
import com.lsykje.diary.ui.theme.DiaryType

/** 설계 문서 5-1절: PIN 또는 패턴으로 잠금 해제(생체인증이 없거나 실패했을 때의 대체 수단) */
@Composable
fun LockEntryScreen(navController: NavController) {
    val context = LocalContext.current
    val lockPrefs = remember { LockPrefs(context) }

    var mode by remember { mutableStateOf(if (lockPrefs.hasPin) "pin" else "pattern") }
    var error by remember { mutableStateOf(false) }

    fun unlock() {
        navController.navigate(Routes.Calendar) {
            popUpTo(Routes.Lock) { inclusive = true }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiaryColors.Surface)
            .padding(horizontal = 32.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            if (mode == "pin") "PIN을 입력해주세요" else "패턴을 그려주세요",
            style = DiaryType.Title,
            color = DiaryColors.Ink,
        )
        if (error) {
            Text(
                "일치하지 않습니다. 다시 시도해주세요",
                style = DiaryType.CaptionRegular,
                color = DiaryColors.InkSoft,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        if (mode == "pin") {
            PinPad(
                onSubmit = { pin ->
                    if (lockPrefs.verifyPin(pin)) unlock() else error = true
                },
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            PatternPad(
                onComplete = { cells ->
                    if (lockPrefs.verifyPattern(cells)) unlock() else error = true
                },
                modifier = Modifier.padding(top = 24.dp),
            )
        }

        if (lockPrefs.hasPin && lockPrefs.hasPattern) {
            Text(
                if (mode == "pin") "패턴으로 전환" else "PIN으로 전환",
                style = DiaryType.Secondary,
                color = DiaryColors.InkSoft,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .clickable {
                        error = false
                        mode = if (mode == "pin") "pattern" else "pin"
                    },
            )
        }
    }
}

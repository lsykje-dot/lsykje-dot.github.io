package com.lsykje.diary.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.lsykje.diary.auth.BiometricAuthenticator
import com.lsykje.diary.auth.LockPrefs
import com.lsykje.diary.ui.nav.Routes
import com.lsykje.diary.ui.theme.DiaryColors
import com.lsykje.diary.ui.theme.DiaryType
import com.lsykje.diary.ui.theme.PrimaryButton

@Composable
fun LockScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val lockPrefs = remember { LockPrefs(context) }

    LaunchedEffect(Unit) {
        if (!lockPrefs.hasAnyLockMethod()) {
            navController.navigate(Routes.lockSetup(firstRun = true)) {
                popUpTo(Routes.Lock) { inclusive = true }
            }
        }
    }

    fun goToCalendar() {
        navController.navigate(Routes.Calendar) {
            popUpTo(Routes.Lock) { inclusive = true }
        }
    }

    val canBiometric = lockPrefs.biometricEnabled && BiometricAuthenticator.isAvailable(activity)
    val hasCodeMethod = lockPrefs.hasPin || lockPrefs.hasPattern

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiaryColors.Surface)
            .padding(horizontal = 32.dp, vertical = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(DiaryColors.PrimaryPastel, RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = DiaryColors.PrimaryStrong)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("나의 일기장", style = DiaryType.Display, color = DiaryColors.Ink)
                Text("오늘 하루를 기록해보세요", style = DiaryType.Secondary.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = DiaryColors.InkSoft)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (canBiometric) {
                PrimaryButton(
                    text = "지문으로 잠금 해제",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        BiometricAuthenticator.authenticate(
                            activity = activity,
                            onSuccess = { goToCalendar() },
                            onFailOrError = { /* 사용자가 다시 시도하거나 아래 링크로 전환 */ },
                        )
                    },
                )
                if (hasCodeMethod) {
                    Text(
                        "PIN 또는 패턴으로 잠금 해제",
                        style = DiaryType.Secondary,
                        color = DiaryColors.InkSoft,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { navController.navigate(Routes.LockEntry) },
                    )
                }
            } else if (hasCodeMethod) {
                PrimaryButton(
                    text = "PIN 또는 패턴으로 잠금 해제",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { navController.navigate(Routes.LockEntry) },
                )
            }
        }
    }
}

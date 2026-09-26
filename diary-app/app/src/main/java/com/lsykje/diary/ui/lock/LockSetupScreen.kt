package com.lsykje.diary.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.lsykje.diary.auth.BiometricAuthenticator
import com.lsykje.diary.auth.LockPrefs
import com.lsykje.diary.ui.nav.Routes
import com.lsykje.diary.ui.theme.DiaryColors
import com.lsykje.diary.ui.theme.DiaryType
import com.lsykje.diary.ui.theme.PrimaryButton

/**
 * 설계 문서 5-1-1절: 잠금 방식(지문·PIN·패턴)을 등록/변경/삭제하는 화면.
 * 최초 실행(firstRun)에서도, 설정 화면에서 재진입할 때도 이 화면 하나로 처리한다.
 * 항상 최소 1개의 방식은 남아 있어야 한다는 원칙을 여기서 강제한다.
 */
@Composable
fun LockSetupScreen(navController: NavController, firstRun: Boolean) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val lockPrefs = remember { LockPrefs(context) }
    val biometricAvailable = remember { BiometricAuthenticator.isAvailable(activity) }

    var biometricOn by remember { mutableStateOf(lockPrefs.biometricEnabled) }
    var hasPin by remember { mutableStateOf(lockPrefs.hasPin) }
    var hasPattern by remember { mutableStateOf(lockPrefs.hasPattern) }
    var activeSetup by remember { mutableStateOf<String?>(null) }

    fun enabledCount() = listOf(biometricOn, hasPin, hasPattern).count { it }
    fun canRemove() = enabledCount() > 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DiaryColors.Surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            if (firstRun) "잠금 방식을 설정해주세요" else "잠금 방식 관리",
            style = DiaryType.Title,
            color = DiaryColors.Ink,
        )
        Text(
            "최소 1가지 이상 등록해야 하며, 여러 개를 함께 등록할 수 있어요",
            style = DiaryType.CaptionRegular,
            color = DiaryColors.InkSoft,
        )

        SetupRow(
            title = "지문 / 얼굴 인증",
            statusText = if (!biometricAvailable) "이 기기에서는 사용할 수 없어요" else if (biometricOn) "사용 중" else "꺼짐",
        ) {
            Switch(
                checked = biometricOn && biometricAvailable,
                enabled = biometricAvailable,
                onCheckedChange = { value ->
                    if (value || canRemove()) {
                        biometricOn = value
                        lockPrefs.biometricEnabled = value
                    }
                },
                colors = SwitchDefaults.colors(checkedTrackColor = DiaryColors.PrimaryStrong),
            )
        }

        SetupRow(
            title = "숫자 PIN (4~6자리)",
            statusText = if (hasPin) "설정됨" else "설정 안 됨",
        ) {
            if (hasPin) {
                TextButton(onClick = {
                    if (canRemove()) {
                        lockPrefs.clearPin()
                        hasPin = false
                    }
                }, enabled = canRemove()) {
                    Text("삭제", color = DiaryColors.InkSoft)
                }
            }
            TextButton(onClick = { activeSetup = if (activeSetup == "pin") null else "pin" }) {
                Text(if (hasPin) "변경" else "설정", color = DiaryColors.PrimaryStrong)
            }
        }
        if (activeSetup == "pin") {
            PinPad(
                onSubmit = { pin ->
                    lockPrefs.setPin(pin)
                    hasPin = true
                    activeSetup = null
                },
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        SetupRow(
            title = "패턴 (9점 연결)",
            statusText = if (hasPattern) "설정됨" else "설정 안 됨",
        ) {
            if (hasPattern) {
                TextButton(onClick = {
                    if (canRemove()) {
                        lockPrefs.clearPattern()
                        hasPattern = false
                    }
                }, enabled = canRemove()) {
                    Text("삭제", color = DiaryColors.InkSoft)
                }
            }
            TextButton(onClick = { activeSetup = if (activeSetup == "pattern") null else "pattern" }) {
                Text(if (hasPattern) "변경" else "설정", color = DiaryColors.PrimaryStrong)
            }
        }
        if (activeSetup == "pattern") {
            PatternPad(
                onComplete = { cells ->
                    lockPrefs.setPattern(cells)
                    hasPattern = true
                    activeSetup = null
                },
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        val canFinish = biometricOn || hasPin || hasPattern
        PrimaryButton(
            text = if (firstRun) "완료" else "뒤로",
            enabled = canFinish,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            onClick = {
                if (firstRun) {
                    navController.navigate(Routes.Calendar) {
                        popUpTo(Routes.Lock) { inclusive = true }
                    }
                } else {
                    navController.popBackStack()
                }
            },
        )
    }
}

@Composable
private fun SetupRow(title: String, statusText: String, actions: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DiaryColors.Card, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(title, style = DiaryType.BodyStrong, color = DiaryColors.Ink)
            Text(statusText, style = DiaryType.CaptionRegular, color = DiaryColors.InkSoft)
        }
        Row(verticalAlignment = Alignment.CenterVertically) { actions() }
    }
}

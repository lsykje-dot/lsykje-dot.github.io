package com.lsykje.diary.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lsykje.diary.ui.theme.DiaryColors
import com.lsykje.diary.ui.theme.DiaryType
import com.lsykje.diary.ui.theme.PrimaryButton

private const val MAX_PIN_LENGTH = 6
private const val MIN_PIN_LENGTH = 4

/** 설계 문서 5-1절: 숫자 PIN(4~6자리) 입력 화면 */
@Composable
fun PinPad(
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var digits by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(MAX_PIN_LENGTH) { index ->
                val filled = index < digits.length
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(
                            if (filled) DiaryColors.PrimaryStrong else DiaryColors.Border,
                            CircleShape,
                        ),
                )
            }
        }

        Column(
            modifier = Modifier.padding(top = 32.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "back"),
            )
            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .then(
                                    if (key.isNotEmpty()) {
                                        Modifier.clickable {
                                            when {
                                                key == "back" -> digits = digits.dropLast(1)
                                                digits.length < MAX_PIN_LENGTH -> digits += key
                                            }
                                        }
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            when (key) {
                                "back" -> Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "지우기", tint = DiaryColors.InkSoft)
                                "" -> {}
                                else -> Text(key, style = DiaryType.Title, color = DiaryColors.Ink)
                            }
                        }
                    }
                }
            }
        }

        PrimaryButton(
            text = "확인",
            enabled = digits.length in MIN_PIN_LENGTH..MAX_PIN_LENGTH,
            onClick = { onSubmit(digits) },
            modifier = Modifier.padding(top = 28.dp).fillMaxWidth(),
        )
    }
}

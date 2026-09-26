package com.lsykje.diary.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 디자인 캔버스의 tokens.css와 1:1로 대응하는 공용 버튼/칩 컴포넌트.
 * Material3 기본 Button 대신, 시안에서 정한 높이·둥글기·색을 정확히 맞추기 위해 직접 만듦.
 */

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val bg = if (enabled) DiaryColors.PrimaryStrong else DiaryColors.Disabled
    val fg = if (enabled) DiaryColors.OnPrimary else DiaryColors.OnDisabled
    androidx.compose.material3.Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = DiaryShapes.Medium,
        color = bg,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            leadingIcon?.invoke()
            Text(text, style = DiaryType.Section, color = fg)
        }
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp).border(1.5.dp, DiaryColors.Border, DiaryShapes.Medium),
        shape = DiaryShapes.Medium,
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            leadingIcon?.invoke()
            Text(text, style = DiaryType.Body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = DiaryColors.Ink)
        }
    }
}

@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = DiaryColors.PrimaryPastel,
    contentColor: Color = DiaryColors.PrimaryStrong,
) {
    Row(
        modifier = modifier
            .background(background, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(text, style = DiaryType.Caption, color = contentColor)
    }
}

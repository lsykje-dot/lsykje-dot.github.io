package com.lsykje.diary.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DiaryColorScheme = lightColorScheme(
    primary = DiaryColors.PrimaryStrong,
    onPrimary = DiaryColors.OnPrimary,
    primaryContainer = DiaryColors.PrimaryPastel,
    onPrimaryContainer = DiaryColors.PrimaryStrong,
    background = DiaryColors.Surface,
    onBackground = DiaryColors.Ink,
    surface = DiaryColors.Card,
    onSurface = DiaryColors.Ink,
    outline = DiaryColors.Border,
)

private val DiaryShapeSet = Shapes(
    extraSmall = DiaryShapes.Small,
    small = DiaryShapes.Small,
    medium = DiaryShapes.Medium,
    large = DiaryShapes.Large,
)

@Composable
fun DiaryAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DiaryColorScheme,
        shapes = DiaryShapeSet,
        content = content,
    )
}

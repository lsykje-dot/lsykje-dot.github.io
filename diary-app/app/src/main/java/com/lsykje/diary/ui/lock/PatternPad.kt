package com.lsykje.diary.ui.lock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.lsykje.diary.ui.theme.DiaryColors
import kotlin.math.hypot

private const val GRID_SIZE = 3
private const val MIN_PATTERN_LENGTH = 4
private const val DOT_HIT_RADIUS_DP = 28

/**
 * 설계 문서 5-1절: 패턴(9점 연결) 입력. 손가락을 떼지 않고 점 사이를 이어서 그리는
 * 일반적인 안드로이드 패턴 잠금과 같은 방식으로 구현.
 */
@Composable
fun PatternPad(
    onComplete: (List<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf(listOf<Int>()) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val hitRadiusPx = with(density) { DOT_HIT_RADIUS_DP.dp.toPx() }

        Canvas(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(260.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            selected = emptyList()
                            dragPosition = offset
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragPosition = change.position
                            val dots = dotCenters(size.width.toFloat(), size.height.toFloat())
                            dots.forEachIndexed { index, center ->
                                if (index !in selected && distance(center, change.position) <= hitRadiusPx) {
                                    selected = selected + index
                                }
                            }
                        },
                        onDragEnd = {
                            dragPosition = null
                            if (selected.size >= MIN_PATTERN_LENGTH) {
                                onComplete(selected)
                            }
                            selected = emptyList()
                        },
                        onDragCancel = {
                            dragPosition = null
                            selected = emptyList()
                        },
                    )
                },
        ) {
            val dots = dotCenters(size.width, size.height)
            val dotRadius = size.width / 18f

            if (selected.size > 1) {
                for (i in 0 until selected.size - 1) {
                    drawLine(
                        color = DiaryColors.PrimaryStrong,
                        start = dots[selected[i]],
                        end = dots[selected[i + 1]],
                        strokeWidth = 6f,
                    )
                }
            }
            val last = selected.lastOrNull()
            if (last != null) {
                dragPosition?.let { pos ->
                    drawLine(color = DiaryColors.PrimaryStrong, start = dots[last], end = pos, strokeWidth = 6f)
                }
            }

            dots.forEachIndexed { index, center ->
                val isSelected = index in selected
                drawCircle(
                    color = if (isSelected) DiaryColors.PrimaryStrong else DiaryColors.Border,
                    radius = dotRadius,
                    center = center,
                    style = Stroke(width = if (isSelected) dotRadius else dotRadius * 0.35f),
                )
                if (isSelected) {
                    drawCircle(color = DiaryColors.PrimaryStrong, radius = dotRadius * 0.35f, center = center)
                }
            }
        }
    }
}

private fun dotCenters(width: Float, height: Float): List<Offset> {
    val stepX = width / (GRID_SIZE + 1)
    val stepY = height / (GRID_SIZE + 1)
    return (0 until GRID_SIZE * GRID_SIZE).map { index ->
        val row = index / GRID_SIZE
        val col = index % GRID_SIZE
        Offset(stepX * (col + 1), stepY * (row + 1))
    }
}

private fun distance(a: Offset, b: Offset): Float = hypot(a.x - b.x, a.y - b.y)

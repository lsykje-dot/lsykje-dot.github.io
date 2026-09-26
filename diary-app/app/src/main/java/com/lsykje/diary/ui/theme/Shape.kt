package com.lsykje.diary.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** 디자인 캔버스의 "모서리 둥글기" 비교에서 16dp가 기본값으로 확정됨 */
object DiaryShapes {
    val Small = RoundedCornerShape(12.dp)
    val Medium = RoundedCornerShape(16.dp)
    val Large = RoundedCornerShape(24.dp)
    val Pill = RoundedCornerShape(999.dp)
}

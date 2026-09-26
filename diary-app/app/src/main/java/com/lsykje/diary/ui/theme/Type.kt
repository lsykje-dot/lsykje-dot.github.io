package com.lsykje.diary.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 디자인 캔버스에서 9단계 → 6단계로 압축한 글자 크기 체계.
 * 폰트는 시스템 기본 산세리프(한글 로캘에서는 OS가 Noto Sans CJK KR 계열을 사용함)를 그대로 씀 —
 * 별도 폰트 파일을 앱에 번들하지 않음.
 */
object DiaryType {
    val Display = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Black)
    val Title = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold)
    val Section = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
    val BodyStrong = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
    val Body = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp)
    val Secondary = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold)
    val Caption = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
    val CaptionRegular = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal)
}

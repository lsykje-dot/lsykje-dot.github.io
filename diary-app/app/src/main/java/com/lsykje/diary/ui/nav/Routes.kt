package com.lsykje.diary.ui.nav

import java.time.LocalDate

object Routes {
    const val Lock = "lock"
    const val LockEntry = "lock_entry"
    const val LockSetup = "lock_setup/{firstRun}"
    const val Calendar = "calendar"
    const val DiaryEdit = "diary_edit/{date}"
    const val DiaryDetail = "diary_detail/{date}"
    const val Settings = "settings"
    const val ComingSoon = "coming_soon/{title}"

    fun lockSetup(firstRun: Boolean) = "lock_setup/$firstRun"
    fun diaryEdit(date: LocalDate) = "diary_edit/$date"
    fun diaryDetail(date: LocalDate) = "diary_detail/$date"
    fun comingSoon(title: String) = "coming_soon/$title"
}

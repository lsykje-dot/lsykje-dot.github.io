package com.lsykje.diary

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.lsykje.diary.ui.nav.DiaryNavHost
import com.lsykje.diary.ui.theme.DiaryAppTheme

/**
 * BiometricPrompt는 FragmentActivity를 요구하므로 ComponentActivity가 아닌
 * FragmentActivity를 상속한다(둘 다 setContent{}로 Compose를 그대로 쓸 수 있음).
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DiaryAppTheme {
                DiaryNavHost()
            }
        }
    }
}

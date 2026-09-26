package com.lsykje.diary.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lsykje.diary.ui.calendar.CalendarScreen
import com.lsykje.diary.ui.diary.DiaryDetailScreen
import com.lsykje.diary.ui.diary.DiaryEditScreen
import com.lsykje.diary.ui.lock.LockEntryScreen
import com.lsykje.diary.ui.lock.LockScreen
import com.lsykje.diary.ui.lock.LockSetupScreen
import com.lsykje.diary.ui.settings.ComingSoonScreen
import com.lsykje.diary.ui.settings.SettingsScreen
import java.time.LocalDate

@Composable
fun DiaryNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.Lock) {

        composable(Routes.Lock) { LockScreen(navController) }

        composable(Routes.LockEntry) { LockEntryScreen(navController) }

        composable(
            Routes.LockSetup,
            arguments = listOf(navArgument("firstRun") { type = NavType.BoolType }),
        ) { backStackEntry ->
            val firstRun = backStackEntry.arguments?.getBoolean("firstRun") ?: false
            LockSetupScreen(navController, firstRun)
        }

        composable(Routes.Calendar) { CalendarScreen(navController) }

        composable(
            Routes.DiaryEdit,
            arguments = listOf(navArgument("date") { type = NavType.StringType }),
        ) { backStackEntry ->
            val date = LocalDate.parse(backStackEntry.arguments?.getString("date"))
            DiaryEditScreen(navController, date)
        }

        composable(
            Routes.DiaryDetail,
            arguments = listOf(navArgument("date") { type = NavType.StringType }),
        ) { backStackEntry ->
            val date = LocalDate.parse(backStackEntry.arguments?.getString("date"))
            DiaryDetailScreen(navController, date)
        }

        composable(Routes.Settings) { SettingsScreen(navController) }

        composable(
            Routes.ComingSoon,
            arguments = listOf(navArgument("title") { type = NavType.StringType }),
        ) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: ""
            ComingSoonScreen(navController, title)
        }
    }
}

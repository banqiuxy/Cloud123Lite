package com.banqiu.thirdparty123pan.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.banqiu.thirdparty123pan.ui.screens.about.AboutScreen
import com.banqiu.thirdparty123pan.ui.screens.home.FileDetailScreen
import com.banqiu.thirdparty123pan.ui.screens.home.HomeScreen
import com.banqiu.thirdparty123pan.ui.screens.home.PreviewScreen
import com.banqiu.thirdparty123pan.ui.screens.home.SearchScreen
import com.banqiu.thirdparty123pan.ui.screens.login.LoginScreen
import com.banqiu.thirdparty123pan.ui.screens.main.MainScreen
import com.banqiu.thirdparty123pan.ui.screens.profile.RecycleBinScreen
import com.banqiu.thirdparty123pan.ui.screens.profile.SettingsScreen
import com.banqiu.thirdparty123pan.ui.screens.profile.ShareListScreen
import com.banqiu.thirdparty123pan.ui.screens.splash.SplashScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateLogin = { navController.navigate(Routes.LOGIN) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                } },
                onNavigateMain = { navController.navigate(Routes.MAIN) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                } }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                onNavigateSearch = { navController.navigate(Routes.SEARCH) },
                onNavigateSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateRecycleBin = { navController.navigate(Routes.RECYCLE_BIN) },
                onNavigateShares = { navController.navigate(Routes.SHARES) },
                onNavigateAbout = { navController.navigate(Routes.ABOUT) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                onOpenFile = { item ->
                    navController.navigate(
                        Routes.fileDetail(
                            item.fileId, item.name, item.size, item.updateTime, item.parentId, item.isFolder
                        )
                    )
                },
                onOpenPreview = { item ->
                    navController.navigate(Routes.preview(item.fileId, item.name))
                }
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onOpenFile = { item ->
                    navController.navigate(
                        Routes.fileDetail(
                            item.fileId, item.name, item.size, item.updateTime, item.parentId, item.isFolder
                        )
                    )
                },
                onOpenPreview = { item ->
                    navController.navigate(Routes.preview(item.fileId, item.name))
                }
            )
        }

        composable(
            route = Routes.FILE_DETAIL,
            arguments = listOf(
                navArgument("fileId") { type = NavType.LongType },
                navArgument("name") { type = NavType.StringType; defaultValue = "" },
                navArgument("size") { type = NavType.LongType; defaultValue = 0L },
                navArgument("modTime") { type = NavType.LongType; defaultValue = 0L },
                navArgument("parentId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("isFolder") { type = NavType.BoolType; defaultValue = false }
            )
        ) { entry ->
            val fileName = entry.arguments?.getString("name") ?: ""
            FileDetailScreen(
                fileId = entry.arguments?.getLong("fileId") ?: 0L,
                name = fileName,
                size = entry.arguments?.getLong("size") ?: 0L,
                modTime = entry.arguments?.getLong("modTime") ?: 0L,
                parentId = entry.arguments?.getLong("parentId") ?: 0L,
                isFolder = entry.arguments?.getBoolean("isFolder") ?: false,
                onBack = { navController.popBackStack() },
                onOpenPreview = { navController.navigate(Routes.preview(it, fileName)) }
            )
        }

        composable(
            route = Routes.PREVIEW,
            arguments = listOf(
                navArgument("fileId") { type = NavType.LongType },
                navArgument("name") { type = NavType.StringType; defaultValue = "" }
            )
        ) { entry ->
            PreviewScreen(
                fileId = entry.arguments?.getLong("fileId") ?: 0L,
                name = entry.arguments?.getString("name") ?: "",
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.RECYCLE_BIN) {
            RecycleBinScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SHARES) {
            ShareListScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
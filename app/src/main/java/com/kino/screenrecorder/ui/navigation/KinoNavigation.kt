package com.kino.screenrecorder.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kino.screenrecorder.ui.screens.editor.EditorScreen
import com.kino.screenrecorder.ui.screens.onboarding.OnboardingScreen
import com.kino.screenrecorder.ui.screens.preview.PreviewScreen
import com.kino.screenrecorder.ui.screens.projects.ProjectsScreen
import com.kino.screenrecorder.ui.screens.recording.RecordingScreen
import com.kino.screenrecorder.ui.screens.settings.SettingsScreen

@Composable
fun KinoNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Onboarding.route,
        modifier = modifier
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onNavigateToProjects = {
                    navController.navigate(Screen.Projects.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Projects.route) {
            ProjectsScreen(
                onNavigateToRecording = {
                    navController.navigate(Screen.Recording.route)
                },
                onNavigateToPreview = { videoId ->
                    navController.navigate(Screen.Preview.createRoute(videoId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        composable(Screen.Recording.route) {
            RecordingScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Preview.route) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
            PreviewScreen(
                videoId = videoId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEditor = { videoId ->
                    navController.navigate(Screen.Editor.createRoute(videoId))
                }
            )
        }
        
        composable(Screen.Editor.route) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
            EditorScreen(
                videoId = videoId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Projects : Screen("projects")
    object Recording : Screen("recording")
    object Preview : Screen("preview/{videoId}") {
        fun createRoute(videoId: String) = "preview/$videoId"
    }
    object Editor : Screen("editor/{videoId}") {
        fun createRoute(videoId: String) = "editor/$videoId"
    }
    object Settings : Screen("settings")
}
package com.lji.assignment.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Main composable that manages the application's navigation state.
 */
@Composable
fun AppNavHost() {

    // Manage navigation state for switching between screens.
    var currentScreen by remember { mutableStateOf<Screen>(Screen.RepoList) }

    when (val screen = currentScreen) {
        is Screen.RepoList -> {
            RepoListScreen(
                onRepoClick = { url ->
                    currentScreen = Screen.WebView(url)
                }
            )
        }
        is Screen.WebView -> {
            WebViewScreen(
                url = screen.url,
                onBack = { currentScreen = Screen.RepoList }
            )
        }
    }
}

/**
 * Sealed class to represent the different screens in the app.
 * This is a simple way to manage navigation without using the Jetpack Navigation component.
 */
sealed class Screen {
    object RepoList : Screen()
    data class WebView(val url: String) : Screen()
}
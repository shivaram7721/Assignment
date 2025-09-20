package com.lji.assignment.presentation.screens

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * A Composable screen that displays a WebView to load a given URL.
 *
 * This screen handles its own back navigation logic, allowing the user to navigate
 * back within the WebView's history before exiting the screen.
 *
 * @param url The URL to be loaded and displayed in the WebView.
 * @param onBack A callback function to be executed when the user presses the back button
 * and there is no further history to navigate back to within the WebView.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(url: String, onBack: () -> Unit) {
    val context = LocalContext.current
    // We remember the WebView instance to use it in our back press logic.
    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            webViewClient = WebViewClient()
        }
    }

    // Access the back press dispatcher to add our custom behavior.
    val backPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    // Use DisposableEffect to manage the lifecycle of the back press callback.
    // The effect is triggered when `backPressedDispatcher` changes.
    DisposableEffect(backPressedDispatcher) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Check if the WebView has history to go back to.
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    onBack()
                }
            }
        }

        // Add the callback to the dispatcher.
        backPressedDispatcher?.addCallback(callback)

        // Clean up the callback when the composable is removed from the screen.
        onDispose {
            callback.remove()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Web View") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        // Use AndroidView to embed the WebView in the Composable layout,
        // and apply the padding from the Scaffold.
        AndroidView(
            factory = { webView },
            update = { it.loadUrl(url) },
            modifier = Modifier.padding(innerPadding) // Fills the remaining space
        )
    }
}
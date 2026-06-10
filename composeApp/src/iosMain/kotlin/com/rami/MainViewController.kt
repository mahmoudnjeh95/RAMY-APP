package com.rami

import androidx.compose.ui.window.ComposeUIViewController

/**
 * Called from Swift/SwiftUI via the generated Kotlin/Native framework.
 * In iosApp/ContentView.swift:
 *   ComposeView().ignoresSafeArea()
 */
fun MainViewController() = ComposeUIViewController { App() }

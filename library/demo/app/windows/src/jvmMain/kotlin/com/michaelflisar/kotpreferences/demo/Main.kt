package com.michaelflisar.kotpreferences.demo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers

fun main() = application {

    Window(
        title = "KotPreferences Demo",
        onCloseRequest = ::exitApplication
    ) {
        DemoContent(
            modifier = Modifier.fillMaxSize(),
            platform = "Windows",
            ioContext = Dispatchers.IO
        )
    }
}
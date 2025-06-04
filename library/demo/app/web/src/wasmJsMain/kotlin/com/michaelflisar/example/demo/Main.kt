package com.michaelflisar.example.demo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.CanvasBasedWindow
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
fun main() {
    CanvasBasedWindow("Demo", canvasElementId = "ComposeTarget") {
        MaterialTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text("Demo", modifier = Modifier.padding(8.dp))
                        }
                    )
                }
            ) { paddingValues ->
                DemoContent(
                    modifier = Modifier.padding(paddingValues).fillMaxSize(),
                    platform = "WASM",
                    ioContext = Dispatchers.Default
                )
            }
        }
    }
}
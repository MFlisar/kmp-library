package com.michaelflisar.example.demo

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.coroutines.CoroutineContext

@Composable
fun DemoContent(
    modifier: Modifier,
    platform: String,
    ioContext: CoroutineContext
) {
    Text("Platform: $platform", modifier = Modifier.padding(16.dp))
}

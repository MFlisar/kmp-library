package com.michaelflisar.example.core

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

actual val ContextIO: CoroutineContext = Dispatchers.Default
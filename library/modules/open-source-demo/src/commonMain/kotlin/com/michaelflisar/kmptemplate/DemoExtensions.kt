package com.michaelflisar.kmptemplate

import androidx.compose.ui.graphics.Color

internal fun Color.disabled() = copy(alpha = 0.38f)

internal fun <T> MutableList<T>.removeFirstSave() : T {
    return removeAt(0)
}
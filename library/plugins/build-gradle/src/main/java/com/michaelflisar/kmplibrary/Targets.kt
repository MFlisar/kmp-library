package com.michaelflisar.kmplibrary

class Targets(
    val android: Boolean = false,
    val iOS: Boolean = false,
    val windows: Boolean = false,
    val linux: Boolean = false,
    val macOS: Boolean = false,
    val wasm: Boolean = false,
    val js: Boolean = false,
) {
    private val enabledTargets = Target.values()
        .filter {
            when (it) {
                Target.ANDROID -> android
                Target.IOS -> iOS
                Target.WINDOWS -> windows
                Target.MACOS -> macOS
                Target.LINUX -> linux
                Target.WASM -> wasm
                Target.JS -> js
            }
        }

    fun isEnabled(target: Target) = enabledTargets.contains(target)
}
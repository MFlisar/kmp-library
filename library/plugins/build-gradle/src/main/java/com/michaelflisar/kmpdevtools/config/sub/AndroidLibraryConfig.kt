package com.michaelflisar.kmpdevtools.config.sub

import org.gradle.api.provider.Provider

class AndroidLibraryConfig(
    val compileSdk: Provider<String>,
    val minSdk: Provider<String>,
    val enableAndroidResources: Boolean = true
)
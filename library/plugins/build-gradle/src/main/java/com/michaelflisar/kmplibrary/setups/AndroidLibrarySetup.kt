package com.michaelflisar.kmplibrary.setups

import org.gradle.api.provider.Provider

class AndroidLibrarySetup(
    val compileSdk: Provider<String>,
    val minSdk: Provider<String>,
    val enableAndroidResources: Boolean = true
)
package com.michaelflisar.kmplibrary.setups

import org.gradle.api.provider.Provider

class AndroidSetup(
    val compileSdk: Provider<String>,
    val minSdk: Provider<String>
)
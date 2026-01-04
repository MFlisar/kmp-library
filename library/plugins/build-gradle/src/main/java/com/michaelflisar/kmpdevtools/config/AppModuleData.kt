package com.michaelflisar.kmpdevtools.config

import com.michaelflisar.kmpdevtools.config.sub.AndroidAppConfig
import com.michaelflisar.kmpdevtools.config.sub.DesktopAppConfig
import com.michaelflisar.kmpdevtools.config.sub.WasmAppConfig
import com.michaelflisar.kmpdevtools.core.configs.Config
import org.gradle.api.Project

class AppModuleData(
    val project: Project,
    val config: Config,
    val appName: String,
    val namespace: String,
    val versionCode: Int,
    val versionName: String,
    val androidConfig: AndroidAppConfig? = null,
    val desktopConfig: DesktopAppConfig? = null,
    val wasmConfig: WasmAppConfig? = null,
)
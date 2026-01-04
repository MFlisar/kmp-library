package com.michaelflisar.kmpdevtools.config

import com.michaelflisar.kmpdevtools.config.sub.AndroidLibraryConfig
import com.michaelflisar.kmpdevtools.core.configs.Config
import com.michaelflisar.kmpdevtools.core.configs.LibraryConfig
import org.gradle.api.Project

class LibraryModuleData(
    val project: Project,
    val config: Config,
    val libraryConfig: LibraryConfig,
    val androidConfig: AndroidLibraryConfig? = null,
)
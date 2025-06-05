package com.michaelflisar.buildlogic.classes

import com.michaelflisar.buildlogic.shared.Setup

class ModuleMetaData(
    val artifactId: String,
    val androidNamespace: String,
    val description: String
) {
    fun libraryDescription(setup: Setup): String {
        val library = setup.library.name
        return "$library - $artifactId module - $description"
    }
}
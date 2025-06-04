package com.michaelflisar.buildlogic.classes

import org.gradle.api.Project

class ModuleMetaData(
    val artifactId: String,
    val androidNamespace: String,
    val description: String
) {
    fun libraryDescription(project: Project): String {
        val library = LIBRARY_NAME.loadString(project)
        return "$library - $artifactId module - $description"
    }


    fun licenseUrl(project: Project): String {
        val github = LIBRARY_GITHUB.loadString(project)
        return "${github}/blob/main/LICENSE"
    }
}
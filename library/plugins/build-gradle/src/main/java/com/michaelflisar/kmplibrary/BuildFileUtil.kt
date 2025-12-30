package com.michaelflisar.kmplibrary

import org.gradle.api.Project
import java.io.File

object BuildFileUtil {

    fun checkGradleProperty(project: Project, property: String): Boolean? {
        if (!project.providers.gradleProperty(property).isPresent) {
            return null
        }
        return project.providers.gradleProperty(property).get().toBoolean()
    }



}
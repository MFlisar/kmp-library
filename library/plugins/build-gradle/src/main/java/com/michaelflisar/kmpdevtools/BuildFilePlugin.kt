package com.michaelflisar.kmpdevtools

import org.gradle.api.Plugin
import org.gradle.api.Project

class BuildFilePlugin : Plugin<Project> {

    private lateinit var project: Project

    override fun apply(project: Project) {
        this.project = project
    }

}
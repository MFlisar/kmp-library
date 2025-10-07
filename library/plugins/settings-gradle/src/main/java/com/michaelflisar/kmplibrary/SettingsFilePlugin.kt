package com.michaelflisar.kmplibrary

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import java.io.File

class SettingsFilePlugin : Plugin<Settings> {

    private lateinit var settings: Settings

    override fun apply(settings: Settings) {
        this.settings = settings
    }

    fun checkGradleProperty(property: String): Boolean? {
        if (!settings.providers.gradleProperty(property).isPresent) {
            return null
        }
        return settings.providers.gradleProperty(property).get().toBoolean()
    }



    /**
     * include a module like following:
     *
     * includeModule(":toolbox:core") => "$folder\\library\\core"
     */
    fun includeModule(fullPath: String, name: String) {
        settings.include(name)
        settings.project(name).projectDir = File(fullPath)
    }

    /**
     * include a module like following:
     *
     * includeToolbox(":toolbox:core") => "$folder\\library\\core"
     * includeToolbox(":toolbox:modules:ui") => "$folder\\library\\modules\\ui"
     * ...
     *
     * or for root based modules:
     * includeToolbox("toolbox:demo", true) => "$folder\\demo"
     * ...
     *
     * @param toolboxFolder the folder where the toolbox library is located
     * @param name the module name
     * @param isInRoot if true, the module is in the root of the toolbox folder, otherwise in the library folder
     */
    fun includeToolbox(toolboxFolder: String, name: String, isInRoot: Boolean = false) {
        val folder = "$toolboxFolder\\Toolbox"
        val relativePath =  name.replaceFirst("toolbox", if (isInRoot) "" else "library")
            .replace("::", ":")
            .replace(":", "\\").removePrefix("\\")
        println("relativePath: $relativePath")
        includeModule("$folder\\$relativePath", name)
    }

}
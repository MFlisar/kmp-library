package com.michaelflisar.kmplibrary

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

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

}
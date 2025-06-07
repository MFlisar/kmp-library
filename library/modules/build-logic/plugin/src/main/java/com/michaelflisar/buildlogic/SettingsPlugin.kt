package com.michaelflisar.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

class SettingPlugin : Plugin<Settings> {

    private lateinit var settings: Settings

    override fun apply(settings: Settings) {
        this.settings = settings
    }
}
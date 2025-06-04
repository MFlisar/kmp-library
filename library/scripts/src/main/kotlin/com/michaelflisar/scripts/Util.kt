package com.michaelflisar.scripts

import java.io.File
import java.io.FileInputStream
import java.util.Properties

object Util {

    fun rootFolder() = System.getProperty("user.dir")

    fun gradleProperties(folder: File): Properties {
        val props = Properties()
        FileInputStream(File(folder, "gradle.properties")).use { props.load(it) }
        return props
    }
}
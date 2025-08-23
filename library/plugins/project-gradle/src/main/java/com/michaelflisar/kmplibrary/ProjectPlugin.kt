package com.michaelflisar.kmplibrary

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import java.io.File

class ProjectPlugin : Plugin<Project> {

    private lateinit var project: Project
    private var logging: Boolean = false

    override fun apply(target: Project) {
        project = target
    }

    fun getDevPath() =
        if (project.providers.gradleProperty("work").isPresent) "D:/dev" else "M:/dev"

    fun extractProguardMapFromAAB(
        appName: String,
        versionName: String,
        outputFolder: () -> String = {
            val dev = getDevPath()
            "$dev/06 - retrace"
        },
    ) {
        with(project) {

            val outputDir = outputFolder()

            //val dev = if (providers.gradleProperty("work").isPresent) "D:/dev" else "M:/dev"
            val projectName = project.name
            val aabFile = file("release/$projectName-release.aab")

            // Files / Paths
            val proguardMapRootZipPath = "BUNDLE-METADATA"
            val proguardMapZipPath =
                "$proguardMapRootZipPath/com.android.tools.build.obfuscation/proguard.map"
            val proguardMapOutput = File("$outputDir/$appName - Proguard $versionName.map")
            val proguardTmpFolder = File("$outputDir/$proguardMapRootZipPath")
            val proguardFile = File(proguardMapOutput.parentFile, proguardMapZipPath)

            // 1) das ProGuard-Map-File aus der aab extrahieren
            copy {
                copySpec {
                    from(zipTree(aabFile)) {
                        include(proguardMapZipPath)
                    }
                    into(proguardMapOutput.parentFile)
                }
            }

            // 2) das ProGuard-Map-File umbenennen
            if (proguardMapOutput.exists())
                proguardMapOutput.delete()
            val success = proguardFile.renameTo(proguardMapOutput)

            // 3) die alten Dateien löschen
            proguardTmpFolder.deleteRecursively()

            if (success) {
                println("ProGuard-Map-Datei wurde in ${proguardMapOutput.absolutePath} umbenannt.")
            } else {
                throw kotlin.Exception("ERROR - ProGuard-Map-Datei wurde NICHT in ${proguardMapOutput.absolutePath} umbenannt!")
            }
        }
    }
}
package com.michaelflisar.kmplibrary

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import java.io.File

open class SettingsFilePluginExtension {

    fun useLiveDependencies(settings: Settings): Boolean {
        return settings.providers.gradleProperty("useLiveDependencies").takeIf { it.isPresent }
            ?.get()?.toBoolean() ?: true
    }

    var logging: Boolean = false

    var includeLibraries: Boolean = false

    var libsFolder: (settings: Settings) -> String = { settings ->
        val dev = if (settings.providers.gradleProperty("work").isPresent) "D:\\dev" else "M:\\dev"
        "$dev\\11 - libs (mine)"
    }

    val excludedFoldersFromPath = listOf("library")

    var libsToInclude: List<String> = listOf(
        // utilities
        "CacheFileProvider",
        "FeedbackManager",
        // main
        "Lumberjack",
        "KotPreferences",
        "KotBilling",
        // compose
        "ComposeChangelog",
        "ComposeDebugDrawer",
        "ComposeDialogs",
        "ComposePreferences",
        "ComposeThemer",
        "ComposeColors",
        // others
        "Toolbox"
    )

    val libFilter: (project: String, relativePath: String) -> Boolean = { project, relativePath ->
        val excludedPathParts = listOf("demo", "test")
        val parts = relativePath.split("\\")
        if (parts.any { it in excludedPathParts }) {
            false
        } else {
            if (project == "Lumberjack") {
                // exclude old view based and timber modules
                val exclusions = listOf(
                    "library\\extensions\\viewer",
                    "library\\implementations\\timber",
                    "library\\loggers\\timber\\console",
                    "library\\loggers\\timber\\file",
                )
                !exclusions.any { relativePath == it }
            } else if (project == "ComposeChangelog") {
                // exclude gradle plugin
                val exclusions = listOf(
                    "library\\gradle-plugin",
                    "library\\gradle-plugin\\plugin"
                )
                !exclusions.any { relativePath == it }
            }
            else {
                true
            }
        }
    }
}

class SettingsFilePlugin : Plugin<Settings> {

    private lateinit var settings: Settings

    override fun apply(settings: Settings) {
        this.settings = settings

        val extension = settings.extensions.create(
            "settingsFilePlugin",
            SettingsFilePluginExtension::class.java
        )
        // Zugriff auf die Konfiguration z.B. nach der Auswertung:
        settings.gradle.settingsEvaluated {
            if (extension.includeLibraries) {
                includeAllMyLibs(extension)
            }
        }
    }

    fun includeAllMyLibs(extension: SettingsFilePluginExtension) {
        extension.libsToInclude.forEach {
            includeAllModules(it, extension)
        }
    }

    fun includeAllModules(
        libraryName: String,
        extension: SettingsFilePluginExtension
    ) {
        // 1) Root Folder
        val file = File(extension.libsFolder(settings), libraryName)

        // 2) alle Ordner mit einem build.gradle.kts File (ohne root, ohne demo) => das sind dann alle Module
        var subProjectFolders = file.walkTopDown()
            .filter { it.isFile && it.name == "build.gradle.kts" }
            .map { it.parentFile }
            .filter { it != file }
            .filter {
                val relativePath = it.relativeTo(file).path
                extension.libFilter(libraryName, relativePath)
            }

        // 3) root + alle intermidiate Ordner hinzufügen (für gradle > 9)
        subProjectFolders = subProjectFolders.map { moduleFolder ->
            val folders = mutableListOf<File>()
            val relative = moduleFolder.relativeTo(file).path
            val parts = relative.split("\\")
            // Alle Zwischenpfade generieren
            (1 until parts.size).map { i ->
                folders += file.resolve(parts.take(i).joinToString("\\"))
            }
            folders + moduleFolder
        }.flatten().distinct()

        if (extension.logging) {
            println("Project = ${file.absolutePath}")
        }

        // 3) Projekte inkludieren (relative Pfade ohne library)
        subProjectFolders.forEach { project ->
            val relativeSubProjectPath = project.relativeTo(file).path
            includeModule(file, relativeSubProjectPath, extension,"  - ")
        }
    }

    fun includeModule(
        folder: File,
        relativePath: String,
        extension: SettingsFilePluginExtension,
        printlnPrefix: String = ""
    ) {
        val relativeName = relativePath.split("\\")
            .filter { !extension.excludedFoldersFromPath.contains(it) }
            .joinToString(":") { it.lowercase() }

        val projectName = folder.name.lowercase()
        var name = ":$projectName"
        if (relativeName.isNotEmpty()) {
            name += ":$relativeName"
        }

        if (extension.logging) {
            //val fullPath = "$folder\\\$relativePath"
            println("${printlnPrefix}Single Project: \"$name\" ($relativePath)")
        }

        settings.include(name)
        settings.project(name).projectDir = File("$folder\\$relativePath")
    }

}
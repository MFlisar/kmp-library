package com.michaelflisar.kmplibrary

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Script
import org.gradle.api.artifacts.DependencySubstitutions
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.initialization.Settings
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.KotlinSettingsScript
import org.gradle.kotlin.dsl.SettingsScriptApi
import java.io.File
import kotlin.text.toBoolean

class ProjectPlugin : Plugin<Project> {

    private lateinit var project: Project

    override fun apply(target: Project) {
        project = target
    }

    fun checkGradleProperty(property: String): Boolean? {
        if (!project.providers.gradleProperty(property).isPresent) {
            return null
        }
        return project.providers.gradleProperty(property).get().toBoolean()
    }

    fun extractProguardMapFromAAB(
        appName: String,
        versionName: String,
        outputFolder: String
    ) {
        with(project) {

            val outputDir = outputFolder

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

fun Project.setupExtractProguardMapFromAAB(
    appName: String,
    appVersionName: String,
    outputFolder: String = "${if (project.providers.gradleProperty("work").isPresent) "D:/dev" else "M:/dev"}/06 - retrace"
) {
    val projectPlugin = project.plugins.getPlugin(ProjectPlugin::class.java)

    afterEvaluate {
        tasks.named("bundleRelease").configure {
            finalizedBy("extractProguardMap")
        }
    }

    tasks.register("extractProguardMap") {
        doLast {
            projectPlugin.extractProguardMapFromAAB(appName, appVersionName, outputFolder)
        }
    }
}

/**
 * usage:
 *
 * dependencySubstitution {
 *    if (projectPlugin.checkGradleProperty("useLocalToolbox") == true) {
 *         substitute(deps.toolbox.core, ":toolbox:core")
 *         substitute(deps.toolbox.app, ":toolbox:app")
 *         ...
 *     }
 * }
 *
 * @param lib the library to substitute, e.g. deps.toolbox.core
 * @param module the module to use instead, e.g. ":toolbox:core"
 */
fun DependencySubstitutions.substitute(
    lib: Provider<MinimalExternalModuleDependency>,
    module: String,
) {
    val dep = lib.get()
    val notation = "${dep.module.group}:${dep.module.name}"
    //println("substitute: $notation => $module")
    substitute(module(notation)).using(project(module))
}

fun Project.dependencySubstitution(
    block: DependencySubstitutions.() -> Unit
) {
    subprojects {
        configurations.matching {
            it.name.contains("Dependencies", ignoreCase = true) ||
                    it.name.endsWith("Classpath", ignoreCase = true)
        }.configureEach {
            resolutionStrategy {
                dependencySubstitution {
                    block()
                }
            }
        }
    }
}

/**
 * include a module like following:
 *
 * includeModule(":toolbox:core") => "$folder\\library\\core"
 */
fun Settings.includeModule(fullPath: String, name: String) {
    include(name)
    project(name).projectDir = File(fullPath)
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
fun Settings.includeToolbox(toolboxFolder: String, name: String, isInRoot: Boolean = false) {
    val folder = "$toolboxFolder\\Toolbox"
    val relativePath =  name.replaceFirst("toolbox", if (isInRoot) "" else "library")
        .replace("::", ":")
        .replace(":", "\\").removePrefix("\\")
    println("relativePath: $relativePath")
    includeModule("$folder\\$relativePath", name)
}
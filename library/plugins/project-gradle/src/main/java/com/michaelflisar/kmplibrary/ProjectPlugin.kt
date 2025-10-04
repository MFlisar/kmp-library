package com.michaelflisar.kmplibrary

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySubstitutions
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.ResolutionStrategy
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.KotlinBuildScript
import java.io.File

class ProjectPlugin : Plugin<Project> {

    private lateinit var project: Project

    override fun apply(target: Project) {
        project = target
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

fun DependencySubstitutions.substitute(
    lib: Provider<MinimalExternalModuleDependency>,
    module: String,
) {
    val dep = lib.get()
    val notation = "${dep.module.group}:${dep.module.name}"
    println("substitute: $notation => $module")
    substitute(module(notation)).using(project(module))
}

fun Project.substitute(
    block: ResolutionStrategy.() -> Unit
) {
    subprojects {
        configurations.matching {
            it.name.endsWith("Classpath", ignoreCase = true) &&
                    !it.name.contains("kotlinCompiler", ignoreCase = true)
        }.configureEach {
            println("Configuring $name | isCanBeResolved: $isCanBeResolved")
            if (true) {
                resolutionStrategy {
                    block()
                }
            }
        }
    }
}
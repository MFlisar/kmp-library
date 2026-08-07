package com.michaelflisar.kmpdevtools

import com.michaelflisar.kmpdevtools.configs.AppModuleConfig
import com.michaelflisar.kmpdevtools.configs.DesktopAppConfig
import edu.sc.seis.launch4j.tasks.Launch4jLibraryTask
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Sync
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Launch4J {

    sealed class Config {

        abstract val outputDirName: String
        abstract val jarTask: String

        data class Thin(
            override val outputDirName: String = "launch4j-thin",
            override val jarTask: String = "proguardReleaseJars",
            val jarFolder: String = "build/compose/tmp/main-release/proguard",
            val mainJarFileName: String = "app-jvm.jar",
        ) : Config()

        data class Fat(
            override val outputDirName: String = "launch4j-fat",
            override val jarTask: String = "flattenReleaseJars",
        ) : Config()
    }

    data class OutputConfig(
        val directory: File? = null,
        val fileName: String? = null,
    )

    internal fun registerTask(
        appModuleConfig: AppModuleConfig,
        desktopAppConfig: DesktopAppConfig,
        config: Config,
        baseTaskName: String = "launch4j",
        outputConfig: OutputConfig = OutputConfig(),
    ) {

        val project = appModuleConfig.project

        val taskName =
            "$baseTaskName${config::class.simpleName}"

        val outputFileName =
            outputConfig.fileName
                ?: "${appModuleConfig.appConfig.name}.exe"

        val launch4jFolder =
            project.layout.buildDirectory.dir(config.outputDirName)

        val outputDirectory =
            outputConfig.directory
                ?: launch4jFolder.get().asFile

        val launch4jTask = project.tasks.register(
            "${taskName}Generate",
            Launch4jLibraryTask::class.java
        ) {

            outputDir.set(config.outputDirName)

            dependsOn(config.jarTask)

            when (config) {

                is Config.Thin -> {

                    jarFiles.set(
                        project.files(
                            project.file(
                                "${config.jarFolder}/${config.mainJarFileName}"
                            )
                        )
                    )

                    classpath.set(
                        project.fileTree(config.jarFolder) {
                            include("*.jar")
                            exclude(config.mainJarFileName)
                        }
                            .files
                            .map { "lib/${it.name}" }
                            .toSet()
                    )
                }

                is Config.Fat -> {

                    setJarTask(
                        project.tasks.getByName(config.jarTask)
                    )
                }
            }

            // setupLaunch4J
            val now = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            mainClassName.set(desktopAppConfig.mainClass)
            icon.set(project.file(desktopAppConfig.ico).absolutePath)
            outfile.set("${appModuleConfig.appConfig.name}.exe")
            productName.set(appModuleConfig.appConfig.name)
            version.set(appModuleConfig.appConfig.versionName)
            textVersion.set(appModuleConfig.appConfig.versionName)
            description = "${appModuleConfig.appConfig.name} - Build at ${now.format(formatter)}"
            copyright.set("©${now.year} ${appModuleConfig.config.developer.name}. All rights reserved.")
            companyName.set(appModuleConfig.config.developer.name)

            outfile.set(outputFileName)
        }

        val previousTask = when (config) {

            is Config.Thin -> {

                project.tasks.register(
                    "${taskName}CopyJars",
                    Copy::class.java
                ) {

                    dependsOn(launch4jTask)

                    from(project.file(config.jarFolder)) {
                        include("*.jar")
                    }

                    into(
                        project.layout.buildDirectory.dir(
                            "${config.outputDirName}/lib"
                        )
                    )
                }
            }

            is Config.Fat -> {
                launch4jTask
            }
        }

        val relocateTask = project.tasks.register(
            "${taskName}Relocate",
            Sync::class.java
        ) {

            dependsOn(previousTask)

            from(launch4jFolder)
            into(outputDirectory)

            doLast {

                val outputFile = File(outputDirectory, outputFileName)

                logger.lifecycle("Executable wurde in folgendem Ordner erstellt:")
                logger.lifecycle(
                    "file:///{}",
                    outputFile.parentFile.absolutePath
                        .replace(" ", "%20")
                        .replace("\\", "/")
                )
            }
        }

        project.tasks.register(taskName) {
            group = "distribution"
            description = "Creates ${config.javaClass.simpleName} Launch4J executable"
            dependsOn(relocateTask)
        }
    }
}
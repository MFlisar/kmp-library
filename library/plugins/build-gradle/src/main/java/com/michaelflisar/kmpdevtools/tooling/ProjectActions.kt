package com.michaelflisar.kmpdevtools.tooling

import com.michaelflisar.kmpdevtools.core.configs.Config
import com.michaelflisar.kmpdevtools.core.configs.LibraryConfig
import com.michaelflisar.kmpdevtools.core.utils.ProjectData
import com.michaelflisar.kmpdevtools.core.utils.ProjectRenamer
import com.michaelflisar.kmpdevtools.core.utils.ScriptStep
import com.michaelflisar.kmpdevtools.core.utils.ScriptUtil
import java.io.File

object ProjectActions {

    fun runProjectRenamer(
        data: ProjectData,
    ) {
        val rootDir = File(System.getProperty("user.dir"))
        val config = Config.readFromProject(rootDir)
        val libraryConfig = LibraryConfig.readFromProject(rootDir)

        val steps = listOf(
            ScriptStep("Rename Package Names") {

                // 1) rename folders
                ProjectRenamer.renameFolder(data = data)

                // 2) rename project content
                ProjectRenamer.renameProject(
                    data = data,
                    renameImports = true,
                    renamePackageNames = true,
                    renameModuleReferences = false,
                )

                // 3) update run configurations
                ProjectRenamer.updateRunConfigurations(data = data)

            },
            ScriptStep("Update iOS App") {

                val folderIOSApp = File(rootDir, "demo/iosApp")
                if (folderIOSApp.exists()) {

                    val fileConfig = File(folderIOSApp, "Configuration/Config.xcconfig")
                    val fileProject = File(folderIOSApp, "iosApp.xcodeproj/project.pbxproj")

                    data.updateFile(fileConfig, replacePackageName = true)
                    data.updateFile(fileProject, replacePackageName = true)

                }
            },
            ScriptStep("Save State") {
                // writes the new package and library names to the state file
                data.updateStateFile()
            }
        )

        ScriptUtil.runScript(
            name = "Rename Package Name",
            steps = steps,
            scriptInfos = {
                ScriptUtil.printDetails(data.asStringMap())
            }
        )
    }
}
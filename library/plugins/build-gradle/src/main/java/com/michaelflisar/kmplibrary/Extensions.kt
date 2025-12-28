package com.michaelflisar.kmplibrary

import com.michaelflisar.kmplibrary.setups.DesktopSetup
import edu.sc.seis.launch4j.tasks.Launch4jLibraryTask
import org.gradle.api.Project
import org.jetbrains.compose.desktop.application.dsl.JvmApplication
import org.jetbrains.compose.desktop.application.dsl.JvmApplicationDistributions
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun JvmApplication.setupWindowsApp(
    project: Project,
    setup: DesktopSetup,
    configNativeDistribution: JvmApplicationDistributions.() -> Unit = {
        targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
    },
) {
    this.mainClass = setup.mainClass

    nativeDistributions {

        configNativeDistribution()

        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        packageName = setup.appName // entspricht dem exe Name
        packageVersion = setup.appVersionName
        description = "${setup.appName} - Build at ${now.format(formatter)}"
        copyright = "©${now.year} ${setup.author}. All rights reserved."
        vendor = setup.author

        // https://github.com/JetBrains/compose-multiplatform/issues/1154
        // => suggestRuntimeModules task ausführen um zu prüfen, was man hier hinzufügen sollte
        // modules("java.instrument", "java.security.jgss", "java.sql", "java.xml.crypto", "jdk.unsupported")

        windows {
            iconFile.set(project.file(setup.ico))
            //includeAllModules = true
        }
    }
}

fun Launch4jLibraryTask.setupLaunch4J(
    setup: DesktopSetup,
    jarTask: String = "flattenReleaseJars",
    outputFile: (exe: File) -> File = { it },
) {
    mainClassName.set(setup.mainClass)
    icon.set(project.file(setup.ico).absolutePath)
    setJarTask(project.tasks.getByName(jarTask))
    outfile.set("${setup.appName}.exe")

    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    productName.set(setup.appName)
    version.set(setup.appVersionName)
    textVersion.set(setup.appVersionName)
    description = "${setup.appName} - Build at ${now.format(formatter)}"
    copyright.set("©${now.year} ${setup.author}. All rights reserved.")
    companyName.set(setup.author)

    doLast {

        val exe = dest.get().asFile

        val finalExe = outputFile(exe)
        if (finalExe != exe) {
            if (finalExe.exists())
                finalExe.delete()
            val moved = exe.renameTo(finalExe)
            if (!moved)
                throw Exception("Konnte exe nicht verschieben!")
        }

        println("")
        println("##############################")
        println("#          LAUNCH4J          #")
        println("##############################")
        println("")
        println("Executable wurde in folgendem Ordner erstellt:")
        println(
            "file:///" + finalExe.parentFile.absolutePath.replace(" ", "%20").replace("\\", "/")
        )
        println("")
    }
}
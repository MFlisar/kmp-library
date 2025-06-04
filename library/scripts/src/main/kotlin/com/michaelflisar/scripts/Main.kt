package com.michaelflisar.scripts

import java.io.File
import java.util.Properties

const val PLACEHOLDER = "com.michaelflisar.example"

fun main() {

    val folder = File(Util.rootFolder())
    val gradleProperties = Util.gradleProperties(folder)

    val libraryName = gradleProperties.getProperty("LIBRARY_NAME") as String
    println("LIBRARY_NAME: $libraryName")

    updateLibraryPackageName(folder, "com.michaelflisar.kotpreferences", "com.michaelflisar.cool.appname")

}

fun updateLibraryPackageName(
    folder: File,
    oldPackageName: String,
    newPackageName: String
) {
    val oldPath = oldPackageName.replace('.', File.separatorChar)
    val newPath = newPackageName.replace('.', File.separatorChar)
    folder
        .walkTopDown()
        .filter { it.isDirectory }
        .onEach { dir ->
            // oldPackageName = com.michaelflisar.example
            // newPackageName = com.michaelflisar.cool.appname
            // falls Ordner ..../com/michaelflisar/example lautet dann solle er
            // zu ..../com/michaelflisar/cool/appname umbenannt werden
            val relative = dir.relativeTo(folder).path
            if (relative.endsWith(oldPath)) {
                val newDir = File(folder, relative.removeSuffix(oldPath) + newPath)
                dir.renameTo(newDir)
                println("Renamed: ${dir.path} -> ${newDir.path}")
            }
        }
}
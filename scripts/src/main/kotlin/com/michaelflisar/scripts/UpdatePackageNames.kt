package com.michaelflisar.scripts

import java.io.File

const val PLACEHOLDER = "com.michaelflisar.example"

fun main() {
    val oldPackageName = PLACEHOLDER
    val newPackageName = "com.michaelflisar.example"
    run(oldPackageName, newPackageName)
}

private fun run(
    oldPackageName: String,
    newPackageName: String
) {
    val folder = File(Util.rootFolder())
    val libraryFolder = File(folder, "library")
    val runConfigFolder = File(folder, ".idea/runConfigurations")
    val gradleProperties = Util.gradleProperties(folder)

    val libraryName = gradleProperties.getProperty("LIBRARY_NAME") as String
    println("LIBRARY_NAME: $libraryName")

    println("Updating package names from '$oldPackageName' to '$newPackageName'? (y/n)")
    val updatePackageName = readln().let { it.trim().lowercase() == "y" }
    if (updatePackageName) {
        updateLibraryPackageName(
            folder = libraryFolder,
            runConfigFolder = runConfigFolder,
            oldPackageName = oldPackageName,
            newPackageName = newPackageName
        )
    } else {
        println("Skipping package name update.")
    }
}

private fun updateLibraryPackageName(
    folder: File,
    runConfigFolder: File,
    oldPackageName: String,
    newPackageName: String
) {
    if (oldPackageName == newPackageName) {
        println("No changes needed, old and new package names are the same: $oldPackageName")
        return
    }

    println("Updating package name from '$oldPackageName' to '$newPackageName' in folder: ${folder.path}")
    val oldPath = oldPackageName.replace('.', File.separatorChar)
    val newPath = newPackageName.replace('.', File.separatorChar)

    // 1) find all folders that belonging to the old package
    val dirsToMove = folder
        .walkTopDown()
        .filter { it.isDirectory }
        .filter {
            val relative = it.relativeTo(folder).path
            !relative.split(File.separatorChar).any { part -> part.startsWith(".") } &&
                    relative.endsWith(oldPath)
        }
        .toList()

    // 2) move files from old package to new package
    for (oldDir in dirsToMove) {
        val relative = oldDir.relativeTo(folder).path
        val newDir = File(folder, relative.removeSuffix(oldPath) + newPath)
        newDir.mkdirs()
        oldDir.listFiles()?.forEach { file ->
            file.copyRecursively(File(newDir, file.name), overwrite = true)
            file.deleteRecursively()
        }
        println("Moved: ${oldDir.path} -> ${newDir.path}")
    }

    // 3) delete old package folders (if they are empty)
    for (oldDir in dirsToMove) {
        var dirToDelete: File? = oldDir
        while (dirToDelete != null && dirToDelete.exists() && dirToDelete.listFiles()
                ?.isEmpty() == true && dirToDelete != folder
        ) {
            val parent = dirToDelete.parentFile
            dirToDelete.delete()
            println("Deleted old dir: ${dirToDelete.path}")
            dirToDelete = parent
        }
    }

    // 4) update imports and package names in all files
    folder.walkTopDown().forEach { file ->
        if (file.isFile && (file.extension == "kt" || file.extension == "kts")) {
            updateFile(file, oldPackageName, newPackageName)
            updateFile(file, oldPath, newPath)
        }
    }

    // 5) update run configurations in IDE
    runConfigFolder.listFiles().forEach {
        updateFile(it, oldPackageName, newPackageName)
    }
    println("Package name update completed.")
}

private fun updateFile(file: File, oldPackageName: String, newPackageName: String) {
    val content = file.readText()
    val updatedContent = content.replace(oldPackageName, newPackageName)
    if (content != updatedContent) {
        file.writeText(updatedContent)
        println("Updated file: $file")
    }
}
package com.michaelflisar.scripts

import java.io.File

const val PLACEHOLDER = "com.michaelflisar.example"

fun main() {
    val oldPackageName = PLACEHOLDER
    val newPackageName = "com.michaelflisar.example.test"
    run(newPackageName, oldPackageName)
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

    //val f = File("D:\\dev2\\kmp-template\\library\\demo\\app\\android\\src\\main\\java\\com\\michaelflisar\\example\\test")
    //val success = f.deleteRecursively()
    //println("Deleted test folder: ${f.path}, success: $success")
    //return

    println("Updating package name from '$oldPackageName' to '$newPackageName' in folder: ${folder.path}")
    val oldPath = oldPackageName.replace('.', File.separatorChar)
    val newPath = newPackageName.replace('.', File.separatorChar)

    // 1) find all folders that belonging to the old package
    val dirsToMove = folder
        .walkTopDown()
        .filter { it.isDirectory }
        .filter {
            val relative = it.relativeTo(folder).path
            relative.endsWith(oldPath) && !relative.split(File.separatorChar).any { part -> part.startsWith(".") }
        }
        .toList()

    println("1... - dirsToMove = ${dirsToMove.size}")

    // 2) move files from old package to new package
    for (oldDir in dirsToMove) {
        val relative = oldDir.relativeTo(folder).path
        val newDir = File(folder, relative.removeSuffix(oldPath) + newPath)
        val filesToMove = oldDir.walkTopDown().toList()
        for (file in filesToMove) {
            val relative = file.relativeTo(oldDir)
            val target = File(newDir, relative.path)
            if (file.isDirectory) {
                target.mkdirs()
            } else {
                // Versuche zu verschieben, sonst kopieren und löschen
                if (!file.renameTo(target)) {
                    file.copyTo(target, overwrite = true)
                    file.delete()
                }
            }
        }
        println("Moved: ${oldDir.path} -> ${newDir.path}")
    }

    // 3) delete old package folders (if they are empty)
    for (oldDir in dirsToMove) {
        var dirToDelete: File? = oldDir
        while (
            dirToDelete != null &&
            dirToDelete.exists() &&
            dirToDelete.walk().none { it.isFile } &&
            dirToDelete != folder &&
            dirToDelete.relativeTo(folder).path.endsWith(oldPath)
        ) {
            val parent = dirToDelete.parentFile
            val success = dirToDelete.deleteRecursively()
            println("Deleted old dir: ${dirToDelete.path} | success: $success")
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
    runConfigFolder.listFiles()?.forEach {
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

fun deleteEmptyDirs(folder: File, oldPath: String) {
    folder.walkBottomUp()
        .filter { it.isDirectory && it.relativeTo(folder).path.endsWith(oldPath) }
        .forEach { dir ->
            if (dir.listFiles()?.all { it.isDirectory.not() || !it.exists() } == true) {
                dir.delete()
                println("Deleted old dir: ${dir.path}")
            }
        }
}
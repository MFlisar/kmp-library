package com.michaelflisar.kmptemplate.scripts

import java.io.File

fun updatePackageNames(
    oldPackageName: String,
    newPackageName: String
) {
    run(oldPackageName, newPackageName)
}

private fun run(
    oldPackageName: String,
    newPackageName: String
) {
    val folder = rootFolder()
    val libraryFolder = File(folder, "library")
    val runConfigFolder = File(folder, ".idea/runConfigurations")

    val libraryName = folder.name.lowercase()
    println("LIBRARY_NAME: $libraryName")

    println("Updating package names from '$oldPackageName' to '$newPackageName'? (y/n)")
    updateLibraryPackageName(
        folder = libraryFolder,
        runConfigFolder = runConfigFolder,
        oldPackageName = oldPackageName,
        newPackageName = newPackageName
    )
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
        .walkTopDownFiltered {
            if (!it.isDirectory)
                return@walkTopDownFiltered false
            val relative = it.relativeTo(folder).path
            relative.endsWith(oldPath) && !relative.split(File.separatorChar)
                .any { part -> part.startsWith(".") }
        }
        .toList()

    println("1... - dirsToMove = ${dirsToMove.size}")

    // 2) move files from old package to new package
    for (oldDir in dirsToMove) {
        val relative = oldDir.relativeTo(folder).path
        val newDir = File(folder, relative.removeSuffix(oldPath) + newPath)
        val filesToMove = oldDir.walkTopDown().toList()
        for (file in filesToMove) {
            val relative2 = file.relativeTo(oldDir)
            val target = File(newDir, relative2.path)
            if (file.isDirectory) {
                target.mkdirs()
            } else {
                // try to rename the file, if it fails (e.g. because a file with the same name exists) overwrite it
                if (!file.renameTo(target)) {
                    file.copyTo(target, overwrite = true)
                    file.delete()
                }
            }
        }
        println("Moved: ${oldDir.path} -> ${newDir.path}")
    }

    // 3) delete old package folders (if they are empty)
    deleteEmptyDirs(folder)

    // 4) update imports and package names in all files
    folder
        .walkTopDownFiltered { file -> file.isFile && (file.extension == "kt" || file.extension == "kts") }
        .forEach { file ->
            file.update(oldPackageName, newPackageName)
            file.update(oldPath, newPath)
        }

    // 5) update run configurations in IDE
    runConfigFolder.listFiles()?.forEach {
        it.update(oldPackageName, newPackageName)
    }
    println("Package name update completed.")
}


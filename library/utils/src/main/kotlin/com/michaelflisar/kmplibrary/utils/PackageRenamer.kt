package com.michaelflisar.kmplibrary.utils

import java.io.File

object PackageRenamer {

    /**
     * Renames a package from packageNameFrom to packageNameTo within the given root directory.
     * This includes renaming folder structures as well as updating import statements and package declarations in Kotlin files.
     */
    fun rename(
        root: File,
        packageNameFrom: String,
        packageNameTo: String
    ) : Boolean {

        // Convert package names to path format
        val fromPath = packageNameFrom.replace('.', File.separatorChar)
        val toPath = packageNameTo.replace('.', File.separatorChar)

        // 1) rename all folders
        renameFolders(root, fromPath, toPath)

        // 2) rename imports/packagenames/references in all files
        renameImports(root, packageNameFrom, packageNameTo)
        renamePackageNames(root, packageNameFrom, packageNameTo)

        return true
    }

    /**
     * Renames all package folders recursively, regardless of their depth in the directory tree.
     * If the target directory already exists, moves all content from the source to the target (overwriting files if needed).
     * deletes empty fromPaths afterwards
     */
    fun renameFolders(
        root: File,
        fromPath: String,
        toPath: String
    ) {
        val dirsToRename = FolderUtil.findFoldersWithPath(root, fromPath)
        println("renameFolders => dirsToRename: "+dirsToRename.size)
        for (dir in dirsToRename) {
            val relative = dir.relativeTo(root).path
            val newRelative = relative.replace(fromPath, toPath)
            val newDir = File(root, newRelative)
            if (!newDir.exists()) {
                // Create parent directories if needed
                newDir.parentFile?.mkdirs()
                dir.renameTo(newDir)
                println("renameFolders => renamed $dir => $newDir")
            } else {
                // Move all content from dir to newDir, overwriting existing files
                FolderUtil.moveDirectoryContent(dir, newDir)
                // Optionally delete dir if empty
                if (dir.listFiles()?.isEmpty() == true) {
                    dir.delete()
                }
                println("renameFolders => merged $dir => $newDir")
            }
        }
    }

    /**
     * Replaces all import statements in Kotlin files from the old package name to the new package name.
     * Only .kt files are processed, recursively from the given root directory.
     */
    fun renameImports(
        root: File,
        packageNameFrom: String,
        packageNameTo: String
    ) {
        // Prepare the import statement prefix
        val importFrom = "import $packageNameFrom"
        val importTo = "import $packageNameTo"

        // Recursively process all .kt files
        var changedFiles = 0
        root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val originalText = file.readText()
                val newText = originalText.replace(importFrom, importTo)
                if (originalText != newText) {
                    file.writeText(newText)
                    changedFiles++
                }
            }

        println("renameImports => changedFiles: $changedFiles")
    }

    /**
     * Replaces all package statements in Kotlin files from the old package name to the new package name.
     * Only .kt(s) files are processed, recursively from the given root directory.
     */
    fun renamePackageNames(
        root: File,
        packageNameFrom: String,
        packageNameTo: String
    ) {
        // Prepare the import statement prefix
        val packageFrom = "package $packageNameFrom"
        val packageTo = "package $packageNameTo"

        // Recursively process all .kt files
        var changedFiles = 0
        root.walkTopDown()
            .filter { it.isFile && it.extension in listOf("kt", "kts") }
            .forEach { file ->
                val originalText = file.readText()
                val newText = originalText.replace(packageFrom, packageTo)
                if (originalText != newText) {
                    file.writeText(newText)
                    changedFiles++
                }
            }

        println("renameImports => changedFiles: $changedFiles")
    }
}
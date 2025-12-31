package com.michaelflisar.kmplibrary.core.utils

import java.io.File

object PackageRenamer {

    /**
     * Renames a package from packageNameFrom to packageNameTo within the given root directory.
     * This includes renaming folder structures as well as updating import statements and package declarations in Kotlin files.
     */
    fun rename(
        root: File,
        packageNameFrom: String,
        packageNameTo: String,
        libraryNameFrom: String,
        libraryNameTo: String,
        log: Boolean = true,
    ): Boolean {

        // Convert package names to path format
        val fromPath = packageNameFrom.replace('.', File.separatorChar)
        val toPath = packageNameTo.replace('.', File.separatorChar)

        // 1) rename all folders
        renameFolders(root, fromPath, toPath, log)

        // 2) rename imports/packagenames/references in all files
        renameLibraryIdInRootSettingsGradleKts(root, libraryNameFrom, libraryNameTo, log)
        renameImports(root, packageNameFrom, packageNameTo, log)
        renamePackageNames(root, packageNameFrom, packageNameTo, log)
        renameModuleReferences(root, libraryNameFrom, libraryNameTo, log)

        return true
    }

    /**
     * Renames all package folders recursively, regardless of their depth in the directory tree.
     * If the target directory already exists, moves all content from the source to the target (overwriting files if needed).
     * deletes empty fromPaths afterwards
     */
    private fun renameFolders(
        root: File,
        fromPath: String,
        toPath: String,
        log: Boolean,
    ) {
        val dirsToRename = FolderUtil.findFoldersWithPath(root, fromPath)
        if (log)
            println("renameFolders => dirsToRename: " + dirsToRename.size)
        for (dir in dirsToRename) {
            val relative = dir.relativeTo(root).path
            val newRelative = relative.replace(fromPath, toPath)
            val newDir = File(root, newRelative)
            if (log)
                print("renaming folder: $relative => $newRelative => result = ")
            if (dir != newDir) {
                if (!newDir.exists()) {
                    // Create parent directories if needed
                    newDir.parentFile?.mkdirs()
                    dir.renameTo(newDir)
                    if (log)
                        print("RENAMED")
                } else {
                    // Move all content from dir to newDir, overwriting existing files
                    FolderUtil.moveDirectoryContent(dir, newDir)
                    // Optionally delete dir if empty
                    if (dir.listFiles()?.isEmpty() == true) {
                        dir.delete()
                    }
                    if (log)
                        print("MERGED")
                }
            } else {
                if (log)
                    print("SKIPPED (same dir)")
            }
            if (log)
                print("\n")
        }
    }

    /**
     * Replaces all import statements in Kotlin files from the old package name to the new package name.
     * Only .kt files are processed, recursively from the given root directory.
     */
    private fun renameImports(
        root: File,
        packageNameFrom: String,
        packageNameTo: String,
        log: Boolean,
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
        if (log)
            println("renameImports => changedFiles: $changedFiles")
    }

    /**
     * Replaces all package statements in Kotlin files from the old package name to the new package name.
     * Only .kt(s) files are processed, recursively from the given root directory.
     */
    private fun renamePackageNames(
        root: File,
        packageNameFrom: String,
        packageNameTo: String,
        log: Boolean,
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

        if (log)
            println("renameImports => changedFiles: $changedFiles")
    }

    /**
     * Replaces all references to the old package name with the new package name in Kotlin and Gradle files.
     * Processes gradle.kts files only
     *
     * following is replaced e.g.:
     *
     * project(":librarytemplate:...)
     *
     */
    private fun renameModuleReferences(
        root: File,
        libraryNameFrom: String,
        libraryNameTo: String,
        log: Boolean,
    ) {
        // Recursively process all .kt(s) and .gradle.kts files
        var changedFiles = 0
        root.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".gradle.kts") }
            .forEach { file ->
                val originalText = file.readText()
                val newText = originalText.replace("project(\":${libraryNameFrom.lowercase()}:", "project(\":${libraryNameTo.lowercase()}:")
                if (originalText != newText) {
                    file.writeText(newText)
                    changedFiles++
                }
            }

        if (log)
            println("renameModuleReferences => changedFiles: $changedFiles")
    }

    /**
     * Replaces libraryId in root settings.gradle.kts
     *
     * val libraryId = "librarytemplate"
     */
    private fun renameLibraryIdInRootSettingsGradleKts(
        root: File,
        libraryNameFrom: String,
        libraryNameTo: String,
        log: Boolean,
    ) {
        val buildGradleKtsFile = File(root, "settings.gradle.kts")
        if (buildGradleKtsFile.exists()) {
            val originalText = buildGradleKtsFile.readText()
            val newText = originalText.replace(libraryNameFrom.lowercase(), libraryNameTo.lowercase())
            if (originalText != newText) {
                buildGradleKtsFile.writeText(newText)
                if (log)
                    println("renameLibraryIdInRootSettingsGradleKts => updated build.gradle.kts")
            } else {
                if (log)
                    println("renameLibraryIdInRootSettingsGradleKts => no changes needed")
            }
        }
    }
}
package com.michaelflisar.kmpdevtools.core.utils

import java.io.File

object FolderUtil {

    /**
     *  all directories whose relative path contains the fromPath
     */
    fun findFoldersWithPath(
        root: File,
        pathToContain: String,
    ): List<File> {
        val dirsToRename = root.walkTopDown()
            .filter { it.isDirectory && it.relativeToOrNull(root)?.path?.contains(pathToContain) == true }
            .toList()
            // Sort by path length ascending, damit Eltern zuerst kommen
            .sortedBy { it.relativeTo(root).path.length }
        // Nur Top-Level-Matches behalten (keine Subfolder von anderen Matches)
        val result = mutableListOf<File>()
        for (dir in dirsToRename) {
            if (result.none { parent -> dir.toPath().startsWith(parent.toPath()) && dir != parent }) {
                result.add(dir)
            }
        }
        return result
    }

    fun isFolderEmptyOrOnlyContainsEmptyFolders(folder: File): Boolean {
        if (!folder.isDirectory) return false
        val files = folder.listFiles() ?: return true
        for (file in files) {
            if (file.isFile) return false
            if (file.isDirectory && !isFolderEmptyOrOnlyContainsEmptyFolders(file)) return false
        }
        return true
    }

    /**
     * Moves all files and subdirectories from sourceDir to targetDir. Overwrites files if they exist.
     */
    fun moveDirectoryContent(sourceDir: File, targetDir: File) {
        sourceDir.listFiles()?.forEach { file ->
            val targetFile = File(targetDir, file.name)
            if (file.isDirectory) {
                if (!targetFile.exists()) targetFile.mkdirs()
                moveDirectoryContent(file, targetFile)
                // Delete source subdirectory if empty
                if (file.listFiles()?.isEmpty() == true) {
                    file.delete()
                }
            } else {
                // Overwrite file if exists
                file.copyTo(targetFile, overwrite = true)
                file.delete()
            }
        }
    }

}
package com.michaelflisar.kmptemplate.scripts

import java.io.File

internal fun rootFolder() = File(System.getProperty("user.dir"))

internal fun File.update(oldString: String, newString: String) {
    val content = readText()
    val updatedContent = content.replace(oldString, newString)
    if (content != updatedContent) {
        writeText(updatedContent)
        println("Updated file: $this")
    }
}

internal fun deleteEmptyDirs(folder: File) {
    folder.walkBottomUp()
        .filter { it.isDirectory && it.listFiles()?.isEmpty() == true }
        .forEach { it.delete() }
}

internal fun File.walkTopDownFiltered(filter: (File) -> Boolean = { true }): Sequence<File> {
    return this.walkTopDown().filter(filter)
}

internal fun File.saveDeleteRecursively() {
    if (exists()) {
        deleteRecursively()
    }
}

internal fun File.saveDelete() {
    if (exists()) {
        delete()
    }
}

internal fun File.deleteIfEmpty() {
    if (isDirectory && listFiles()?.isEmpty() == true) {
        delete()
    }
}
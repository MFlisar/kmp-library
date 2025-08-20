package com.michaelflisar.kmplibrary

import java.io.File

fun rootFolder() = File(System.getProperty("user.dir"))

fun File.update(oldString: String, newString: String) {
    val content = readText()
    val updatedContent = content.replace(oldString, newString)
    if (content != updatedContent) {
        writeText(updatedContent)
        println("Updated file: $this")
    }
}

fun deleteEmptyDirs(folder: File) {
    folder.walkBottomUp()
        .filter { it.isDirectory && it.listFiles()?.isEmpty() == true }
        .forEach { it.delete() }
}

fun File.walkTopDownFiltered(filter: (File) -> Boolean = { true }): Sequence<File> {
    return this.walkTopDown().filter(filter)
}

fun File.saveDeleteRecursively() {
    if (exists()) {
        deleteRecursively()
    }
}

fun File.saveDelete() {
    if (exists()) {
        delete()
    }
}

fun File.deleteIfEmpty() {
    if (isDirectory && listFiles()?.isEmpty() == true) {
        delete()
    }
}
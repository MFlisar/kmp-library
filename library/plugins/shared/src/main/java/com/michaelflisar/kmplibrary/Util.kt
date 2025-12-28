package com.michaelflisar.kmplibrary

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.DeserializationStrategy
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

fun <T> Yaml.Companion.tryRead(file: File, deserializer: DeserializationStrategy<T>): T? {
    return try {
        val content = file.readText(Charsets.UTF_8)
        Yaml.default.decodeFromString(deserializer, content)
    } catch (e: Exception) {
        null
    }
}

inline fun <reified T> Yaml.Companion.read(file: File, deserializer: DeserializationStrategy<T>): T {
    return try {
        val content = file.readText(Charsets.UTF_8)
        Yaml.default.decodeFromString(deserializer, content)
    } catch (e: Exception) {
        e.printStackTrace()
        throw RuntimeException("Failed to read `${T::class.simpleName}` from path '${file.path}'", e)
    }
}
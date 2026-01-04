package com.michaelflisar.kmpdevtools.core

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.DeserializationStrategy
import java.io.File

fun <T> Yaml.Companion.tryRead(file: File, deserializer: DeserializationStrategy<T>): T? {
    return try {
        val content = file.readText(Charsets.UTF_8)
        Yaml.default.decodeFromString(deserializer, content)
    } catch (e: Exception) {
        null
    }
}

inline fun <reified T> Yaml.Companion.read(
    file: File,
    deserializer: DeserializationStrategy<T>,
): T {
    return try {
        val content = file.readText(Charsets.UTF_8)
        Yaml.default.decodeFromString(deserializer, content)
    } catch (e: Exception) {
        e.printStackTrace()
        throw RuntimeException(
            "Failed to read `${T::class.simpleName}` from path '${file.path}'",
            e
        )
    }
}
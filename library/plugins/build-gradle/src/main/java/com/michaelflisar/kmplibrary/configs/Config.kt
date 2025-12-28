package com.michaelflisar.kmplibrary.configs

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.gradle.api.Project
import java.io.File
import kotlin.io.readText

@Serializable
data class Config(
    @SerialName("java-version") val javaVersion: String
) {
    companion object {

        fun read(project: Project, relativePath: String): Config {
            val file = File(project.projectDir, relativePath)
            return read(file)
        }

        fun read(file: File): Config {
            return try {
                tryRead(file)!!
            } catch (e: Exception) {
                e.printStackTrace()
                throw kotlin.RuntimeException("Failed to read `Config` from path '${file.path}'", e)
            }
        }

        fun tryRead(file: File): Config? {
            if (!file.exists()) {
                return null
            }
            val content = file.readText(Charsets.UTF_8)
            return Yaml.default.decodeFromString(serializer(), content)
        }
    }
}


package com.michaelflisar.kmpgradletools

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class SetupApp(
    @SerialName("java-version") val javaVersion: String
) {
    companion object {

        private const val YML_FILE = ".kmp-gradle-tools/app.yml"

        fun file(root: File): File {
            return File(root, YML_FILE)
        }

        fun tryRead(root: File): SetupApp? {
            val file = file(root)
            return try {
                val content = file.readText(Charsets.UTF_8)
                Yaml.default.decodeFromString(serializer(), content)
            } catch (e: Exception) {
                null
            }
        }
    }
}


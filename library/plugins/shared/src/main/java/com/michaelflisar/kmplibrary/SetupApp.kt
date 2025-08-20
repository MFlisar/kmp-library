package com.michaelflisar.kmplibrary

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class SetupApp(
    @SerialName("java-version") val javaVersion: String
) {
    companion object {

        fun file(root: File): File {
            return File(root, Constants.YML_APP)
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


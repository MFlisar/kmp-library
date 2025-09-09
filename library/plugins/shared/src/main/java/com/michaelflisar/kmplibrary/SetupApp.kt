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

        fun read(root: File) = Yaml.read(file(root), serializer())
        fun tryRead(root: File) = Yaml.tryRead(file(root), serializer())
    }
}


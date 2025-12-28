package com.michaelflisar.kmplibrary.configs

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import kotlin.io.readText

@Serializable
data class LibraryConfig(
    val developer: Developer,
    val library: Library,
    val maven: Maven,
) {
    companion object {

        fun read(file: File): LibraryConfig {
            return try {
                tryRead(file)!!
            } catch (e: Exception) {
                e.printStackTrace()
                throw kotlin.RuntimeException(
                    "Failed to read `LibraryConfig` from path '${file.path}'",
                    e
                )
            }
        }

        fun tryRead(file: File): LibraryConfig? {
            if (!file.exists()) {
                return null
            }
            val content = file.readText(Charsets.UTF_8)
            return Yaml.default.decodeFromString(serializer(), content)
        }
    }

    @Serializable
    class Developer(
        val name: String,
        val mail: String,
        @SerialName("maven-id") val mavenId: String,
        @SerialName("social-github") val socialGithub: String,
    )

    @Serializable
    class Library(
        val name: String,
        val release: Int,
        @SerialName("link-repo") val linkRepo: String,
        val license: License
    ) {
        @Serializable
        class License(
            val name: String,
            val link: String,
        )
    }

    @Serializable
    class Maven(
        @SerialName("group-id") val groupId: String,
        @SerialName("primary-artifact-id") val primaryArtifactId: String,
    )
}


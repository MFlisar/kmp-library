package com.michaelflisar.kmplibrary

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class SetupLibrary(
    val developer: Developer,
    @SerialName("java-version") val javaVersion: String,
    val library: Library,
    val maven: Maven,
) {
    companion object {

        fun file(root: File): File {
            return File(root, Constants.YML_LIBRARY)
        }

        fun read(root: File): SetupLibrary {
            val file = file(root)
            return try {
                tryRead(root)!!
            } catch (e: Exception) {
                e.printStackTrace()
                throw RuntimeException("Failed to read `SetupLibrary` from path '${file.path}'", e)
            }
        }

        fun tryRead(root: File): SetupLibrary? {
            val file = file(root)
            return try {
                val content = file.readText(Charsets.UTF_8)
                Yaml.default.decodeFromString(serializer(), content)
            } catch (e: Exception) {
                null
            }
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
        @SerialName("link-docs") val linkDocs: String,
        @SerialName("link-repo") val linkRepo: String,
        @SerialName("repo-name") val repoName: String,
        val license: License,
        val screenshots: List<Screenshot>,
        @SerialName("about-me") val aboutMe: Boolean,
    ) {
        val id = name.lowercase().split(" ")
            .mapIndexed { index, word -> if (index == 0) word else word.replaceFirstChar { it.uppercase() } }
            .joinToString("")

        @Serializable
        class License(
            val name: String,
            val link: String,
        )

        @Serializable
        class Screenshot(
            val name: String,
            val images: List<String>
        )
    }

    @Serializable
    class Maven(
        @SerialName("group-id") val groupId: String,
        @SerialName("primary-artifact-id") val primaryArtifactId: String,
    )
}


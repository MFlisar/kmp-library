package com.michaelflisar.kmplibrary.configs

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.gradle.api.Project
import java.io.File
import kotlin.io.readText

@Serializable
data class LibraryConfig(
    val developer: Developer,
    val library: Library,
    val maven: Maven,
    val modules: List<Module>
) {
    companion object {

        fun read(project: Project, relativePath: String): Config {
            val file = File(project.projectDir, relativePath)
            return Config.Companion.read(file)
        }

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

    fun getModuleByPath(path: String): Module {
        return modules.find { it.path.replace("\\", "/") == path.replace("\\", "/") }
            ?: throw RuntimeException("module setup definition not found for path: $path => make sure to define it inside library config yml file")
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

    @Serializable
    class Module(
        val name: String,
        @SerialName("artifact-id") val artifactId: String,
        val description: String,
        val path: String
    ) {
        fun libraryDescription(setup: LibraryConfig): String {
            val library = setup.library.name
            return "$library - $artifactId module - $description"
        }
    }
}


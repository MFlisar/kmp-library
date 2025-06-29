package com.michaelflisar.kmpgradletools

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import java.io.File

@Serializable
data class SetupModules(
    val groups: List<Group>? = null,
    val modules: List<Module>,
) {
    companion object {

        private val YML_FILE = ".kmp-gradle-tools/modules.yml"

        fun file(root: File): File {
            return File(root, YML_FILE)
        }

        fun read(root: File): SetupModules {
            val file = file(root)
            return try {
                val content = file.readText(Charsets.UTF_8)
                Yaml.default.decodeFromString(serializer(), content)
            } catch (e: Exception) {
                e.printStackTrace()
                throw RuntimeException("Failed to read `SetupModules` from path '${file.path}'", e)
            }
        }
    }

    fun getModuleByPath(path: String): Module {
        return modules.find { it.relativePath.replace("\\", "/") == path.replace("\\", "/") }
            ?: throw RuntimeException("module setup definition not found for path: $path => make sure to define it inside `$YML_FILE`")
    }

    @Serializable
    class Group(
        val id: String,
        val label: String,
        @SerialName("gradle-comment") val gradleComment: String,
    )

    @Serializable
    class Module(
        val relativePath: String,
        @SerialName("artifact-id") val artifactId: String,
        val group: String? = null,
        val description: String,
        val optional: Boolean,
        @SerialName("platforms-info") val platformInfo: String?,
        val dependencies: List<Dependency>?,
    ) {
        fun libraryDescription(setup: SetupLibrary): String {
            val library = setup.library.name
            return "$library - $artifactId module - $description"
        }

        @Serializable
        class Dependency(
            val name: String,
            @SerialName("versions-file") val versionsFile: String,
            @SerialName("versions-key") val versionsKey: String,
            val link: String,
        )
    }
}
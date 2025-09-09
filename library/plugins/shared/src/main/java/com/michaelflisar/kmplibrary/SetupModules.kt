package com.michaelflisar.kmplibrary

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class SetupModules(
    val groups: List<Group>? = null,
    val modules: List<Module>,
) {
    companion object {

        fun file(root: File): File {
            return File(root, Constants.YML_MODULES)
        }

        fun read(root: File) = Yaml.read(file(root), serializer())
        fun tryRead(root: File) = Yaml.tryRead(file(root), serializer())
    }

    fun getModuleByPath(path: String): Module {
        return modules.find { it.relativePath.replace("\\", "/") == path.replace("\\", "/") }
            ?: throw RuntimeException("module setup definition not found for path: $path => make sure to define it inside `${Constants.YML_MODULES}`")
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
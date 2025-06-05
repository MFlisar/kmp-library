package com.michaelflisar.buildlogic.shared

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlNull
import com.charleskorn.kaml.YamlScalar
import com.charleskorn.kaml.YamlTaggedNode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

class SetupData(
    val setup: Setup,
    val yaml: List<YamlValue>
) {
    companion object {

        fun read(root: File): SetupData {
            val setupFile = File(root, "generator/setup.yml")
            return try {
                val content = setupFile.readText(Charsets.UTF_8)
                val setup = Yaml.default.decodeFromString(Setup.serializer(), content)
                val yaml = Yaml.default.parseToYamlNode(content)
                val values = collectYamlValues(yaml)
                SetupData(setup, values)
            } catch (e: Exception) {
                e.printStackTrace()
                throw RuntimeException("Failed to read setup file: ${setupFile.path}", e)
            }
        }

        private fun collectYamlValues(
            node: YamlNode,
            result: MutableList<YamlValue> = mutableListOf()
        ): List<YamlValue> {
            when (node) {
                is YamlMap -> {
                    for (value in node.entries.values) {
                        collectYamlValues(value, result)
                    }
                }

                is YamlList -> {
                    val path = node.path.toHumanReadableString()
                    val value = node.items.joinToString(", ") { it.toString() }
                    result.add(YamlValue(value, path))
                }

                is YamlScalar -> {
                    val path = node.path.toHumanReadableString()
                    val value = node.content
                    result.add(YamlValue(value, path))
                }

                is YamlNull -> {}
                is YamlTaggedNode -> {}
            }
            return result
        }
    }

    fun getYamlValue(path: String): YamlValue {
        return yaml.firstOrNull { it.path == path }!!
    }

    class YamlValue(
        val value: String,
        val path: String
    ) {
        override fun toString(): String {
            return "YamlValue(value='$value', path='$path')"
        }
    }
}

@Serializable
class Setup(
    val developer: Developer,
    @SerialName("java-version") val javaVersion: String,
    val library: Library,
    val maven: Maven,
    val groups: List<Group>? = null,
    val modules: List<Module>,
    @SerialName("other-projects") val otherProjects: List<OtherProjectGroup>?
) {
    fun getModuleByPath(path: String): Module {
        return modules.find { it.relativePath == path }
            ?: throw RuntimeException("module setup definition not found for path: $path")
    }

    @Serializable
    class Developer(
        val id: String,
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
        val screenshots: List<String>
    ) {
        @Serializable
        class License(
            val name: String,
            val link: String
        )
    }

    @Serializable
    class Maven(
        @SerialName("group-id") val groupId: String,
        @SerialName("primary-artifact-id") val primaryArtifactId: String
    )

    @Serializable
    class Group(
        val id: String,
        val label: String,
        @SerialName("gradle-comment") val gradleComment: String
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
        fun libraryDescription(setup: Setup): String {
            val library = setup.library.name
            return "$library - $artifactId module - $description"
        }

        @Serializable
        class Dependency(
            val name: String,
            @SerialName("versions-file") val versionsFile: String,
            @SerialName("versions-key") val versionsKey: String,
            val link: String
        )
    }

    @Serializable
    class OtherProjectGroup(
        val group: String,
        val projects: List<OtherProject>
    ) {
        @Serializable
        class OtherProject(
            val name: String,
            val link: String,
            val image: String? = null,
            val maven: String,
            val description: String,
        )
    }
}
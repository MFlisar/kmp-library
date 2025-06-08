package com.michaelflisar.kmptemplate

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
    companion object {

        val RELATIVE_YAML_FILE_PATH = "documentation/setup.yml"

        fun read(root: File): Setup {
            val file = File(root, RELATIVE_YAML_FILE_PATH)
            return try {
                val content = file.readText(Charsets.UTF_8)
                Yaml.default.decodeFromString(serializer(), content)
            } catch (e: Exception) {
                e.printStackTrace()
                throw RuntimeException("Failed to read setup from path '${file.path}'", e)
            }
        }

        fun readYaml(root: File): List<YamlValue> {
            val file = File(root, RELATIVE_YAML_FILE_PATH)
            return try {
                val content = file.readText(Charsets.UTF_8)
                val yaml = Yaml.default.parseToYamlNode(content)
                collectYamlValues(yaml)
            } catch (e: Exception) {
                e.printStackTrace()
                throw RuntimeException("Failed to read setup from path '${file.path}'", e)
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
    fun getModuleByPath(path: String): Module {
        return modules.find { it.relativePath.replace("\\", "/") == path.replace("\\", "/") }
            ?: throw RuntimeException("module setup definition not found for path: $path")
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
        val screenshots: List<String>
    ) {
        val id = name.lowercase().split(" ")
            .mapIndexed { index, word -> if (index == 0) word else word.replaceFirstChar { it.uppercase() } }
            .joinToString("")

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

    class YamlValue(
        val value: String,
        val path: String
    )
}
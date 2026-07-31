package com.michaelflisar.kmpdevtools.core.configs

import com.michaelflisar.kmpdevtools.core.ConfigDefaults
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Config(
    @SerialName("java-version") val javaVersion: String,
    val developer: Developer = Developer(),
    val project: Project,
    val readme: Readme = Readme(),
    val settings: Settings = Settings()
) {

    companion object : ConfigReader<Config>(
        ConfigDefaults.FILE_CONFIG,
        { Config.serializer() }
    )

    /**
     * default values:
     *
     * just to avoid the necessity to define them in the config file,
     * if  a project is not published at all
     */
    @Serializable
    class Developer(
        val name: String = "<UNKNOWN>",
        val mail: String = "<NONE>",
        @SerialName("maven-id") val mavenId: String = "<NONE>",
        @SerialName("github-user-name") val githubUserName: String = "<NONE>",
    )

    @Serializable
    class Project(
        val namespace: String,
    )

    /*
     * readme:
        screenshots:
          excludeRoot: true
           excludeFolders: [previews]
           excludeImages: []
     */
    @Serializable
    class Readme(
        val screenshots: Screenshots = Screenshots(),
    ) {
        @Serializable
        class Screenshots(
            @SerialName("exclude-root") val excludeRoot: Boolean = true,
            @SerialName("group-by-folder") val groupByFolders: Boolean = true,
            @SerialName("excluded-folders") val excludedFolders: List<String> = emptyList(),
            @SerialName("excluded-images") val excludedImages: List<String> = emptyList(),
        )
    }

    @Serializable
    class Settings(
        @SerialName("log-dependencies") val logDependencies: Boolean = false
    )
}
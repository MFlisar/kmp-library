package com.michaelflisar.kmpgradletools

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class SetupOtherProjects(
    @SerialName("other-projects") val otherProjects: List<OtherProjectGroup>,
) {
    companion object {

        fun read(
            load: () -> String,
            customOtherProjectsYamlUrl: String,
        ): SetupOtherProjects {
            return try {
                val content = load()
                Yaml.default.decodeFromString(serializer(), content)
            } catch (e: Exception) {
                e.printStackTrace()
                throw RuntimeException(
                    "Failed to read `SetupOtherProjects` from path '$customOtherProjectsYamlUrl'",
                    e
                )
            }
        }
    }

    @Serializable
    class OtherProjectGroup(
        val group: String,
        val projects: List<OtherProject>,
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
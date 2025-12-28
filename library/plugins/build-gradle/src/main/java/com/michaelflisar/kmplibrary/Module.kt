package com.michaelflisar.kmplibrary

import com.michaelflisar.kmplibrary.configs.LibraryConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Module(
    @SerialName("artifact-id") val artifactId: String,
    val description: String,
) {
    fun libraryDescription(setup: LibraryConfig): String {
        val library = setup.library.name
        return "$library - $artifactId module - $description"
    }
}
package com.michaelflisar.buildlogic.classes

import org.gradle.api.Project

// Developer
val DEVELOPER_ID = ProjectProperty("DEVELOPER_ID")
val DEVELOPER_NAME = ProjectProperty("DEVELOPER_NAME")
val DEVELOPER_EMAIL = ProjectProperty("DEVELOPER_EMAIL")

// Versions
val JAVA_VERSION = ProjectProperty("JAVA_VERSION")

// Library
val LIBRARY_NAME = ProjectProperty("LIBRARY_NAME")
val LIBRARY_GROUP_ID = ProjectProperty("LIBRARY_GROUP_ID")
val LIBRARY_RELEASE = ProjectProperty("LIBRARY_RELEASE")
val LIBRARY_GITHUB = ProjectProperty("LIBRARY_GITHUB")
val LIBRARY_LICENSE = ProjectProperty("LIBRARY_LICENSE")

class ProjectProperty(
    val key: String
) {
    fun load(project: Project): Any? {
        return project.property(key)
    }

    fun loadString(project: Project): String {
        return load(project) as String
    }

    fun loadInt(project: Project): Int {
        return load(project) as Int
    }
}
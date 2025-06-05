import com.michaelflisar.buildlogic.BuildLogicPlugin

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("com.michaelflisar.buildlogic")
}

// get build logic plugin
val buildLogicPlugin = project.plugins.getPlugin(BuildLogicPlugin::class.java)

kotlin {

    // Java
    jvm()

    // -------
    // Sources
    // -------

    sourceSets {

        commonMain.dependencies {

            // Kotlin
            implementation(kotlinx.coroutines.core)

            implementation(libs.kotlin.test)
            implementation(kotlinx.coroutines.test)
            implementation(kotlinx.io.core)

            implementation(project(":kmp-template:modules:core"))

        }
    }
}
import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.gradle.maven.publish.plugin)
}

// -------------------
// Informations
// -------------------

val description = "a gradle plugin that provides common functions for kmp libraries"

// Module
val artifactId = "gradle-plugin"

// Library
val libraryName = "kmp-build-logic"
val libraryDescription = "kmp-build-logic - $artifactId module - $description"
val groupID = "io.github.mflisar.kmp-build-logic"
val release = 2023
val github = "https://github.com/MFlisar/kmp-build-logic"
val license = "Apache License 2.0"
val licenseUrl = "$github/blob/main/LICENSE"


gradlePlugin {
    plugins {
        create("BuildPlugin") {
            id = "com.michaelflisar.buildlogic.build-plugin"
            implementationClass = "com.michaelflisar.buildlogic.BuildPlugin"
        }
        create("SettingsPlugin") {
            id = "com.michaelflisar.buildlogic.settings-plugin"
            implementationClass = "com.michaelflisar.buildlogic.SettingsPlugin"
        }
        isAutomatedPublishing = true
    }
}

dependencies {
    implementation(libs.gradle.maven.publish.plugin)
    implementation(libs.kotlin.multiplatform)
    implementation(libs.android.build.tools)
    implementation(deps.yaml)

    api(project(":shared"))
}

mavenPublishing {

    configure(
        GradlePlugin(
            javadocJar = JavadocJar.Dokka("dokkaHtml"),
            sourcesJar = true
        )
    )

    coordinates(
        groupId = groupID,
        artifactId = artifactId,
        version = System.getenv("TAG")
    )

    pom {
        name.set(libraryName)
        description.set(libraryDescription)
        inceptionYear.set("$release")
        url.set(github)

        licenses {
            license {
                name.set(license)
                url.set(licenseUrl)
            }
        }

        developers {
            developer {
                id.set("mflisar")
                name.set("Michael Flisar")
                email.set("mflisar.development@gmail.com")
            }
        }

        scm {
            url.set(github)
        }
    }

    // Configure publishing to Maven Central
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, true)

    // Enable GPG signing for all publications
    signAllPublications()
}
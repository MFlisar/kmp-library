import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.gradle.maven.publish.plugin)
}

// -------------------
// Informations
// -------------------

val description = "functions and scripts to automate tasks for kmp-library projects"

// Module
val artifactId = "scripts"

// Library
val libraryName = "kmp-library"
val libraryDescription = "$libraryName - $artifactId module - $description"
val groupID = "io.github.mflisar.kmp-library"
val release = 2025
val github = "https://github.com/MFlisar/kmp-library"
val license = "Apache License 2.0"
val licenseUrl = "$github/blob/main/LICENSE"

// -------------------
// Setup
// -------------------

dependencies {
    implementation(deps.ktoml.core)
    implementation(deps.ktoml.file)
    implementation(deps.jsoup)
    implementation(deps.yaml)

    implementation(gradleApi())

    api(project(":library:plugins:shared"))
}

// -------------------
// Configurations
// -------------------

mavenPublishing {

    configure(
        JavaLibrary(
            javadocJar = JavadocJar.Dokka("dokkaHtml"),
            sourcesJar = true
        )
    )

    coordinates(
        groupId = groupID,
        artifactId = artifactId,
        version = System.getenv("TAG") ?: "LOCAL-SNAPSHOT"
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
    publishToMavenCentral(true)

    // Enable GPG signing for all publications
    if (System.getenv("CI")?.toBoolean() == true)
        signAllPublications()
}
import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.gradle.maven.publish.plugin)
}

// -------------------
// Informations
// -------------------

val description = "a gradle plugin that provides build functions for kmp libraries"

// Module
val artifactId = "plugins-shared"

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

kotlin {

    sourceSets {

        val main by getting {
            dependencies {
                implementation(deps.yaml)
            }
        }
    }
}

// -------------------
// Configurations
// -------------------

tasks.named<Jar>("jar") {
    dependsOn(tasks.named("compileJava"))
}

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
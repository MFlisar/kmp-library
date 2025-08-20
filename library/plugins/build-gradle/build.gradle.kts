import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar

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

val description = "a gradle build file plugin that provides common functions for kmp libraries"

// Module
val artifactId = "plugins-build-gradle"

// Library
val libraryName = "kmp-library"
val libraryDescription = "$libraryName - $artifactId module - $description"
val groupID = "io.github.mflisar.kmp-library"
val release = 2025
val github = "https://github.com/MFlisar/kmp-library"
val license = "Apache License 2.0"
val licenseUrl = "$github/blob/main/LICENSE"

// -------------------
// Plugins
// -------------------

gradlePlugin {
    plugins {
        create("Build Gradle Plugin") {
            id = "$groupID.$artifactId"
            implementationClass = "com.michaelflisar.kmplibrary.BuildFilePlugin"
        }
        isAutomatedPublishing = true
    }
}

dependencies {

    implementation(libs.gradle.maven.publish.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.android.build.tools)
    implementation(libs.compose)
    implementation(libs.kotlin.compose)
    implementation(libs.launch4j)

    implementation(deps.yaml)

    api(project(":library:plugins:shared"))
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
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
val libraryName = "kmp-template"
val libraryDescription = "$libraryName - $artifactId module - $description"
val groupID = "io.github.mflisar.kmp-template"
val release = 2023
val github = "https://github.com/MFlisar/kmp-template"
val license = "Apache License 2.0"
val licenseUrl = "$github/blob/main/LICENSE"

// -------------------
// Plugins
// -------------------

gradlePlugin {
    plugins {
        create("$groupID.build-plugin") {
            id = "$groupID.build-plugin"
            implementationClass = "com.michaelflisar.kmptemplate.BuildFilePlugin"
        }
        //create("$groupID.settings-plugin") {
        //    id = "$groupID.settings-plugin"
        //    implementationClass = "com.michaelflisar.kmptemplate.SettingFilePlugin"
        //}
        isAutomatedPublishing = true
    }
}

dependencies {
    implementation(libs.gradle.maven.publish.plugin)
    implementation(libs.kotlin.multiplatform)
    implementation(libs.android.build.tools)
    implementation(deps.yaml)

    api(project(":build-logic:shared"))
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
    if (System.getenv("CI")?.toBoolean() == true)
        signAllPublications()
}
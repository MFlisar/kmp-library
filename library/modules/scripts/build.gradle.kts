import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SonatypeHost

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

val description = "a collection of scripts to generate mkdocs based documentation for the kmp library"

// Module
val artifactId = "scripts"

// Library
val libraryName = "kmp-build-logic"
val libraryDescription = "kmp-build-logic - $artifactId module - $description"
val groupID = "io.github.mflisar.kmp-build-logic"
val release = 2023
val github = "https://github.com/MFlisar/kmp-build-logic"
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

    implementation(project(":shared"))
}

// allows to run the application with `./gradlew run -PmainClass=com.michaelflisar.scripts.UpdateDocsKt`
if (System.getenv("CI")?.toBoolean() == true) {
    application {
        val mc = project.findProperty("mainClass") as? String
        if (mc != null) {
            mainClass.set(mc)
        }
    }
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
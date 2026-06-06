import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

import java.net.URL
import java.net.URLClassLoader

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.gradle.maven.publish.plugin)
}

// -------------------
// Informations
// -------------------

val description =
    "a gradle plugin and plain kotlin dependency that provides core functions for kmp libraries for development tooling"

// Module
val artifactId = "core"

// Library
val libraryName = "kmp-devtools"
val libraryDescription = "$libraryName - $artifactId module - $description"
val groupID = "io.github.mflisar.kmpdevtools"
val release = 2025
val github = "https://github.com/MFlisar/kmp-devtools"
val license = "Apache License 2.0"
val licenseUrl = "$github/blob/main/LICENSE"

// -------------------
// Setup
// -------------------

kotlin {

    sourceSets {

        val main by getting { }

        main.dependencies {

            implementation(gradleKotlinDsl())

            implementation(deps.yaml)

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
            javadocJar = JavadocJar.Dokka("dokkaGenerateHtml"),
            sourcesJar = SourcesJar.Sources()
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

tasks.register("invokeGithubUtil") {
    group = "verification"
    description = "Invokes GithubUtil.getLastRelease for all ReleaseType values via reflection"
    dependsOn("classes") // sicherstellen, dass der Code kompiliert ist

    doLast {
        val repo = project.findProperty("githubRepo")?.toString()
            ?: System.getenv("GITHUB_REPO")
            ?: "MFlisar/kmp-devtools"

        // build classpath -> URLs
        val urls = sourceSets["main"].runtimeClasspath.files.map { it.toURI().toURL() }.toTypedArray()
        val loader = URLClassLoader(urls, null) // null parent macht die Invocation isolierter

        try {
            // Lade das GithubUtil object und dessen enum ReleaseType
            val utilClass = loader.loadClass("com.michaelflisar.kmpdevtools.core.utils.GithubUtil")
            val enumClass = loader.loadClass("com.michaelflisar.kmpdevtools.core.utils.GithubUtil\$ReleaseType")
            val instance = utilClass.getField("INSTANCE").get(null)

            // Methode mit (String, ReleaseType)
            val method = utilClass.getMethod("getLastRelease", String::class.java, enumClass)

            // Alle Enum-Werte durchlaufen und aufrufen
            val enumValuesMethod = enumClass.getMethod("values")
            val enumValues = enumValuesMethod.invoke(null) as Array<*>

            for (enumConst in enumValues) {
                val typeName = enumConst.toString()
                val result = method.invoke(instance, repo, enumConst) as? String
                println("ReleaseType: $typeName -> $result")
            }
        } finally {
            // sicher schließen
            try {
                loader.close()
            } catch (_: Exception) { /* ignore */ }
        }
    }
}
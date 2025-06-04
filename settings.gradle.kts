pluginManagement {

    // repositories for build
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }

    includeBuild("build-logic")
}

dependencyResolutionManagement {

    // repositories for dependencies
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }

    versionCatalogs {
        create("app") {
            from(files("gradle/app.versions.toml"))
        }
        create("androidx") {
            from(files("gradle/androidx.versions.toml"))
        }
        create("kotlinx") {
            from(files("gradle/kotlinx.versions.toml"))
        }
        create("deps") {
            from(files("gradle/deps.versions.toml"))
        }
    }

}

// --------------------
// include modules
// --------------------

val library = settings.providers.gradleProperty("LIBRARY_NAME").get()
println("Modules:")
val folderLibrary = File(rootDir, "library")
folderLibrary
    .walk()
    .maxDepth(10)
    .filter { it.isDirectory && File(it, "build.gradle.kts").exists() }
    .forEach { dir ->
        val relativePath = dir.relativeTo(folderLibrary)
        val relativeRoot = generateSequence(relativePath) { it.parentFile }.last()
        val baseModulePath = ":" + relativePath.invariantSeparatorsPath.replace('/', ':')
        val modulePath: String
        if (relativeRoot.name != "demo") {
            modulePath = ":" + library.lowercase() + baseModulePath
            include(modulePath)
            project(modulePath).projectDir = dir
        } else {
            modulePath = ":" + relativePath.invariantSeparatorsPath.replace('/', ':')
            include(modulePath)
            project(modulePath).projectDir = dir
        }
        println("  - $modulePath")
    }

include(":generator:scripts")
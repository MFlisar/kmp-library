pluginManagement {

    // repositories for build
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }


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

include(":shared")
project(":shared").projectDir = file("library/modules/build-logic/shared")
include(":plugin")
project(":plugin").projectDir = file("library/modules/build-logic/plugin")
/*
includeBuild("library/modules/build-logic") {
    //dependencySubstitution {
    //    substitute(project(":shared"))
    //        .using(project(":kmp-template:modules:build-logic:shared"))
    //}
}*/

// --------------------
// include modules
// --------------------

/*
plugins {
    id("com.michaelflisar.buildlogic.settings-plugin")
}

apply<com.michaelflisar.buildlogic.SettingPlugin>()
val settingsPlugin = settings.plugins.getPlugin(com.michaelflisar.buildlogic.SettingPlugin::class.java)
settingsPlugin.importAllModules()
*/

val library = rootDir.name.lowercase()//settings.providers.gradleProperty("LIBRARY_KEY").get()
println("Modules:")
val folderLibrary = File(rootDir, "library")
folderLibrary
    .walk()
    .maxDepth(10)
    .filter { it.isDirectory && File(it, "build.gradle.kts").exists() }
    .filter { !it.path.contains("build-logic") }
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
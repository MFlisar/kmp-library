pluginManagement {

    // repositories for build
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
        mavenLocal()
    }

}

dependencyResolutionManagement {

    // repositories for dependencies
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
        maven("https://jitpack.io")
        mavenLocal()
    }

    versionCatalogs {
        create("kotlinx") {
            from(files("gradle/kotlinx.versions.toml"))
        }
        create("deps") {
            from(files("gradle/deps.versions.toml"))
        }
        create("app") {
            from(files("gradle/app.versions.toml"))
        }
    }

}

fun includeModule(
    moduleName: String,
    path: String)
{
    include(moduleName)
    project(moduleName).projectDir = file(path)
}

includeModule(":shared","library/shared")

includeModule(":plugin", "library/plugin")
includeModule(":docs", "library/docs")
includeModule(":scripts", "library/scripts")
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
        create("libs") {
            from(files("../../../gradle/libs.versions.toml"))
        }
        create("kotlinx") {
            from(files("../../../gradle/kotlinx.versions.toml"))
        }
        create("deps") {
            from(files("../../../gradle/deps.versions.toml"))
        }
    }
}

include(":build-logic:shared")
project(":build-logic:shared").projectDir = file("shared")

include(":build-logic:plugin")
project(":build-logic:plugin").projectDir = file("plugin")
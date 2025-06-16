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

include(":build-logic:shared")
project(":build-logic:shared").projectDir = file("library/modules/build-logic/shared")

include(":build-logic:plugin")
project(":build-logic:plugin").projectDir = file("library/modules/build-logic/plugin")

include(":docs")
project(":docs").projectDir = file("library/modules/docs")

include(":open-source-demo")
project(":open-source-demo").projectDir = file("library/modules/open-source-demo")
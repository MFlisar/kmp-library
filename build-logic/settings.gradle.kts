
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
        create("app") {
            from(files("../gradle/app.versions.toml"))
        }
        create("androidx") {
            from(files("../gradle/androidx.versions.toml"))
        }
        create("kotlinx") {
            from(files("../gradle/kotlinx.versions.toml"))
        }
        create("deps") {
            from(files("../gradle/deps.versions.toml"))
        }
    }
}

pluginManagement {

    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}
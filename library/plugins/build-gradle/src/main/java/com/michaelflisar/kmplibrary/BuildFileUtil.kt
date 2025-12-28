package com.michaelflisar.kmplibrary

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.LibraryExtension
import com.michaelflisar.kmplibrary.configs.Config
import com.michaelflisar.kmplibrary.configs.LibraryConfig
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.Platform
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import java.io.File
import kotlin.text.toBoolean
import kotlin.text.toInt

object BuildFileUtil {

    fun checkGradleProperty(project: Project, property: String): Boolean? {
        if (!project.providers.gradleProperty(property).isPresent) {
            return null
        }
        return project.providers.gradleProperty(property).get().toBoolean()
    }

    /**
     * Configures the project for publishing to Maven Central.
     * This includes setting up the Maven coordinates, POM metadata, and signing.
     *
     * Usage:
     *
     * to only auto-publish releases without suffixes like "-debug", "-alpha", "-test" use following:
     * <pre><code>
     * autoReleaseOnMavenCentral = { version -> !version.contains("-") }
     * </code></pre>
     *
     * @param project The Gradle project to configure.
     * @param module The module information for the publication.
     * @param libraryConfig The library configuration for the publication.
     * @param platform The platform configuration for the publication.
     * @param autoReleaseOnMavenCentral A function that determines if releases should be automatically published.
     * @param sign Whether to sign the publications.
     * @param version The version of the library, defaults to the value of the "TAG" environment variable (TAG is set by github action workflow) or "LOCAL-SNAPSHOT".
     */
    fun setupMavenPublish(
        project: Project,
        libraryConfig: LibraryConfig,
        platform: Platform = KotlinMultiplatform(
            javadocJar = JavadocJar.Dokka("dokkaHtml"),
            sourcesJar = true
        ),
        autoReleaseOnMavenCentral: (version: String) -> Boolean = { true },
        sign: Boolean = System.getenv("CI")?.toBoolean() == true,
        version: String = System.getenv("TAG") ?: "LOCAL-SNAPSHOT"
    ) {
        val path = project.projectDir.relativeTo(project.rootDir).path
        val module = libraryConfig.getModuleByPath(path)

        project.extensions.configure(MavenPublishBaseExtension::class.java) {
            configure(platform)
            coordinates(
                groupId = libraryConfig.maven.groupId,
                artifactId = module.artifactId,
                version = version
            )

            pom {
                name.set(libraryConfig.library.name)
                description.set(module.libraryDescription(libraryConfig))
                inceptionYear.set(libraryConfig.library.release.toString())
                url.set(libraryConfig.library.linkRepo)

                licenses {
                    license {
                        name.set(libraryConfig.library.license.name)
                        url.set(libraryConfig.library.license.link)
                    }
                }

                developers {
                    developer {
                        id.set(libraryConfig.developer.mavenId)
                        name.set(libraryConfig.developer.name)
                        email.set(libraryConfig.developer.mail)
                    }
                }

                scm {
                    url.set(libraryConfig.library.linkRepo)
                }
            }

            // Configure publishing to Maven Central
            publishToMavenCentral(autoReleaseOnMavenCentral(version))

            // Enable GPG signing for all publications
            if (sign) {
                signAllPublications()
            }
        }
    }

    fun setupAndroidLibrary(
        project: Project,
        config: Config,
        androidNamespace: String,
        compileSdk: Provider<String>,
        minSdk: Provider<String>,
        buildConfig: Boolean
    ) {
        project.extensions.configure(LibraryExtension::class.java) {
            namespace = androidNamespace

            this.compileSdk = compileSdk.get().toInt()

            buildFeatures {
                this.buildConfig = buildConfig
            }

            defaultConfig {
                this.minSdk = minSdk.get().toInt()
            }

            compileOptions {
                sourceCompatibility = JavaVersion.toVersion(config.javaVersion)
                targetCompatibility = JavaVersion.toVersion(config.javaVersion)
            }
        }
    }

    fun setupAndroidApp(
        project: Project,
        config: Config,
        androidNamespace: String,
        compileSdk: Provider<String>,
        minSdk: Provider<String>,
        targetSdk: Provider<String>,
        versionCode: Int,
        versionName: String,
        buildConfig: Boolean,
        checkDebugKeyStoreProperty: Boolean
    ) {
        project.extensions.configure(ApplicationExtension::class.java) {
            namespace = androidNamespace

            this.compileSdk = compileSdk.get().toInt()

            buildFeatures {
                this.buildConfig = buildConfig
            }

            defaultConfig {
                this.minSdk = minSdk.get().toInt()
                this.targetSdk = targetSdk.get().toInt()
                this.versionCode = versionCode
                this.versionName = versionName
            }

            compileOptions {
                sourceCompatibility = JavaVersion.toVersion(config.javaVersion)
                targetCompatibility = JavaVersion.toVersion(config.javaVersion)
            }

            // eventually use local custom signing
            if (checkDebugKeyStoreProperty) {
                val debugKeyStore = project.providers.gradleProperty("debugKeyStore").orNull
                if (debugKeyStore != null) {
                    signingConfigs {
                        getByName("debug") {
                            keyAlias = "androiddebugkey"
                            keyPassword = "android"
                            storeFile = File(debugKeyStore)
                            storePassword = "android"
                        }
                    }
                }
            }
        }
    }

}
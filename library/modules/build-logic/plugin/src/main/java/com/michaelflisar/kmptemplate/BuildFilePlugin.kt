package com.michaelflisar.kmptemplate

import com.android.build.gradle.LibraryExtension
import com.michaelflisar.kmptemplate.Setup
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.Platform
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class BuildFilePlugin : Plugin<Project> {

    private lateinit var project: Project
    private lateinit var setup: Setup

    override fun apply(project: Project) {
        this.project = project
        setup = Setup.read(project.rootDir)
    }

    /*
        * Configures the project for publishing to Maven Central.
        * This includes setting up the Maven coordinates, POM metadata, and signing.
        *
        * Usage:
        *
        * to only auto-publish releases without suffixes like "-debug", "-alpha", "-test" use following:
        * <pre><code>
        * // TAG is set by github action workflow
        * autoReleaseOnMavenCentral = !System.getenv("TAG").orEmpty().contains("-")
        * </code></pre>
        *
        * @param platform The platform configuration for the publication.
        * @param autoReleaseOnMavenCentral A function that determines if releases should be automatically published.
        * @param tag The tag used for the versioning, typically set by CI/CD.
        * @param sign Whether to sign the publications.
     */
    fun setupMavenPublish(
        platform: Platform = KotlinMultiplatform(
            javadocJar = JavadocJar.Dokka("dokkaHtml"),
            sourcesJar = true
        ),
        autoReleaseOnMavenCentral: Boolean = true,
        sign: Boolean = System.getenv("CI")?.toBoolean() == true,
    ) {
        val path = project.projectDir.relativeTo(project.rootDir).path
        val module = setup.getModuleByPath(path)
        project.extensions.configure(MavenPublishBaseExtension::class.java) {
            configure(platform)
            coordinates(
                groupId = setup.maven.groupId,
                artifactId = module.artifactId,
                version = System.getenv("TAG")
            )

            pom {
                name.set(setup.library.name)
                description.set(module.libraryDescription(setup))
                inceptionYear.set(setup.library.release.toString())
                url.set(setup.library.linkRepo)

                licenses {
                    license {
                        name.set(setup.library.license.name)
                        url.set(setup.library.license.link)
                    }
                }

                developers {
                    developer {
                        id.set(setup.developer.mavenId)
                        name.set(setup.developer.name)
                        email.set(setup.developer.mail)
                    }
                }

                scm {
                    url.set(setup.library.linkRepo)
                }
            }

            // Configure publishing to Maven Central
            publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, autoReleaseOnMavenCentral)

            // Enable GPG signing for all publications
            if (sign) {
                signAllPublications()
            }
        }
    }

    fun setupTargets(
        targets: Targets,
    ) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {

            // Android
            if (targets.android) {
                androidTarget {
                    publishLibraryVariants("release")
                    compilerOptions {
                        jvmTarget.set(JvmTarget.fromTarget(setup.javaVersion))
                    }
                }
            }

            // iOS
            if (targets.iOS) {
                iosX64()
                iosArm64()
                iosSimulatorArm64()
            }

            // Windows
            if (targets.windows) {
                jvm()
            }

            // macOS
            if (targets.macOS) {
                macosX64()
                macosArm64()
            }

            // Linux
            if (targets.linux) {
                linuxX64()
                linuxArm64()
            }

            // WASM
            if (targets.wasm) {
                @OptIn(ExperimentalWasmDsl::class)
                wasmJs {
                    nodejs()
                }
            }

            // JavaScript
            if (targets.js) {
                js()
                js(IR)
            }
        }
    }

    fun setupAndroid(
        androidNamespace: String,
        compileSdk: Provider<String>,
        minSdk: Provider<String>,
        compose: Boolean,
        buildConfig: Boolean
    ) {
        project.extensions.configure(LibraryExtension::class.java) {
            namespace = androidNamespace

            this.compileSdk = compileSdk.get().toInt()

            buildFeatures {
                this.compose = compose
                this.buildConfig = buildConfig
            }

            defaultConfig {
                this.minSdk = minSdk.get().toInt()
            }

            compileOptions {
                sourceCompatibility = JavaVersion.toVersion(setup.javaVersion)
                targetCompatibility = JavaVersion.toVersion(setup.javaVersion)
            }
        }
    }
}
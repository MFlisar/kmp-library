package com.michaelflisar.kmpgradletools

import com.android.build.gradle.LibraryExtension
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.Platform
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class BuildFilePlugin : Plugin<Project> {

    private lateinit var project: Project
    private lateinit var setupLibrary: SetupLibrary
    private var javaVersion: String? = null

    private var setupModules: SetupModules? = null

    override fun apply(project: Project) {
        this.project = project
        setupLibrary = SetupLibrary.read(project.rootDir)
        javaVersion = project.findProperty("KMP-TEMPLATE-JAVA-VERSION") as String?
    }

    fun javaVersion(): String {
        return javaVersion ?: setupLibrary.javaVersion
    }

    /**
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
        version: String = System.getenv("TAG") ?: "UNSPECIFIED"
    ) {
       if (setupModules == null) {
            setupModules = SetupModules.read(project.rootDir)
        }
        val setup = setupModules!!

        val path = project.projectDir.relativeTo(project.rootDir).path
        val module = setup.getModuleByPath(path)
        project.extensions.configure(MavenPublishBaseExtension::class.java) {
            configure(platform)
            coordinates(
                groupId = setupLibrary.maven.groupId,
                artifactId = module.artifactId,
                version = version
            )

            pom {
                name.set(setupLibrary.library.name)
                description.set(module.libraryDescription(setupLibrary))
                inceptionYear.set(setupLibrary.library.release.toString())
                url.set(setupLibrary.library.linkRepo)

                licenses {
                    license {
                        name.set(setupLibrary.library.license.name)
                        url.set(setupLibrary.library.license.link)
                    }
                }

                developers {
                    developer {
                        id.set(setupLibrary.developer.mavenId)
                        name.set(setupLibrary.developer.name)
                        email.set(setupLibrary.developer.mail)
                    }
                }

                scm {
                    url.set(setupLibrary.library.linkRepo)
                }
            }

            // Configure publishing to Maven Central
            publishToMavenCentral(autoReleaseOnMavenCentral)

            // Enable GPG signing for all publications
            if (sign) {
                signAllPublications()
            }
        }
    }

    fun setupTargets(
        targets: Targets
    ) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {

            // Android
            if (targets.android) {
                androidTarget {
                    publishLibraryVariants("release")
                    compilerOptions {
                        jvmTarget.set(JvmTarget.fromTarget(javaVersion()))
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
                sourceCompatibility = JavaVersion.toVersion(javaVersion())
                targetCompatibility = JavaVersion.toVersion(javaVersion())
            }
        }
    }
}
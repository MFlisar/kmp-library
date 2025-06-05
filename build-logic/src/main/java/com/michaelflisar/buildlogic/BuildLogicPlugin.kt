package com.michaelflisar.buildlogic

import com.android.build.gradle.LibraryExtension
import com.michaelflisar.buildlogic.classes.Targets
import com.michaelflisar.buildlogic.shared.Setup
import com.michaelflisar.buildlogic.shared.SetupData
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class BuildLogicPlugin : Plugin<Project> {

    private lateinit var project: Project
    private lateinit var setup: Setup

    override fun apply(project: Project) {
        this.project = project
        setup = SetupData.read(project.rootDir).setup
    }

    fun setupMavenPublish(
        autoPublishReleases: Boolean = true
    ) {
        val path = project.projectDir.relativeTo(project.rootDir).path
        val module = setup.getModuleByPath(path)
        project.extensions.configure(MavenPublishBaseExtension::class.java) {
            configure(
                KotlinMultiplatform(
                    javadocJar = JavadocJar.Dokka("dokkaHtml"),
                    sourcesJar = true
                )
            )
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
            val tag = System.getenv("TAG").orEmpty() // is set by the github action workflow
            val autoReleaseOnMavenCentral =
                autoPublishReleases && !tag.contains("-") // debug, alpha and test builds end like "-debug", "-alpha", "-test" and should not be released automatically
            publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, autoReleaseOnMavenCentral)

            // Enable GPG signing for all publications
            if (System.getenv("CI")?.toBoolean() == true) {
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
        minSdk: Provider<String>
    ) {
        project.extensions.configure(LibraryExtension::class.java) {
            namespace = androidNamespace

            this.compileSdk = compileSdk.get().toInt()

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
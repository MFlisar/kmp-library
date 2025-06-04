package com.michaelflisar.buildlogic

import com.android.build.gradle.LibraryExtension
import com.michaelflisar.buildlogic.classes.DEVELOPER_EMAIL
import com.michaelflisar.buildlogic.classes.DEVELOPER_ID
import com.michaelflisar.buildlogic.classes.DEVELOPER_NAME
import com.michaelflisar.buildlogic.classes.JAVA_VERSION
import com.michaelflisar.buildlogic.classes.LIBRARY_GITHUB
import com.michaelflisar.buildlogic.classes.LIBRARY_GROUP_ID
import com.michaelflisar.buildlogic.classes.LIBRARY_LICENSE
import com.michaelflisar.buildlogic.classes.LIBRARY_NAME
import com.michaelflisar.buildlogic.classes.LIBRARY_RELEASE
import com.michaelflisar.buildlogic.classes.ModuleMetaData
import com.michaelflisar.buildlogic.classes.Targets
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class BuildLogicPlugin : Plugin<Project> {

    private lateinit var project: Project

    override fun apply(project: Project) {
        this.project = project
    }

    fun setupMavenPublish(
        module: ModuleMetaData,
        autoPublishReleases: Boolean = true
    ) {
        project.extensions.configure(MavenPublishBaseExtension::class.java) {
            configure(
                KotlinMultiplatform(
                    javadocJar = JavadocJar.Dokka("dokkaHtml"),
                    sourcesJar = true
                )
            )
            coordinates(
                groupId = LIBRARY_GROUP_ID.loadString(project),
                artifactId = module.artifactId,
                version = System.getenv("TAG")
            )

            pom {
                name.set(LIBRARY_NAME.loadString(project))
                description.set(module.libraryDescription(project))
                inceptionYear.set(LIBRARY_RELEASE.loadString(project))
                url.set(LIBRARY_GITHUB.loadString(project))

                licenses {
                    license {
                        name.set(LIBRARY_LICENSE.loadString(project))
                        url.set(module.licenseUrl(project))
                    }
                }

                developers {
                    developer {
                        id.set(DEVELOPER_ID.loadString(project))
                        name.set(DEVELOPER_NAME.loadString(project))
                        email.set(DEVELOPER_EMAIL.loadString(project))
                    }
                }

                scm {
                    url.set(LIBRARY_GITHUB.loadString(project))
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
                        jvmTarget.set(JvmTarget.fromTarget(JAVA_VERSION.loadString(project)))
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

    fun setupAndroid(meta: ModuleMetaData, compileSdk: Provider<String>, minSdk: Provider<String>) {
        setupAndroid(meta.androidNamespace, compileSdk, minSdk)
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
                sourceCompatibility = JavaVersion.toVersion(JAVA_VERSION.loadString(project))
                targetCompatibility = JavaVersion.toVersion(JAVA_VERSION.loadString(project))
            }
        }
    }
}
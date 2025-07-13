package com.michaelflisar.kmpgradletools

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.LibraryExtension
import com.android.tools.r8.internal.wa
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
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithSimulatorTests
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

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
        * autoReleaseOnMavenCentral = { version -> !version.contains("-") }
        * </code></pre>
        *
        * @param platform The platform configuration for the publication.
        * @param autoReleaseOnMavenCentral A function that determines if releases should be automatically published.
        * @param sign Whether to sign the publications.
        * @param version The version of the library, defaults to the value of the "TAG" environment variable (TAG is set by github action workflow) or "LOCAL-SNAPSHOT".
     */
    fun setupMavenPublish(
        platform: Platform = KotlinMultiplatform(
            javadocJar = JavadocJar.Dokka("dokkaHtml"),
            sourcesJar = true
        ),
        autoReleaseOnMavenCentral: (version: String) -> Boolean = { true },
        sign: Boolean = System.getenv("CI")?.toBoolean() == true,
        version: String = System.getenv("TAG") ?: "LOCAL-SNAPSHOT"
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
            publishToMavenCentral(autoReleaseOnMavenCentral(version))

            // Enable GPG signing for all publications
            if (sign) {
                signAllPublications()
            }
        }
    }

    fun setupTargetsLibrary(
        targets: Targets
    ) {
        setupTargets(targets, listOf("release"), false, "", "")
    }

    fun setupTargetsApp(
        targets: Targets,
        wasmModuleName: String = "app",
        wasmOutputFileName: String = "app.js"
    ) {
        setupTargets(targets, emptyList(), true, wasmModuleName, wasmOutputFileName)
    }

    private fun setupTargets(
        targets: Targets,
        publishLibraryVariantsNames: List<String>,
        isApp: Boolean,
        wasmModuleName: String,
        wasmOutputFileName: String,
        configAndroid: (KotlinAndroidTarget.() -> Unit) = {},
        configIOS: (KotlinNativeTarget.() -> Unit) = {},
        configIOSTests: (KotlinNativeTargetWithSimulatorTests.() -> Unit) = {},
        configWindows: (KotlinJvmTarget.() -> Unit) = {},
        configMacOS: (KotlinNativeTargetWithHostTests.() -> Unit) = {},
        configLinux: (KotlinNativeTarget.() -> Unit) = {},
        configWASM: (KotlinWasmJsTargetDsl.() -> Unit) = {},
        configJS: (KotlinJsTargetDsl.() -> Unit) = {},
    ) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {

            // Android
            if (targets.android) {
                androidTarget {
                    if (publishLibraryVariantsNames.isNotEmpty())
                        publishLibraryVariants(*publishLibraryVariantsNames.toTypedArray())
                    compilerOptions {
                        jvmTarget.set(JvmTarget.fromTarget(javaVersion()))
                    }
                    configAndroid()
                }
            }

            // iOS
            if (targets.iOS) {
                iosX64 {
                    configIOS()
                    configIOSTests()
                }
                iosArm64 {
                    configIOS()
                }
                iosSimulatorArm64 {
                    configIOS()
                    configIOSTests()
                }
            }

            // Windows
            if (targets.windows) {
                jvm {
                    configWindows()
                }
            }

            // macOS
            if (targets.macOS) {
                macosX64 {
                    configMacOS()
                }
                macosArm64 {
                    configMacOS()
                }
            }

            // Linux
            if (targets.linux) {
                linuxX64 {
                    configLinux()
                }
                linuxArm64 {
                    configLinux()
                }
            }

            // WASM
            if (targets.wasm) {
                @OptIn(ExperimentalWasmDsl::class)
                wasmJs {
                    if (isApp) {
                        outputModuleName.set(wasmModuleName)
                        val rootDirPath = project.rootDir.path
                        browser {
                            commonWebpackConfig {
                                outputFileName = wasmOutputFileName
                                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                                    static = (static ?: mutableListOf()).apply {
                                        // Serve sources to debug inside browser
                                        add(rootDirPath)
                                    }
                                }
                            }
                        }
                        binaries.executable()
                    } else {
                        nodejs()
                    }
                    configWASM()
                }
            }

            // JavaScript
            if (targets.js) {
                js {
                    configJS()
                }
                js(IR) {
                    configJS()
                }
            }
        }
    }

    fun setupAndroidLibrary(
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

    fun setupAndroidApp(
        androidNamespace: String,
        compileSdk: Provider<String>,
        minSdk: Provider<String>,
        targetSdk: Provider<String>,
        versionCode: Int,
        versionName: String,
        buildConfig: Boolean
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
                sourceCompatibility = JavaVersion.toVersion(javaVersion())
                targetCompatibility = JavaVersion.toVersion(javaVersion())
            }
        }
    }


}
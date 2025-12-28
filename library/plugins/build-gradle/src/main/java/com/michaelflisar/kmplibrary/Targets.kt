package com.michaelflisar.kmplibrary

import com.michaelflisar.kmplibrary.configs.Config
import com.michaelflisar.kmplibrary.setups.WasmSetup
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithSimulatorTests
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

class Targets(
    val android: Boolean = false,
    val iOS: Boolean = false,
    val windows: Boolean = false,
    val linux: Boolean = false,
    val macOS: Boolean = false,
    val wasm: Boolean = false,
    val js: Boolean = false,
) {
    private val enabledPlatforms = Platform.values()
        .filter {
            when (it) {
                Platform.ANDROID -> android
                Platform.IOS -> iOS
                Platform.WINDOWS -> windows
                Platform.MACOS -> macOS
                Platform.LINUX -> linux
                Platform.WASM -> wasm
                Platform.JS -> js
            }
        }

    fun isEnabled(target: Platform) = enabledPlatforms.contains(target)

    fun getPlatforms(exclusions: List<Platform>): List<Platform> {
        return enabledPlatforms.filter { !exclusions.contains(it) }
    }

    fun setupDependencies(
        sourceSet: KotlinSourceSet,
        sourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
        buildTargets: Targets,
        platforms: List<Platform>,
        platformsNotSupported: Boolean = false,
    ) {
        val targets = if (platformsNotSupported) {
            buildTargets.getPlatforms(exclusions = platforms)
        } else {
            platforms
        }
        targets.filter { buildTargets.isEnabled(it) }.forEach { target ->
            val groupMain = sourceSets.maybeCreate(target.nameMain)
            groupMain.dependsOn(sourceSet)
            target.targets.forEach {
                sourceSets.getByName("${it}Main").dependsOn(groupMain)
            }
        }
    }

    fun setupTargetsLibrary(
        project: Project,
        config: Config,
        publishLibraryVariantsNames: List<String> = listOf("release"),
        configureAndroid: (KotlinAndroidTarget.() -> Unit) = {},
        configureIOS: (KotlinNativeTarget.() -> Unit) = {},
        configureIOSTests: (KotlinNativeTargetWithSimulatorTests.() -> Unit) = {},
        configureWindows: (KotlinJvmTarget.() -> Unit) = {},
        configureMacOS: (KotlinNativeTargetWithHostTests.() -> Unit) = {},
        configureLinux: (KotlinNativeTarget.() -> Unit) = {},
        configureWASM: (KotlinWasmJsTargetDsl.() -> Unit) = {},
        configureJS: (KotlinJsTargetDsl.() -> Unit) = {},
    ) {
        setupAndroidLibraryTarget(project, config, publishLibraryVariantsNames, configureAndroid)
        setupIOSTarget(project, configureIOS, configureIOSTests)
        setupWindowsTarget(project, configureWindows)
        setupMacOSTarget(project, configureMacOS)
        setupLinuxTarget(project, configureLinux)
        setupWasmLibraryTarget(project, configureWASM)
        setupJSTarget(project, configureJS)
    }

    fun setupTargetsApp(
        project: Project,
        config: Config,
        wasmSetup: WasmSetup,
        configAndroid: (KotlinAndroidTarget.() -> Unit) = {},
        configIOS: (KotlinNativeTarget.() -> Unit) = {},
        configIOSTests: (KotlinNativeTargetWithSimulatorTests.() -> Unit) = {},
        configWindows: (KotlinJvmTarget.() -> Unit) = {},
        configMacOS: (KotlinNativeTargetWithHostTests.() -> Unit) = {},
        configLinux: (KotlinNativeTarget.() -> Unit) = {},
        configWASM: (KotlinWasmJsTargetDsl.() -> Unit) = {},
        configJS: (KotlinJsTargetDsl.() -> Unit) = {},
    ) {
        setupAndroidAppTarget(project, config, configAndroid)
        setupIOSTarget(project, configIOS, configIOSTests)
        setupWindowsTarget(project, configWindows)
        setupMacOSTarget(project, configMacOS)
        setupLinuxTarget(project, configLinux)
        setupWasmAppTarget(project, wasmSetup, configWASM)
        setupJSTarget(project, configJS)
    }

    /**
     * Setup Android library target in the given project with the given configuration.
     *
     * @param project The Gradle project to configure.
     * @param config The configuration to use for the Android target.
     * @param publishLibraryVariantsNames The names of the library variants to publish. Default is ["release"].
     * @param configure A lambda to configure the Android target.
     */
    fun setupAndroidLibraryTarget(
        project: Project,
        config: Config,
        publishLibraryVariantsNames: List<String> = listOf("release"),
        configure: (KotlinAndroidTarget.() -> Unit) = {},
    ) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            if (android) {
                androidTarget {
                    if (publishLibraryVariantsNames.isNotEmpty())
                        publishLibraryVariants(*publishLibraryVariantsNames.toTypedArray())
                    compilerOptions {
                        jvmTarget.set(JvmTarget.fromTarget(config.javaVersion))
                    }
                    configure()
                }
            }
        }
    }

    /**
     * Setup Android app target in the given project with the given configuration.
     *
     * @param project The Gradle project to configure.
     * @param config The configuration to use for the Android target.
     * @param configure A lambda to configure the Android target.
     */
    fun setupAndroidAppTarget(
        project: Project,
        config: Config,
        configure: (KotlinAndroidTarget.() -> Unit) = {},
    ) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            if (android) {
                androidTarget {
                    compilerOptions {
                        jvmTarget.set(JvmTarget.fromTarget(config.javaVersion))
                    }
                    configure()
                }
            }
        }
    }

    /**
     * Setup iOS targets in the given project with the given configuration.
     *
     * @param project The Gradle project to configure.
     * @param configure A lambda to configure each iOS target.
     * @param configureTests A lambda to configure simulator tests for applicable iOS targets.
     */
    fun setupIOSTarget(
        project: Project,
        configure: (KotlinNativeTarget.() -> Unit),
        configureTests: (KotlinNativeTargetWithSimulatorTests.() -> Unit),
    ) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            if (iOS) {
                iosX64 {
                    configure()
                    configureTests()
                }
                iosArm64 {
                    configure()
                }
                iosSimulatorArm64 {
                    configure()
                    configureTests()
                }
            }
        }
    }

    /**
     * Setup Windows (JVM) target in the given project with the given configuration.
     *
     * @param project The Gradle project to configure.
     * @param configure A lambda to configure the Windows (JVM) target.
     */
    fun setupWindowsTarget(
        project: Project,
        configure: (KotlinJvmTarget.() -> Unit),
    ) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            if (windows) {
                jvm {
                    configure()
                }
            }
        }
    }

    /**
     * Setup macOS targets in the given project with the given configuration.
     *
     * @param project The Gradle project to configure.
     * @param configure A lambda to configure each macOS target.
     */
    fun setupMacOSTarget(
        project: Project,
        configure: (KotlinNativeTargetWithHostTests.() -> Unit),
    ) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            if (macOS) {
                macosX64 {
                    configure()
                }
                macosArm64 {
                    configure()
                }
            }
        }
    }

    /**
     * Setup Linux targets in the given project with the given configuration.
     *
     * @param project The Gradle project to configure.
     * @param configure A lambda to configure each Linux target.
     */
    fun setupLinuxTarget(
        project: Project,
        configure: (KotlinNativeTarget.() -> Unit),
    ) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            if (linux) {
                linuxX64 {
                    configure()
                }
                linuxArm64 {
                    configure()
                }
            }
        }
    }

    /**
     * Setup Wasm app target in the given project with the given configuration.
     *
     * @param project The Gradle project to configure.
     * @param wasmSetup The Wasm setup configuration.
     * @param configure A lambda to configure the Wasm target.
     */
    fun setupWasmAppTarget(
        project: Project,
        wasmSetup: WasmSetup,
        configure: (KotlinWasmJsTargetDsl.() -> Unit),
    ) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            if (wasm) {
                @OptIn(ExperimentalWasmDsl::class)
                wasmJs {
                    outputModuleName.set(wasmSetup.moduleName)
                    val rootDirPath = project.rootDir.path
                    val projectDirPath = project.projectDir.path
                    browser {
                        commonWebpackConfig {
                            outputFileName = wasmSetup.outputFileName
                            devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                                static = (static ?: mutableListOf()).apply {
                                    // Serve sources to debug inside browser
                                    add(rootDirPath)
                                    add(projectDirPath)
                                }
                            }
                        }
                    }
                    binaries.executable()

                    configure()
                }
            }
        }
    }

    fun setupWasmLibraryTarget(
        project: Project,
        configure: (KotlinWasmJsTargetDsl.() -> Unit),
    ) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            if (wasm) {
                @OptIn(ExperimentalWasmDsl::class)
                wasmJs {
                    nodejs()
                    configure()
                }
            }
        }
    }

    /**
     * Setup JS targets in the given project with the given configuration.
     *
     * @param project The Gradle project to configure.
     * @param configure A lambda to configure each JS target.
     */
    fun setupJSTarget(
        project: Project,
        configure: (KotlinJsTargetDsl.() -> Unit),
    ) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            if (js) {
                js {
                    configure()
                }
                js(IR) {
                    configure()
                }
            }
        }
    }


}
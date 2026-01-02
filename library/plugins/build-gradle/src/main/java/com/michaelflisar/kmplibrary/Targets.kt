package com.michaelflisar.kmplibrary

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.androidLibrary
import com.michaelflisar.kmplibrary.core.configs.Config
import com.michaelflisar.kmplibrary.core.configs.LibraryConfig
import com.michaelflisar.kmplibrary.core.Platform
import com.michaelflisar.kmplibrary.setups.AndroidLibrarySetup
import com.michaelflisar.kmplibrary.setups.WasmAppSetup
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
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import kotlin.text.toInt

class Targets(
    val android: Boolean = false,
    val iOS: Boolean = false,
    val windows: Boolean = false,
    val linux: Boolean = false,
    val macOS: Boolean = false,
    val wasm: Boolean = false,
    val js: Boolean = false,
) {
    private val enabledPlatforms = Platform.entries
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
            target.targets.forEach {
                sourceSets.getByName("${it}Main").dependsOn(sourceSet)
            }
        }
    }

    fun setupTargetsLibrary(
        project: Project,
        config: Config,
        libraryConfig: LibraryConfig,
        androidSetup: AndroidLibrarySetup?,
        configureAndroid: (KotlinMultiplatformAndroidLibraryTarget.() -> Unit) = {},
        configureIOS: (KotlinNativeTarget.() -> Unit) = {},
        configureIOSTests: (KotlinNativeTargetWithSimulatorTests.() -> Unit) = {},
        configureWindows: (KotlinJvmTarget.() -> Unit) = {},
        configureMacOS: (KotlinNativeTargetWithHostTests.() -> Unit) = {},
        configureLinux: (KotlinNativeTarget.() -> Unit) = {},
        configureWASM: (KotlinWasmJsTargetDsl.() -> Unit) = {},
        configureJS: (KotlinJsTargetDsl.() -> Unit) = {},
    ) {
        if (androidSetup == null && android) {
            throw IllegalArgumentException("AndroidSetup must be provided when Android target is enabled")
        }
        if (androidSetup != null)
            setupAndroidLibraryTarget(project, config,  libraryConfig, androidSetup, configureAndroid)
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
        wasmSetup: WasmAppSetup,
        configureAndroid: (KotlinAndroidTarget.() -> Unit) = {},
        configureIOS: (KotlinNativeTarget.() -> Unit) = {},
        configureIOSTests: (KotlinNativeTargetWithSimulatorTests.() -> Unit) = {},
        configureWindows: (KotlinJvmTarget.() -> Unit) = {},
        configureMacOS: (KotlinNativeTargetWithHostTests.() -> Unit) = {},
        configureLinux: (KotlinNativeTarget.() -> Unit) = {},
        configureWASM: (KotlinWasmJsTargetDsl.() -> Unit) = {},
        configureJS: (KotlinJsTargetDsl.() -> Unit) = {},
    ) {
        setupAndroidAppTarget(project, config, configureAndroid)
        setupIOSTarget(project, configureIOS, configureIOSTests)
        setupWindowsTarget(project, configureWindows)
        setupMacOSTarget(project, configureMacOS)
        setupLinuxTarget(project, configureLinux)
        setupWasmAppTarget(project, wasmSetup, configureWASM)
        setupJSTarget(project, configureJS)
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
        libraryConfig: LibraryConfig,
        androidSetup: AndroidLibrarySetup,
        configure: (KotlinMultiplatformAndroidLibraryTarget.() -> Unit) = {},
    ) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            if (android) {

                androidLibrary {

                    namespace = libraryConfig.library.namespace
                    compileSdk = androidSetup.compileSdk.get().toInt()
                    minSdk = androidSetup.minSdk.get().toInt()

                    compilerOptions {
                        jvmTarget.set(JvmTarget.fromTarget(config.javaVersion))
                    }

                    androidResources { enable = androidSetup.enableAndroidResources }

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

    fun setupXCFramework(
        project: Project,
        frameworkName: String,
        folderCInterop: File = project.file("iosXCFramework/cinterop"),
        folderXCFramework: File = project.file("iosXCFramework/${frameworkName}.xcframework"),
        relativeHeadersFolderInXCFramework: String = "Headers"
    ) {
        // helper function
        fun sliceDirFor(target: KonanTarget): String = when (target) {
            KonanTarget.IOS_ARM64 -> "ios-arm64"
            KonanTarget.IOS_X64 -> "ios-x86_64-simulator"
            KonanTarget.IOS_SIMULATOR_ARM64 -> "ios-arm64_x86_64-simulator"
            else -> error("Unsupported target: $target")
        }

        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            if (iOS) {



                listOf(
                    iosX64(),
                    iosArm64(),
                    iosSimulatorArm64()
                ).forEach { iosTarget ->

                    iosTarget.compilations.getByName("main") {

                        cinterops.create(frameworkName) {

                            // 1) add all .def files in the cinterop folder
                            val defFiles =
                                folderCInterop.listFiles { file: File -> file.extension == "def" }
                            defFiles.forEach { defFile(it) }

                            // 2) include header dirs (cinterop + provided Headers folder)
                            val sliceDir =
                                folderXCFramework.resolve(sliceDirFor(iosTarget.konanTarget))
                            includeDirs.allHeaders(
                                sliceDir.resolve("$frameworkName.framework/$relativeHeadersFolderInXCFramework"),
                                folderCInterop
                            )

                            // 3) compiler options
                            compilerOpts(
                                "-F", sliceDir.absolutePath,
                                "-framework", frameworkName
                            )
                        }
                    }
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
        wasmSetup: WasmAppSetup,
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
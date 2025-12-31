package com.michaelflisar.kmplibrary.readme

import com.michaelflisar.kmplibrary.core.configs.LibraryConfig

object ReadmeDefaults {

    val DefaultReadmeTemplate = """
        {{ header }}

        {{ partials.introduction }}

        {{ partials.features }}

        # :information_source: Table of Contents

        - [Screenshots](#camera-screenshots)
        - [Supported Platforms](#computer-supported-platforms)
        - [Setup](#wrench-setup)
        - [Usage](#page_facing_up-usage)
        - [Modules](#file_folder-modules)
        - [Demo](#diamonds-demo)
        - [More](#information_source-more)

        # :camera: Screenshots

        {{ screenshots }}

        # :computer: Supported Platforms

        {{ supported_platforms }}

        # :wrench: Setup

        <details>

        <summary>Dependencies</summary>

        <br>

        Simply add the dependencies inside your **build.gradle.kts** file.

        ```kotlin
        {{ setup-via-dependencies }}
        ```

        </details>

        <details>

        <summary>Version Catalogue</summary>

        <br>

        Define the dependencies inside your **libs.versions.toml** file.

        ```toml
        {{ setup-via-version-catalogue1 }}
        ```

        And then use the definitions in your projects **build.gradle.kts** file like following:

        ```shell
        {{ setup-via-version-catalogue2 }}
        ```

        </details>

        # :page_facing_up: Usage

        {{ partials.usage }}

        # :file_folder: Modules

        {{ modules }}

        # :diamonds: Demo

        A full [demo](/demo) is included inside the demo module, it shows nearly every usage with working examples.

        # :information_source: More

        {{ links }}

    """.trimIndent()

    val ImageAndroid = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/android-3DDC84?style=for-the-badge",
        altText = "Android"
    )
    val ImageIOS = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/ios-A2AAAD?style=for-the-badge",
        altText = "iOS"
    )
    val ImageWindows = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/windows-5382A1?style=for-the-badge",
        altText = "Windows"
    )
    val ImageMacOS = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/macos-B0B0B0?style=for-the-badge",
        altText = "macOS"
    )
    val ImageLinux = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/linux-FF6600?style=for-the-badge",
        altText = "Linux"
    )
    val ImageWASM = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/wasm-624DE7?style=for-the-badge",
        altText = "WebAssembly"
    )
    val ImageJS = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/javascript-F7DF1E?style=for-the-badge",
        altText = "JavaScript"
    )

    fun imageMavenCentral(libraryConfig: LibraryConfig) = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/maven-central/v/${libraryConfig.maven.groupId}/${libraryConfig.maven.primaryArtifactId}?style=for-the-badge&color=blue",
        altText = "Maven Central",
        linkUrl = "https://central.sonatype.com/artifact/${libraryConfig.maven.groupId}/${libraryConfig.maven.primaryArtifactId}"
    )

    fun imageAPI(minSdk: Int) = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/api-${minSdk}%2B-brightgreen.svg?style=for-the-badge",
        altText = "API"
    )

    fun imageKotlin(libraryConfig: LibraryConfig) = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/github/languages/top/${libraryConfig.developer.githubUserName}/${libraryConfig.library.name}.svg?style=for-the-badge&amp;color=blueviolet",
        altText = "Kotlin"
    )

    fun imageKMP() = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/badge/Kotlin_Multiplatform-blue?style=for-the-badge&amp;label=Kotlin",
        altText = "Kotlin Multiplatform"
    )

    fun imageLicence(libraryConfig: LibraryConfig) = UpdateReadmeUtil.markdownImage(
        imageUrl = "https://img.shields.io/github/license/${libraryConfig.developer.githubUserName}/${libraryConfig.library.name}?style=for-the-badge",
        altText = "License",
        linkUrl = "https://github.com/${libraryConfig.developer.githubUserName}/${libraryConfig.library.name}/blob/master/LICENSE"
    )
}
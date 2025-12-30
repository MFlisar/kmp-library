package com.michaelflisar.kmplibrary.readme

import com.michaelflisar.kmplibrary.configs.LibraryConfig

object ReadmeDefaults {

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
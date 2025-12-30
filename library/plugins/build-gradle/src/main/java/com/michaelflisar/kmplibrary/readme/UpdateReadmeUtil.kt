package com.michaelflisar.kmplibrary.readme

import java.io.File

object UpdateReadmeUtil {

    /**
     * Returns a Markdown string for an image, optionally wrapped in a link.
     * @param imageUrl The URL of the image (required)
     * @param altText The alt text for the image (optional, default: "")
     * @param linkUrl The URL to link to (optional, default: null)
     * @return Markdown string for the image, optionally as a link
     */
    fun markdownImage(imageUrl: String, altText: String = "", linkUrl: String? = null): String {
        val imageMarkdown = "![${altText}](${imageUrl})"
        return if (linkUrl != null) {
            "[${imageMarkdown}](${linkUrl})"
        } else {
            imageMarkdown
        }
    }

    /**
     * Reads a property from a TOML file.
     *
     * @param file The TOML file to read from.
     * @param region The region in the TOML file (e.g. "versions")
     * @param key The key to read (e.g. "minSDK")
     */
    fun readTOMLProperty(file: File, region: String, key: String): String {
        val content = file.readText(Charsets.UTF_8)
        val regionStart = content.indexOf("[$region]")
        if (regionStart == -1) {
            throw RuntimeException("Region [$region] not found in TOML file: ${file.path}")
        }
        val regionEnd =
            content.indexOf("[", regionStart + 1).let { if (it == -1) content.length else it }
        val regionContent = content.substring(regionStart, regionEnd)
        val regex = Regex("""$key\s*=\s*["']?([^"'\n\r]+)["']?""")
        val matchResult = regex.find(regionContent)
        return matchResult?.groups?.get(1)?.value
            ?: throw RuntimeException("Key '$key' not found in region [$region] of TOML file: ${file.path}")
    }

    /**
     * Parses the supported platforms from a module's build.gradle.kts file.
     *
     * content may look like this:
     * val buildTargets = Targets(
     *     // mobile
     *     android = true,
     *     iOS = true,
     *     // desktop
     *     windows = true,
     *     macOS = true,
     *     // web
     *     wasm = true
     * )
     *
     *
     * @param file The root directory of the module.
     * @return A list of supported platform names.
     */
    fun getSupportedPlatformsFromModule(file: File): List<String> {

        // 1) get buildTargets block
        val regex = Regex("val buildTargets = Targets\\((.*?)\\)", RegexOption.DOT_MATCHES_ALL)
        val matchResult = regex.find(file.readText())

        // 2) extract platforms
        val platforms = mutableListOf<String>()
        matchResult?.groups?.get(1)?.value
            ?.lines()
            ?.filter { !it.trim().startsWith("//") && it.contains("=") }
            ?.forEach {
            val parts = it.trim()
                .removeSuffix(",")
                .split("=")
            if (parts.size == 2) {
                val platformName = parts[0].trim()
                val isEnabled = parts[1].trim().toBoolean()
                if (isEnabled) {
                    platforms.add(platformName)
                }
            }
        }

        return platforms
    }
}
package com.michaelflisar.kmplibrary.readme

import com.michaelflisar.kmplibrary.configs.Config
import com.michaelflisar.kmplibrary.configs.LibraryConfig
import java.io.File

object UpdateReadmeUtil {

    fun update(
        rootDir: File,
        readmeTemplate: String,
        config: Config,
        libraryConfig: LibraryConfig,
    ) {
        println("")
        println("#####################################")
        println("###   BEGIN updateMarkdownFiles   ###")
        println("#####################################")
        println("")

        // files
        val fileAppVersionToml = File(rootDir, "gradle/app.versions.toml")
        val fileReadme = File(rootDir, "README.md")
        val folderDocumentation = File(rootDir, "documentation")
        val folderDocumentationModules = File(rootDir, "documentation/modules")
        val folderDocumentationScreenshots = File(rootDir, "documentation/screenshots")

        // load data from project files
        val minSdk = readTOMLProperty(fileAppVersionToml, "versions", "minSdk").toInt()
        val supportedPlatforms = libraryConfig.modules.map { module ->
            val platforms =
                getSupportedPlatformsFromModule(File(rootDir, "${module.path}/build.gradle.kts"))
            module to platforms
        }
        val allSupportedPlatforms = supportedPlatforms.map { it.second }.flatten().distinct()
        val allSupportedPlatformsLowercase = allSupportedPlatforms.map { it.lowercase() }

        val isAndroidSupported = allSupportedPlatformsLowercase.contains("android")
        val isIosSupported = allSupportedPlatformsLowercase.contains("ios")
        val isWindowsSupported = allSupportedPlatformsLowercase.contains("windows")
        val isMacOsSupported = allSupportedPlatformsLowercase.contains("macos")
        val isLinuxSupported = allSupportedPlatformsLowercase.contains("linux")
        val isWasmSupported = allSupportedPlatformsLowercase.contains("wasm")
        val isJsSupported = allSupportedPlatformsLowercase.contains("js")

        // 1) load all markdown files from the documentation folder (excluding _partials)
        val markdownTitleRegex = Regex("^#\\s+(.*)$", RegexOption.MULTILINE)
        //val pathDocumentation = "documentation"
        //val pathModules = "documentation/modules"
        val markdownFilesWithName = folderDocumentation
            .walkTopDown()
            .filter { it.isFile && it.extension == "md" }
            .filter { !it.path.contains("_partials") }
            .toList()
            .sortedBy { it.path }
            .map {
                val content = it.readText()
                // extract name from first markdown header
                val nameMatch = markdownTitleRegex.find(content)
                val name = nameMatch?.groups?.get(1)?.value?.trim() ?: it.nameWithoutExtension
                it to name
            }

        // 2) get all modules and other markdown files as links
        val moduleLinks = markdownFilesWithName
            .filter { it.first.startsWith(folderDocumentationModules) }
            .map {
                val relativePath = it.first.relativeTo(rootDir).path.replace("\\", "/")
                val (path, name) = it
                "- [$name]($relativePath)"
            }
        val otherLinks = markdownFilesWithName
            .filter { !it.first.startsWith(folderDocumentationModules) }
            .map {
                val relativePath = it.first.relativeTo(rootDir).path.replace("\\", "/")
                val (path, name) = it
                "- [$name]($relativePath)"
            }

        // 3) create header replacement
        val imageMavenCentral = ReadmeDefaults.imageMavenCentral(libraryConfig)
        val imageAPI = ReadmeDefaults.imageAPI(minSdk)
        val imageKotlin = ReadmeDefaults.imageKotlin(libraryConfig)
        val imageKMP = ReadmeDefaults.imageKMP()
        val imageLicence = ReadmeDefaults.imageLicence(libraryConfig)

        val headerLine1 = "$imageMavenCentral $imageAPI $imageKotlin $imageKMP $imageLicence"
        val headerLine2 = "# ${libraryConfig.library.name}"
        val headerLine3 = listOfNotNull(
            if (isAndroidSupported) ReadmeDefaults.ImageAndroid else null,
            if (isIosSupported) ReadmeDefaults.ImageIOS else null,
            if (isWindowsSupported) ReadmeDefaults.ImageWindows else null,
            if (isMacOsSupported) ReadmeDefaults.ImageMacOS else null,
            if (isLinuxSupported) ReadmeDefaults.ImageLinux else null,
            if (isWasmSupported) ReadmeDefaults.ImageWASM else null,
            if (isJsSupported) ReadmeDefaults.ImageJS else null,
        )
            .joinToString(" ")
        val header = listOf(
            headerLine1,
            headerLine2,
            headerLine3
        ).joinToString("\n")

        // 4) create supported platforms table
        val supportedPlatformsTable = buildString {
            val header = listOf("Module") + allSupportedPlatforms
            appendLine("| " + header.joinToString(" | ") + " |")
            appendLine("|" + header.joinToString("|") { "---" } + "|")
            for ((module, platforms) in supportedPlatforms) {
                val row = listOf(module.name) + allSupportedPlatformsLowercase.map { platform ->
                    if (platforms.map { it.lowercase() }
                            .contains(platform.lowercase())) "✅" else "❌"
                }
                appendLine("| " + row.joinToString(" | ") + " |")
            }
        }

        // 5) create setup instructions
        val libraryName = libraryConfig.library.name.lowercase().replace(" ", "-")
        val setupViaDependencies = buildString {
            appendLine("val $libraryName = \"<LATEST-VERSION>\"")
            appendLine()
            for (module in libraryConfig.modules) {
                appendLine("implementation(\"${libraryConfig.maven.groupId}:${module.artifactId}:\${$libraryName}\")")
            }
        }

        val setupViaVersionCatalogue1 = buildString {
            appendLine("[versions]")
            appendLine()
            appendLine("$libraryName = \"<LATEST-VERSION>\"")
            appendLine()
            appendLine("[libraries]")
            appendLine()
            for (module in libraryConfig.modules) {
                appendLine(
                    "${
                        module.artifactId.replace(
                            "-",
                            "."
                        )
                    } = { module = \"${libraryConfig.maven.groupId}:${module.artifactId}\", version.ref = \"$libraryName\" }"
                )
            }
        }

        val setupViaVersionCatalogue2 = buildString {
            for (module in libraryConfig.modules) {
                appendLine("implementation(libs.${module.artifactId.replace("-", ".")})")
            }
        }

        // 6) create screenshot replacement
        val screenshots = folderDocumentationScreenshots.listFiles().map {
            val relativePath = it.relativeTo(rootDir).path.replace("\\", "/")
            "![${it.nameWithoutExtension}]($relativePath)"
        }

        // 2) read template content
        var readmeContent = readmeTemplate

        // 3) replace placeholders in readme with content from markdown files
        val replacements = listOf(
            Placeholder("{{ header }}", header),
            Partial(
                "{{ partials.introduction }}",
                File(rootDir, "documentation/_partials/introduction.md.partial")
            ),
            Partial(
                "{{ partials.features }}",
                File(rootDir, "documentation/_partials/features.md.partial")
            ),
            Partial("{{ partials.usage }}", File(rootDir, "documentation/_partials/usage.md.partial")),
            Placeholder("{{ modules }}", moduleLinks.joinToString("\n")),
            Placeholder("{{ links }}", otherLinks.joinToString("\n")),
            Placeholder("{{ supported_platforms }}", supportedPlatformsTable),
            Placeholder("{{ setup-via-dependencies }}", setupViaDependencies),
            Placeholder("{{ setup-via-version-catalogue1 }}", setupViaVersionCatalogue1),
            Placeholder("{{ setup-via-version-catalogue2 }}", setupViaVersionCatalogue2),
            Placeholder("{{ screenshots }}", screenshots.joinToString("\n")),
        )
        for (replacement in replacements) {
            readmeContent = replacement.replace(readmeContent)
        }

        // 4) write updated readme content to README.md
        fileReadme.writeText(readmeContent)

        println("")
        println("#####################################")
        println("###   END updateMarkdownFiles     ###")
        println("#####################################")
    }

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
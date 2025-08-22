package com.michaelflisar.kmplibrary.docs

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.tree.nodes.TomlFile
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValuePrimitive
import com.michaelflisar.kmplibrary.SetupLibrary
import com.michaelflisar.kmplibrary.SetupModules
import com.michaelflisar.kmplibrary.SetupOtherProjects
import com.michaelflisar.kmplibrary.SetupOtherProjects.OtherProjectGroup
import com.michaelflisar.kmplibrary.Target
import com.michaelflisar.kmplibrary.classes.YamlValue
import com.michaelflisar.kmplibrary.deleteIfEmpty
import com.michaelflisar.kmplibrary.rootFolder
import com.michaelflisar.kmplibrary.saveDelete
import com.michaelflisar.kmplibrary.saveDeleteRecursively
import com.michaelflisar.kmplibrary.update
import com.michaelflisar.kmplibrary.walkTopDownFiltered
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.File
import java.util.Properties
import java.util.jar.JarFile

private const val PLACEHOLDER_CUSTOM_NAV = "# <CUSTOM-NAV>"
private const val PLACEHOLDER_MORE_NAV = "# <MORE-NAV>"

private const val PLACEHOLDER_INDEX_INFO_FEATURES = "# <INFO_FEATURES>"
private const val PLACEHOLDER_INDEX_INFO_PLATFORMS = "# <INFO_PLATFORMS>"
private const val PLACEHOLDER_INDEX_INFO_BOTTOM = "# <INFO_BOTTOM>"

private const val NAV_TO_IGNORE = "usage.md"

private const val REL_PATH_DOCS_CUSTOM_PARTS_INDEX_FEATURES = "parts/index_features.md"
private const val REL_PATH_DOCS_CUSTOM_PARTS_INDEX_PLATFORM_COMMENTS =
    "parts/index_platform_comments.md"
private const val REL_PATH_DOCS_CUSTOM_PARTS_INDEX_BOTTOM = "parts/index_bottom.md"

/**
 * Registers a Gradle task that generates documentation files based on the provided parameters and project properties.
 *
 * @param tasks The TaskContainer to register the task in.
 * @param project The Gradle project context.
 * @param name The name of the task to be registered. Default is "buildDocs".
 * @param relativePathDocsCustom The relative path to the custom documentation files. Default is "documentation/custom".
 * @param relativeModulesPath The relative path to the library modules. Default is "library".
 * @param relativeDemosPath The relative path to the demo projects. Default is "demo".
 * @param customOtherProjectsYamlUrl The URL to a custom YAML file containing other projects information. Default is "https://raw.githubusercontent.com/MFlisar/kmp-library/refs/heads/main/data/other-projects.yml".
 * @param defaultRelativePathGeneratedDocsOutput The default relative path where generated documentation files will be output. Default is "gen/docs".
 * @param multiplatform A flag indicating whether the library is multiplatform. Default is true.
 */
fun registerBuildDocsTasks(
    tasks: TaskContainer,
    project: Project,
    name: String = "buildDocs",
    relativePathDocsCustom: String = "documentation/custom",
    relativeModulesPath: String = "library",
    relativeDemosPath: String? = "demo",
    customOtherProjectsYamlUrl: String = "https://raw.githubusercontent.com/MFlisar/kmp-library/refs/heads/main/data/other-projects.yml",
    defaultRelativePathGeneratedDocsOutput: String = "gen/docs",
    multiplatform: Boolean = true
) {
    val generatedDocsDir = project.findProperty("generatedDocsDir") as String?
        ?: defaultRelativePathGeneratedDocsOutput
    tasks.registerBuildDocsTasks2(
        name = name,
        relativePathDocsCustom = relativePathDocsCustom,
        relativePathGeneratedDocsOutput = generatedDocsDir,
        relativeModulesPath = relativeModulesPath,
        relativeDemosPath = relativeDemosPath,
        customOtherProjectsYamlUrl = customOtherProjectsYamlUrl,
        multiplatform = multiplatform
    )
}

private fun TaskContainer.registerBuildDocsTasks2(
    name: String,
    relativePathDocsCustom: String,
    relativePathGeneratedDocsOutput: String,
    relativeModulesPath: String,
    relativeDemosPath: String?,
    customOtherProjectsYamlUrl: String,
    multiplatform: Boolean
) {
    register(name) { task ->
        task.doLast {
            buildDocs(
                relativePathDocsCustom = relativePathDocsCustom,
                relativePathGeneratedDocsOutput = relativePathGeneratedDocsOutput,
                relativeModulesPath = relativeModulesPath,
                relativeDemosPath = relativeDemosPath,
                customOtherProjectsYamlUrl = customOtherProjectsYamlUrl,
                multiplatform = multiplatform
            )
            println("Docs have been build!")
        }
    }
}

/*
 * generates the documentation files in the <relativePathGeneratedDocsOutput> folder
 *
 * automatically detects all gradle properties that start with "DOC_", "LIBRARY_" or "DEVELOPER_" and uses them as placeholders for the replacement logic
 */
private fun buildDocs(
    relativePathDocsCustom: String,
    relativePathGeneratedDocsOutput: String,
    relativeModulesPath: String,
    relativeDemosPath: String?,
    customOtherProjectsYamlUrl: String,
    multiplatform: Boolean
) {

    val ci = System.getenv("CI")?.toBoolean() == true
    val root = rootFolder()

    println("Building docs [ci = $ci]...")

    val documentationFolder = File(root, relativePathGeneratedDocsOutput)
    val docCustom = File(root, relativePathDocsCustom)

    println("Creating setup data...")
    val setupLibrary = SetupLibrary.read(root)
    val setupModules = SetupModules.read(root)
    val yamlValues = YamlValue.readYaml(SetupLibrary.file(root))
    var otherProjects: SetupOtherProjects? = null

    if (customOtherProjectsYamlUrl != null) {
        otherProjects = SetupOtherProjects.read({
            Jsoup.connect(customOtherProjectsYamlUrl).execute().body()
        }, customOtherProjectsYamlUrl)
    }

    // 0) check integrity of the setup
    for (module in setupModules.modules) {
        val group = module.group
        if (group != null) {
            if (setupModules.groups?.find { it.id == group } == null) {
                throw RuntimeException("Module '${module.artifactId}' has group '$group' but no group with this id is defined in the setup!")
            }
        }
    }

    // 1) copy all doc files from the template folder including the custom files
    println("1) Copy all doc files from the template folder including the custom files...")
    copyDoc(
        documentationFolder = documentationFolder,
        docCustom = docCustom
    )

    // 2) update all placeholders in the documentation files
    println("2) Update all placeholders in the documentation files...")
    updatePlaceholders(
        documentationFolder = documentationFolder,
        yamlValues = yamlValues
    )

    // 3) update placeholders with part files in index.md
    println("3) Update placeholders with part files in index.md...")
    updatePlaceholdersInIndexMd(
        documentationFolder = documentationFolder,
        docCustom = docCustom
    )

    // 4) add nav items
    // - modules
    // - migration
    // - rest...
    println("4) Adding nav items to mkdocs.yml...")
    updateCustomNav(
        documentationFolder = documentationFolder,
        docCustom = docCustom,
        prioritizedFolders = listOf("modules", "migration"),
        addOtherProjects = otherProjects != null,
        addAboutMe = setupLibrary.library.aboutMe
    )

    // 5) generate project.yaml
    println("5) Generate project.yaml...")
    generateProjectYaml(
        root = root,
        documentationFolder = documentationFolder,
        setupLibrary = setupLibrary,
        setupModules = setupModules,
        relativeModulesPath = relativeModulesPath,
        relativeDemosPath = relativeDemosPath,
        multiplatform = multiplatform
    )

    // 6) generate other-projects.yaml
    println("6) Generate other-projects.yaml...")
    generateOtherProjectsYaml(
        documentationFolder = documentationFolder,
        otherProjects = otherProjects?.otherProjects ?: emptyList()
    )

    println("Building docs finished successfully!")
}

private fun copyDoc(
    documentationFolder: File,
    docCustom: File,
) {
    // 1) delete the old documentation folder
    documentationFolder.mkdirs()
    documentationFolder.saveDeleteRecursively()

    // 2) copy the template folder
    val jarUrl = BuildGradleFile::class.java.protectionDomain.codeSource.location
    val jarFile = JarFile(File(jarUrl.toURI()))
    copyTemplateFromJar(jarFile, "template/", documentationFolder)
    //docTemplateFolder.copyRecursively(documentationFolder, overwrite = true)

    // 3) copy the custom folder
    val files = docCustom.walkTopDownFiltered { it.isFile }
    for (f in files) {
        val relativePath = f.relativeTo(docCustom).path.cleanFileName()
        val targetFile = File(documentationFolder, relativePath)
        targetFile.parentFile.mkdirs()
        f.copyTo(targetFile, overwrite = true)
    }
    //docCustom.copyRecursively(documentationFolder, overwrite = false)
    //documentationFolder
    //    .walkTopDownFiltered { it.isFile }
    //    .forEach {
    //        val renamed = it.name.cleanFileName()
    //        if (renamed != it.name) {
    //            val newFile = File(it.parentFile, renamed)
    //            it.renameTo(newFile)
    //        }
    //    }

    // 4) delete parts folders
    val partFeatures = File(documentationFolder, REL_PATH_DOCS_CUSTOM_PARTS_INDEX_FEATURES)
    val partPlatformFeatures =
        File(documentationFolder, REL_PATH_DOCS_CUSTOM_PARTS_INDEX_PLATFORM_COMMENTS)
    val partBottom = File(documentationFolder, REL_PATH_DOCS_CUSTOM_PARTS_INDEX_BOTTOM)
    partFeatures.saveDelete()
    partPlatformFeatures.saveDelete()
    partBottom.saveDelete()
    partPlatformFeatures.parentFile.deleteIfEmpty()
}

private fun updatePlaceholders(
    documentationFolder: File,
    yamlValues: List<YamlValue>
) {
    // 4) iterate the generated documentation files and replace the placeholders
    documentationFolder.walkTopDown().forEach { file ->
        if (file.isFile) {
            val originalContent = file.readText()
            var content = originalContent
            for (yamlValue in yamlValues) {
                val placeholder = "<${yamlValue.path}>"
                val value = yamlValue.value
                content = content.replace(placeholder, value)
            }
            if (content != originalContent)
                file.writeText(content)
        }
    }
}

private fun updatePlaceholdersInIndexMd(
    documentationFolder: File,
    docCustom: File,
) {
    val file = File(documentationFolder, "docs/index.md")

    val partFeatures = File(docCustom, REL_PATH_DOCS_CUSTOM_PARTS_INDEX_FEATURES)
    val partPlatformComments = File(docCustom, REL_PATH_DOCS_CUSTOM_PARTS_INDEX_PLATFORM_COMMENTS)
    val partBottom = File(docCustom, REL_PATH_DOCS_CUSTOM_PARTS_INDEX_BOTTOM)

    val features = partFeatures.takeIf { it.exists() }?.readText(Charsets.UTF_8) ?: ""
    val platformComments =
        partPlatformComments.takeIf { it.exists() }?.readText(Charsets.UTF_8) ?: ""
    val bottom = partBottom.takeIf { it.exists() }?.readText(Charsets.UTF_8) ?: ""

    file.update(PLACEHOLDER_INDEX_INFO_FEATURES, features)
    file.update(PLACEHOLDER_INDEX_INFO_PLATFORMS, platformComments)
    file.update(PLACEHOLDER_INDEX_INFO_BOTTOM, bottom)
}

private fun updateCustomNav(
    documentationFolder: File,
    docCustom: File,
    prioritizedFolders: List<String>,
    addOtherProjects: Boolean,
    addAboutMe: Boolean
) {
    val file = File(documentationFolder, "mkdocs.yml")

    val navFolder = File(docCustom, "docs")
    var mdFiles = navFolder
        .walkTopDownFiltered {
            it.isFile &&
                    it.extension == "md"
                    && it.name != NAV_TO_IGNORE
        }.toList()
    mdFiles = walkTopDownWithPrioritizedFoldersOnTop(
        files = mdFiles,
        folder = navFolder,
        prioritizedFolders = prioritizedFolders,
    )
    val customNavFiles = mdFiles
        .map {
            NavItem(navFolder, it)
        }

    val customNavLines = ArrayList<String>()
    val existingPaths = ArrayList<String>()
    with(customNavLines) {
        for (nav in customNavFiles) {
            if (!existingPaths.contains(nav.navPath)) {
                val allPaths = nav.allFolderPathsTopDown().toMutableList()
                //println("nav: level = ${nav.folderPathElements.size} | name = ${nav.name} | path = ${nav.path}")
                //for (p in allPaths) {
                //    println("  - allPaths: level = ${p.level} | name = ${p.name} | path = ${p.path}")
                //}
                //println("  - existingPaths.contains(allPaths.first().path = ${if (allPaths.isEmpty()) "EMPTY" else existingPaths.contains(allPaths.first().path)}")
                while (allPaths.isNotEmpty()) {
                    val p = allPaths.removeFirst()
                    if (!existingPaths.contains(p.path)) {
                        add("  ".repeat(p.level + 1) + "- ${p.name}:")
                        existingPaths += p.path
                        //println("path added: level = ${p.level} | name = ${p.name} | path = ${p.path}")
                    }
                }
            }
            add("  ".repeat(nav.folderPathElements.size + 1) + "- ${nav.name}: ${nav.navPath}")
        }
    }

    val replacement = customNavLines.joinToString("\n")
    file.update(PLACEHOLDER_CUSTOM_NAV, replacement)

    var replacement2 =
        listOfNotNull(
            "    - Other Libraries: other-libraries.md".takeIf { addOtherProjects },
            "    - About Me: me.md".takeIf { addAboutMe }
        )
    if (replacement2.isEmpty()) {
        file.update(PLACEHOLDER_MORE_NAV, "")
    } else {
        replacement2 = listOf("  - More:") + replacement2
        file.update(PLACEHOLDER_MORE_NAV, replacement2.joinToString("\n"))
    }

}

private fun generateProjectYaml(
    root: File,
    documentationFolder: File,
    setupLibrary: SetupLibrary,
    setupModules: SetupModules,
    relativeModulesPath: String,
    relativeDemosPath: String?,
    multiplatform: Boolean
) {
    val file = File(documentationFolder, "_data/project.yml")
    val allModuleBuildGradleFile =
        File(root, relativeModulesPath).walkTopDownFiltered { it.name == "build.gradle.kts" }
            .toList()
            .map {
                BuildGradleFile(root, it)
            }
    val allModuleKtFile =
        File(root, relativeModulesPath).walkTopDownFiltered { it.extension == "kt" }.toList()
    val tomlApp = loadToml(root, "app.versions.toml")
    val tomlLibs = loadToml(root, "libs.versions.toml")

    // data library
    val id = setupLibrary.library.id
    val siteName = setupLibrary.library.name
    val minSdk = tomlApp.findKey("versions", "minSdk")
    val repoName = setupLibrary.library.repoName
    val maven = setupLibrary.maven.groupId
    val mavenArtifact = setupLibrary.maven.primaryArtifactId
    val supportedPlatforms = allModuleBuildGradleFile
        .map { it.platforms }
        .flatten()
        .distinct()
    val screenshots = setupLibrary.library.screenshots
    val branch = File(root, ".git/HEAD")
        .readText(Charsets.UTF_8)
        .let { content ->
            val prefix = "ref: refs/heads/"
            if (content.startsWith(prefix)) {
                content.removePrefix(prefix).trim()
            } else {
                null // Detached HEAD oder Commit-Hash
            }
        }

    val demo = relativeDemosPath?.let { File(root, it) }?.takeIf { it.exists() }

    // data dependencies
    val versionCompose = tomlLibs.tryFindKey("versions", "compose")
    var versionAndroidXMaterial3: String? = null
    var versionAndroidXComposeRuntime: String? = null
    if (versionCompose != null) {
        val urlMaterial3 =
            "https://repo1.maven.org/maven2/org/jetbrains/compose/material3/material3/${versionCompose}/material3-${versionCompose}.pom"
        val urlRuntime =
            "https://repo1.maven.org/maven2/org/jetbrains/compose/runtime/runtime/${versionCompose}/runtime-${versionCompose}.pom"

        versionAndroidXMaterial3 = findVersionInPOM(urlMaterial3, "androidx.compose.material3")
            ?: throw RuntimeException("Version for 'androidx.compose.material3' not found!")
        versionAndroidXComposeRuntime = findVersionInPOM(urlRuntime, "androidx.compose.runtime")
            ?: throw RuntimeException("Version for 'androidx.compose.runtime' not found!")
    }
    val experimental = allModuleKtFile
        .filter {
            val found = it.readText(Charsets.UTF_8).lines().any { line ->
                line.contains("@Experimental") ||
                        line.contains("@OptIn(Experimental")
            }
            if (found) {
                println("Experimental found in ${it.path}")
            }
            found
        }
        .toList()
        .isNotEmpty()

    val content = buildString {
        appendLine("# -------")
        appendLine("# Library")
        appendLine("# -------")
        appendLine("")
        appendLine("library:")
        appendLine("  id: $id")
        appendLine("  name: $siteName")
        appendLine("  api: $minSdk")
        appendLine("  github: $repoName")
        appendLine("  maven: $maven")
        appendLine("  maven-main-library: $mavenArtifact")
        appendLine("  multiplatform: $multiplatform")
        if (!multiplatform) {
            val p = Target.ANDROID
            appendLine("  platforms:")
            appendLine("    - name: ${p.targetName}")
            appendLine("      color: ${p.color}")
        } else if (supportedPlatforms.isEmpty()) {
            appendLine("  platforms: []")
        } else {
            appendLine("  platforms:")
            for (p in supportedPlatforms) {
                appendLine("    - name: ${p.targetName}")
                appendLine("      color: ${p.color}")
            }
        }
        if (screenshots.isEmpty()) {
            appendLine("  screenshots: []")
        } else {
            appendLine("  screenshots:")
            for (s in screenshots) {
                appendLine("    - name: ${s.name}")
                appendLine("      images:")
                for (i in s.images) {
                    appendLine("        - $i")
                }
            }
        }

        appendLine("  branch: $branch")
        if (demo != null) {
            appendLine("  demo-path: $relativeDemosPath")
        }
        appendLine("")
        appendLine("# -------")
        appendLine("# Dependencies")
        appendLine("# -------")
        appendLine("")
        appendLine("dependencies:")
        appendLine("  experimental: $experimental")
        if (versionCompose != null) {
            if (multiplatform) {
                appendLine("  compose-multiplatform: $versionCompose # https://github.com/JetBrains/compose-multiplatform/releases")
            } else {
                appendLine("  compose: $versionCompose # https://developer.android.com/jetpack/androidx/releases/compose")
            }
            appendLine("  jetpack-compose-runtime: $versionAndroidXComposeRuntime # https://developer.android.com/jetpack/androidx/releases/compose-runtime")
            appendLine("  jetpack-compose-material3: $versionAndroidXMaterial3 # https://developer.android.com/jetpack/androidx/releases/compose-material3")
        }
        appendLine("")
        appendLine("# -------")
        appendLine("# Groups")
        appendLine("# -------")
        appendLine("")
        if (setupModules.groups == null) {
            appendLine("# no groups defined")
        } else {
            appendLine("groups:")
            setupModules.groups!!.forEach {
                appendLine("  - id: ${it.id}")
                appendLine("    label: ${it.label}")
                appendLine("    gradle-comment: ${it.gradleComment}")
            }
        }
        appendLine("")
        appendLine("# -------")
        appendLine("# Modules")
        appendLine("# -------")
        appendLine("")
        appendLine("modules:")
        setupModules.modules.forEach { module ->
            val buildGradleFile =
                allModuleBuildGradleFile.find { it.relativeModulePath.normalizePath() == module.relativePath.normalizePath() }
                    ?: throw RuntimeException("BuildGradleFile not found for module: ${module.relativePath} in ${allModuleBuildGradleFile.map { it.relativeModulePath }}")
            appendLine("  - id: ${module.artifactId}")
            appendLine("    group: ${module.group}")
            appendLine("    description: ${module.description}")
            appendLine("    optional: ${module.optional}")
            appendArray("    ", "platforms", buildGradleFile.platforms.map { it.targetName })
            appendLine("    platform-info: \"${module.platformInfo ?: ""}\"")
            if (module.dependencies?.isNotEmpty() == true) {
                appendLine("    dependencies:")
                module.dependencies!!.forEach { dep ->
                    val toml = loadToml(root, dep.versionsFile)
                    val version = toml.findKey("versions", dep.versionsKey)
                    appendLine("      - name: ${dep.name}")
                    appendLine("        link: ${dep.link}")
                    appendLine("        version: $version")
                }
            } else {
                appendLine("    dependencies: []")
            }
        }
    }

    file.parentFile.mkdirs()
    file.delete()
    file.writeText(content, Charsets.UTF_8)
}

private fun generateOtherProjectsYaml(
    documentationFolder: File,
    otherProjects: List<OtherProjectGroup>
) {
    val file = File(documentationFolder, "_data/other-projects.yml")

    if (otherProjects.isEmpty()) {
        file.saveDelete()
        return
    }

    val content = buildString {
        appendLine("libraries:")
        for (group in otherProjects) {
            appendLine("  ${group.group}:")
            for (other in group.projects) {
                appendLine("    - name: ${other.name}")
                appendLine("      link: ${other.link}")
                if (other.image != null) {
                    appendLine("      image: ${other.image}")
                }
                appendLine("      maven: ${other.maven}")
                appendLine("      description: ${other.description}")
            }
        }
    }

    file.parentFile.mkdirs()
    file.delete()
    file.writeText(content, Charsets.UTF_8)
}

// ----------------------------
// helper functions
// ----------------------------

private fun copyTemplateFromJar(jarFile: JarFile, templatePrefix: String, targetDir: File) {
    val entries = jarFile.entries()
    while (entries.hasMoreElements()) {
        val entry = entries.nextElement()
        if (entry.name.startsWith(templatePrefix) && !entry.isDirectory) {
            val outFile = File(targetDir, entry.name.removePrefix(templatePrefix))
            outFile.parentFile.mkdirs()
            jarFile.getInputStream(entry).use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}

private fun walkTopDownWithPrioritizedFoldersOnTop(
    files: List<File>,
    folder: File,
    prioritizedFolders: List<String>,
): List<File> {

    val prioritizedPaths = prioritizedFolders.map { File(folder, it).canonicalPath }
    val prioritized = mutableListOf<File>()
    val rest = mutableListOf<File>()

    files.forEach { file ->
        val canonical = file.canonicalPath
        val isPrioritized = prioritizedPaths.any { prioPath ->
            canonical.startsWith(prioPath + File.separator) || canonical == prioPath
        }
        if (isPrioritized) {
            prioritized += file
        } else {
            rest += file
        }
    }

    val prioritizedSorted = prioritizedFolders.flatMap { prio ->
        val prioRoot = File(folder, prio).canonicalFile
        prioritized.filter { it.canonicalPath.startsWith(prioRoot.canonicalPath) }
            .sortedBy { it.canonicalPath }
    }
    return prioritizedSorted + rest
}

private fun Properties.getString(key: String): String {
    return getProperty(key) as String
}

private fun loadToml(root: File, fileName: String): TomlFile {
    return Toml.tomlParser.parseString(
        File(
            root,
            "gradle/$fileName"
        ).readText(Charsets.UTF_8)
    )
}

private fun TomlFile.findKey(table: String, key: String): String {
    val table = findTableInAstByName(table)
        ?: throw RuntimeException("Table '$table' not found in TOML file")
    val key = table.children.find { it.name == key }
        ?: throw RuntimeException("Key '$key' not found in TOML table '$table'")
    return (key as TomlKeyValuePrimitive).value.content.toString()
}

private fun TomlFile.tryFindKey(table: String, key: String): String? {
    val table = findTableInAstByName(table) ?: return null
    val key = table.children.find { it.name == key } ?: return null
    return (key as TomlKeyValuePrimitive).value.content.toString()
}

private fun String.findBetween(from: String, to: String): String? {
    val start = this.indexOf(from)
    if (start == -1) return null
    val fromEnd = start + from.length
    val end = this.indexOf(to, fromEnd)
    if (end == -1) return null
    return this.substring(fromEnd, end)
}

private fun String.removeComments(): String {
    val singleLine = Regex("//.*?$", RegexOption.MULTILINE)
    val multiLine = Regex("/\\*.*?\\*/", setOf(RegexOption.DOT_MATCHES_ALL))
    return this.replace(multiLine, "").replace(singleLine, "")
}

private fun String.splitOrEmpty(delimiter: String): List<String> {
    return if (this.isEmpty()) {
        emptyList()
    } else {
        this.split(delimiter)
    }
}

private fun StringBuilder.appendArray(inset: String, key: String, values: List<String>) {
    if (values.isEmpty()) {
        appendLine("$inset$key: []")
    } else {
        appendLine("$inset$key:")
        for (value in values) {
            appendLine("$inset  - $value")
        }
    }
}

private fun findVersionInPOM(url: String, groupId: String): String? {
    val jsoup = Jsoup.connect(url)
        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
        .get()

    val doc = Jsoup.parse(jsoup.outerHtml(), "", Parser.xmlParser())
    val elements = doc.select("dependencies")
        .map {
            it.select("dependency").toList()
        }
        .flatten()

    return elements.filter {
        it.select("groupId").text() == groupId
    }.firstOrNull()?.select("version")?.firstOrNull()?.text()
}

private fun findKotlinFunctionNamedParameters(
    content: String,
    functionName: String,
): Map<String, String> {
    val parameters = content.findBetween("$functionName(", ")")
        ?.removeComments()
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { !it.startsWith("//") }
    return parameters?.mapNotNull {
        val parts = it.split("=").map { it.trim() }
        if (parts.size != 2) {
            return@mapNotNull null
        }
        parts[0] to parts[1].removeSurrounding("\"")
    }?.toMap() ?: emptyMap()
}

private fun String.normalizePath(): String {
    return replace("\\", "/")
}

private fun String.cleanFileName(): String {
    return replace(" ", "-").lowercase()
}

// ----------------------------
// Classes
// ----------------------------

class NavItem(
    val root: File,
    val file: File,
) {
    val relativeFilePath = file.relativeTo(root).path
    val relativeFolderPath = file.parentFile.relativeTo(root).path
    val folderPathElements = relativeFolderPath.split(File.separatorChar).filter { it.isNotEmpty() }

    val name = file.nameWithoutExtension
    val navPath = relativeFilePath.replace(File.separatorChar, '/').cleanFileName()

    fun allFolderPathsTopDown(): List<Path> {
        if (relativeFolderPath.isEmpty())
            return emptyList()
        return List(folderPathElements.size) { index ->
            val path =
                folderPathElements.subList(0, index + 1).joinToString(File.separatorChar.toString())
            Path(path, index, folderPathElements[index])
        }
    }
}

class Path(
    val path: String,
    val level: Int,
    val name: String,
)

class BuildGradleFile(
    root: File,
    file: File,
) {
    private val content = file.readText(Charsets.UTF_8)

    val relativeModulePath = file.parentFile.relativeTo(root).path

    val platforms by lazy {
        val targets = findKotlinFunctionNamedParameters(content, "Targets")
        targets.filter { it.value == "true" }
            .map { Target.parseParameterName(it.key) }
    }
}
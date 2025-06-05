package com.michaelflisar.scripts

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.tree.nodes.TomlFile
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValuePrimitive
import com.michaelflisar.buildlogic.shared.Setup
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.File
import java.util.Properties
import com.michaelflisar.buildlogic.shared.SetupData

val PLACEHOLDER_CUSTOM_NAV = "# <CUSTOM-NAV>"
val PLACEHOLDER_INDEX_INFO_FEATURES = "# <INFO_FEATURES>"
val PLACEHOLDER_INDEX_INFO_PLATFORMS = "# <INFO_PLATFORMS>"

val NAV_TO_IGNORE = "usage.md"

val REL_PATH_DOCS_OUTPUT = "generator/generated/docs" // must match working-directory in build-mkdocs.yaml
val REL_PATH_DOCS_CUSTOM = "generator/docs-custom"
val REL_PATH_DOCS_TEMPLATE = "generator/docs-template"

/*
 * generates the documentation files in the "documentation" folder
 *
 * automatically detects all gradle properties that start with "DOC_", "LIBRARY_" or "DEVELOPER_" and uses them as placeholders for the replacement logic
 */
fun main() {

    val root = rootFolder()
    val documentationFolder = File(root, REL_PATH_DOCS_OUTPUT)
    val docTemplateFolder = File(root, REL_PATH_DOCS_TEMPLATE)
    val docCustom = File(root, REL_PATH_DOCS_CUSTOM)

    val setupData = SetupData.read(root)

    // 1) copy all doc files from the template folder including the custom files
    copyDoc(
        documentationFolder = documentationFolder,
        docTemplateFolder = docTemplateFolder,
        docCustom = docCustom
    )

    // 2) update all placeholders in the documentation files
    updatePlaceholders(
        documentationFolder = documentationFolder,
        setupData = setupData
    )

    // 3) add nav items
    // - modules
    // - migration
    // - rest...
    updateCustomNav(
        documentationFolder = documentationFolder,
        docCustom = docCustom,
        prioritizedFolders = listOf("modules", "migration"),
    )

    // 4) generate project.yaml => this reads data from ...
    generateProjectYaml(
        root = root,
        documentationFolder = documentationFolder,
        setup = setupData.setup
    )
}

private fun copyDoc(
    documentationFolder: File,
    docTemplateFolder: File,
    docCustom: File
) {
    // 1) delete the old documentation folder
    documentationFolder.saveDeleteRecursively()

    // 2) copy the template folder
    docTemplateFolder.copyRecursively(documentationFolder, overwrite = true)

    // 3) copy the custom folder
    docCustom.copyRecursively(documentationFolder, overwrite = false)
}

private fun updatePlaceholders(
    documentationFolder: File,
    setupData: SetupData
) {
    // 4) iterate the generated documentation files and replace the placeholders
    documentationFolder.walkTopDown().forEach { file ->
        if (file.isFile) {
            var originalContent = file.readText()
            var content = originalContent
            for (yamlValue in setupData.yaml) {
                val placeholder = "<${yamlValue.path}>"
                val value = yamlValue.value
                content = content.replace(placeholder, value)
            }
            if (content != originalContent)
                file.writeText(content)
        }
    }
}

private fun updateCustomNav(
    documentationFolder: File,
    docCustom: File,
    prioritizedFolders: List<String>
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
}

private fun generateProjectYaml(
    root: File,
    documentationFolder: File,
    setup: Setup
) {
    val file = File(documentationFolder, "_data/project.yml")
    val allModuleBuildGradleFile =
        File(root, "library/modules").walkTopDownFiltered { it.name == "build.gradle.kts" }.toList()
    val allModuleKtFile =
        File(root, "library/modules").walkTopDownFiltered { it.extension == "kt" }.toList()
    val tomlApp =
        Toml.tomlParser.parseString(File(root, "gradle/app.versions.toml").readText(Charsets.UTF_8))
    val tomlLibs = Toml.tomlParser.parseString(
        File(
            root,
            "gradle/libs.versions.toml"
        ).readText(Charsets.UTF_8)
    )

    // data library
    val siteName = setup.library.name
    val minSdk = tomlApp.findKey("versions", "minSdk")
    val repoName = setup.library.repoName
    val maven = setup.maven.groupId
    val mavenArtifact = setup.maven.primaryArtifactId
    val multiplatform =
        "true" // TODO: template is multiplatform... would need another base template???
    val supportedPlatforms = allModuleBuildGradleFile
        .map {
            val content = it.readText(Charsets.UTF_8)
            val buildTargets =
                content.findBetween("Targets(", ")")!!.removeComments().split(",").map { it.trim() }
            val platforms = buildTargets.mapNotNull {
                val parts = it.split("=").map { it.trim() }
                if (parts[1] == "true") {
                    parts[0]
                } else {
                    null
                }
            }
            platforms
        }
        .flatten()
        .distinct()
    val screenshots = setup.library.screenshots
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
    val demoPath = "library/demo"
    val demo = File(root, demoPath).exists()

    // data dependencies
    val versionCMP = tomlLibs.findKey("versions", "compose")
    var versionAndroidXMaterial3: String? = null
    var versionAndroidXComposeRuntime: String? = null
    val composeMultiplatformVersions = if (multiplatform != null) {

        val urlMaterial3 =
            "https://repo1.maven.org/maven2/org/jetbrains/compose/material3/material3/${versionCMP}/material3-${versionCMP}.pom"
        val urlRuntime =
            "https://repo1.maven.org/maven2/org/jetbrains/compose/runtime/runtime/${versionCMP}/runtime-${versionCMP}.pom"

        versionAndroidXMaterial3 =
            findVersionInPOM(urlMaterial3, "androidx.compose.material3")
                ?: throw RuntimeException("Version not found!")
        versionAndroidXComposeRuntime =
            findVersionInPOM(urlRuntime, "androidx.compose.runtime")
                ?: throw RuntimeException("Version not found!")
    } else null
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
        appendLine("  name: $siteName")
        appendLine("  api: $minSdk")
        appendLine("  github: $repoName")
        appendLine("  maven: $maven")
        appendLine("  maven-main-library: $mavenArtifact")
        appendLine("  multiplatform: $multiplatform")
        appendLine("  platforms:")
        for (p in supportedPlatforms) {
            appendLine("    - $p")
        }
        if (screenshots.isEmpty()) {
            appendLine("  screenshots: []")
        } else {
            appendLine("  screenshots:")
            for (screenshot in screenshots) {
                appendLine("    - $screenshot")
            }
        }
        appendLine("  branch: $branch")
        if (demo) {
            appendLine("  demo-path: $demoPath")
        }
        appendLine("# -------")
        appendLine("# Dependencies")
        appendLine("# -------")
        appendLine("")
        appendLine("dependencies:")
        appendLine("  compose-multiplatform: $versionCMP # https://github.com/JetBrains/compose-multiplatform/releases")
        appendLine("  jetpack-compose-runtime: $versionAndroidXComposeRuntime # https://developer.android.com/jetpack/androidx/releases/compose-runtime")
        appendLine("  jetpack-compose-material3: $versionAndroidXMaterial3 # https://developer.android.com/jetpack/androidx/releases/compose-material3")
        appendLine("  experimental: $experimental")
        appendLine("")
        appendLine("# -------")
        appendLine("# Groups")
        appendLine("# -------")
        appendLine("")

        appendLine("")
        appendLine("# -------")
        appendLine("# Modules")
        appendLine("# -------")
        appendLine("")

    }

    file.delete()
    file.writeText(content, Charsets.UTF_8)
}

// ----------------------------
// helper functions
// ----------------------------

private fun walkTopDownWithPrioritizedFoldersOnTop(
    files: List<File>,
    folder: File,
    prioritizedFolders: List<String>
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

fun Properties.getString(key: String): String {
    return getProperty(key) as String
}

fun TomlFile.findKey(table: String, key: String): String {
    return (findTableInAstByName(table)!!.children.find { it.name == key } as TomlKeyValuePrimitive).value.content.toString()
}

fun String.findBetween(from: String, to: String): String? {
    val start = this.indexOf(from)
    if (start == -1) return null
    val fromEnd = start + from.length
    val end = this.indexOf(to, fromEnd)
    if (end == -1) return null
    return this.substring(fromEnd, end)
}

fun String.removeComments(): String {
    val singleLine = Regex("//.*?$", RegexOption.MULTILINE)
    val multiLine = Regex("/\\*.*?\\*/", setOf(RegexOption.DOT_MATCHES_ALL))
    return this.replace(multiLine, "").replace(singleLine, "")
}

fun String.splitOrEmpty(delimiter: String): List<String> {
    return if (this.isEmpty()) {
        emptyList()
    } else {
        this.split(delimiter)
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

// ----------------------------
// Classes
// ----------------------------

class DocsPlaceholder(
    val key: String
) {
    val replacement: String = "<$key>"

    fun getValue(gradleProperties: Properties): String {
        return gradleProperties.getProperty(key) as String
    }
}

class NavItem(
    val root: File,
    val file: File
) {
    val relativeFilePath = file.relativeTo(root).path
    val relativeFolderPath = file.parentFile.relativeTo(root).path
    val folderPathElements = relativeFolderPath.split(File.separatorChar).filter { it.isNotEmpty() }

    val name = file.nameWithoutExtension
    val navPath = relativeFilePath.replace(File.separatorChar, '/')

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
    val name: String
)


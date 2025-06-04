package com.michaelflisar.scripts

import java.io.File
import java.util.Properties

val PLACEHOLDER_CUSTOM_NAV = "# <CUSTOM-NAV>"
val PLACEHOLDER_INDEX_INFO_FEATURES = "# <INFO_FEATURES>"
val PLACEHOLDER_INDEX_INFO_PLATFORMS = "# <INFO_PLATFORMS>"

val NAV_TO_IGNORE = "usage.md"

/*
 * generates the documentation files in the "documentation" folder
 *
 * automatically detects all gradle properties that start with "DOC_", "LIBRARY_" or "DEVELOPER_" and uses them as placeholders for the replacement logic
 */
fun main() {

    val root = rootFolder()
    val documentationFolder = File(root, "documentation")
    val docTemplateFolder = File(root, "scripts/docs-template")
    val docCustom = File(root, "scripts/docs-custom")

    val gradleProperties = gradleProperties(root)
    val placeholders = gradleProperties.toList().filter {
        it.first.toString().startsWith("DOC_") ||
                it.first.toString().startsWith("LIBRARY_") ||
                it.first.toString().startsWith("DEVELOPER_")
    }.map {
        DocsPlaceholder(it.first.toString())
    }

    // 1) copy all doc files from the template folder including the custom files
    copyDoc(
        documentationFolder = documentationFolder,
        docTemplateFolder = docTemplateFolder,
        docCustom = docCustom
    )

    // 2) update all placeholders in the documentation files
    updatePlaceholders(
        documentationFolder = documentationFolder,
        gradleProperties = gradleProperties,
        placeholders = placeholders
    )

    // 3) add nav items
    // - modules
    // - migration
    // - rest...
    updateCustomNav(
        documentationFolder = documentationFolder,
        docCustom = docCustom
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
    gradleProperties: Properties,
    placeholders: List<DocsPlaceholder>
) {
    // 4) iterate the generated documentation files and replace the placeholders
    documentationFolder.walkTopDown().forEach { file ->
        if (file.isFile) {
            var originalContent = file.readText()
            var content = originalContent
            for (placeholder in placeholders) {
                content =
                    content.replace(placeholder.replacement, placeholder.getValue(gradleProperties))
            }
            if (content != originalContent)
                file.writeText(content)
        }
    }
}

private fun updateCustomNav(
    documentationFolder: File,
    docCustom: File
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
        prioritizedFolders = listOf("modules", "migration"),
    )
    val customNavFiles = mdFiles
        .map {
            NavItem(navFolder, it)
        }

    val customNavLines = ArrayList<String>()
    val existingPaths = ArrayList<String>()
    with(customNavLines) {
        for (nav in customNavFiles) {
            if (!existingPaths.contains(nav.path)) {
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
            add("  ".repeat(nav.folderPathElements.size + 1) + "- ${nav.name}: ${nav.path}")
        }
    }

    val replacement = customNavLines.joinToString("\n")
    file.update(PLACEHOLDER_CUSTOM_NAV, replacement)
}

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
    val path = relativeFilePath

    fun allFolderPathsTopDown(): List<Path> {
        if (relativeFolderPath.isEmpty())
            return emptyList()
        return List(folderPathElements.size) { index ->
            val path = folderPathElements.subList(0, index + 1).joinToString(File.separatorChar.toString())
            Path(path, index, folderPathElements[index])
        }
    }
}

class Path(
    val path: String,
    val level: Int,
    val name: String
)

fun walkTopDownWithPrioritizedFoldersOnTop(
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
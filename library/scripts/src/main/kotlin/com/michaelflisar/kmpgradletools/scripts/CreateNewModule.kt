package com.michaelflisar.kmpgradletools.scripts

import java.io.File

/*
 * example:
 * createNewModule(
        rootDir = rootDir,
        folder = "app/feature",
        baseFeatureName = "feature1",
        newFeatureName = "feature2"
    )
*/
fun createNewModule(
    rootDir: File,
    folder: String,
    baseFeatureName: String,
    newFeatureName: String,
    exclude: (file: File, relativePath: String) -> Boolean = { file, relativePath ->
        relativePath.startsWith("build\\") ||
                relativePath == "build"
    }
) {

    // Input
    val baseFeature = "$folder/$baseFeatureName"
    val newFeature = "$folder/$newFeatureName"

    // create copy + replace
    val sourceDir = File(rootDir, baseFeature)
    val targetDir = File(rootDir, newFeature)

    if (!sourceDir.exists()) throw Exception("Source folder not found: ${sourceDir.path}")
    if (targetDir.exists()) throw Exception("Target folder exists already: ${targetDir.path}")
    sourceDir
        .walkTopDown()
        .filter {
            val relativePath = it.relativeTo(sourceDir).path
            !exclude(it, relativePath)
        }
        .forEach { file ->
            val relPath = file
                .relativeTo(sourceDir).path
                .replaceFolder(baseFeatureName, newFeatureName)

            val targetFile = File(targetDir, relPath)
            if (file.isDirectory) {
                targetFile.mkdirs()
            } else {
                var content = file.readText()
                content = content.replaceInContent(baseFeatureName, newFeatureName)
                targetFile.writeText(content)
            }
        }
    println("module copy created: $baseFeatureName => $newFeatureName")
}

private fun String.replaceFolder(
    folder: String,
    replacement: String
) : String {
    return this
        .replace("${File.separator}$folder${File.separator}", "${File.separator}$replacement${File.separator}")
}

private fun String.replaceInContent(
    value: String,
    replacement: String
): String {
    return this
        .replace(".${value}.", ".${replacement}.")
}
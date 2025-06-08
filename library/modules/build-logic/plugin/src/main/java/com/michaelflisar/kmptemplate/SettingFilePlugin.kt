package com.michaelflisar.kmptemplate

/*
class SettingFilePlugin : Plugin<Settings> {

    private lateinit var settings: Settings

    override fun apply(settings: Settings) {
        this.settings = settings
    }

    fun importAllModules(
        library: String =  with(settings) { rootDir.name.lowercase() },
        relativeLibraryFolderPath: String = "library"
    ) {
        with(settings) {
            println("Modules:")
            val folderLibrary = File(rootDir, relativeLibraryFolderPath)
            folderLibrary
                .walk()
                .maxDepth(10)
                .filter { it.isDirectory && File(it, "build.gradle.kts").exists() }
                .forEach { dir ->
                    val relativePath = dir.relativeTo(folderLibrary)
                    val relativeRoot = generateSequence(relativePath) { it.parentFile }.last()
                    val baseModulePath =
                        ":" + relativePath.invariantSeparatorsPath.replace('/', ':')
                    val modulePath: String
                    if (relativeRoot.name != "demo") {
                        modulePath = ":" + library.lowercase() + baseModulePath
                        include(modulePath)
                        project(modulePath).projectDir = dir
                    } else {
                        modulePath = ":" + relativePath.invariantSeparatorsPath.replace('/', ':')
                        include(modulePath)
                        project(modulePath).projectDir = dir
                    }
                    println("  - $modulePath")
                }
        }
    }
}*/
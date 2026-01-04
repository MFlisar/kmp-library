package com.michaelflisar.kmpdevtools.tooling

import com.michaelflisar.kmpdevtools.core.classes.RelativePath

class ToolingSetup(
    val root: RelativePath.Root,
    val gradlewFile: RelativePath = RelativePath("gradlew"),
    val scriptsFolder: RelativePath = RelativePath("tooling/scripts"),
    val xcodeProjectFolder: RelativePath? = RelativePath("demo/iosApp/iosApp.xcodeproj").takeIf {
        it.getLocalFile(root).exists()
    },
    val buildXCFrameworkFile: RelativePath = RelativePath("tooling/scripts/build_xcframework.sh"),
    val buildOnMacFile: RelativePath = RelativePath("tooling/scripts/build_on_mac.sh")
) {
    fun getExecutables(): List<RelativePath> {
        return listOf(
            gradlewFile,
            buildXCFrameworkFile,
            buildOnMacFile
        )
    }
}
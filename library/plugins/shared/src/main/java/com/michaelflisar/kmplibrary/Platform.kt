package com.michaelflisar.kmplibrary

enum class Platform(
    val targetName: String,
    val sourceSetName: String,
    val targets: List<String>,
    val color: String,
) {
    ANDROID("android", "android", listOf("android"), "3DDC84"),                           // Android Green
    IOS("ios", "ios", listOf("iosX64", "iosArm64", "iosSimulatorArm64"), "A2AAAD"),   // Apple Grey
    WINDOWS("windows","jvm",  listOf("jvm"), "5382A1"),                                       // JVM Blue (official Java blue)
    MACOS("macos", "macos",listOf("macosX64", "macosArm64"), "B0B0B0"),                // macOS Light Grey
    LINUX("linux", "linux", listOf("linuxX64", "linuxArm64"), "FF6600"),                // Linux Orange
    WASM("wasm", "wasmJs", listOf("wasmJs"), "624DE7"),                             // WebAssembly Purple
    JS("js", "js", listOf("js", "js(IR)"), "F7DF1E")                                 // JavaScript Yellow
    ;

    val nameMain = "${sourceSetName}Main"

    companion object {

        /**
         * parser function names from the Targets class
         */
        fun parseParameterName(name: String): Platform {
            return Platform.entries.find { it.targetName.equals(name, true) } ?:
                throw IllegalArgumentException("Unknown target name: $name")
        }

        /**
         * Targets: [WINDOWS], [MACOS], [LINUX]
         *
         * not supported: [ANDROID], [IOS], [WASM], [JS]
         */
        val LIST_COMPUTER = listOf(WINDOWS, MACOS, LINUX)

        /**
         * Targets: [IOS], [MACOS]
         *
         * not supported: [ANDROID], [WINDOWS], [LINUX], [WASM], [JS]
         */
        val LIST_APPLE = listOf(IOS, MACOS)

        /**
         * Targets: [ANDROID], [IOS]
         *
         * not supported: [WINDOWS], [MACOS], [LINUX], [WASM], [JS]
         */
        val LIST_MOBILE = listOf(ANDROID, IOS)

        /**
         * Targets: [ANDROID], [WINDOWS], [IOS], [MACOS], [LINUX]
         *
         * not supported: [WASM], [JS]
         */
        val LIST_FILE_SUPPORT = listOf(ANDROID, WINDOWS, IOS, MACOS, LINUX)
    }
}
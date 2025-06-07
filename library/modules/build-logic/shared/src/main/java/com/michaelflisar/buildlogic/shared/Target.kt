package com.michaelflisar.buildlogic.shared

enum class Target(
    val targetName: String,
    val targets: List<String>,
    val color: String,
) {
    ANDROID("android", listOf("android"), "3DDC84"),                           // Android Green
    IOS("ios", listOf("iosX64", "iosArm64", "iosSimulatorArm64"), "A2AAAD"),   // Apple Grey
    JVM("jvm", listOf("jvm"), "5382A1"),                                       // JVM Blue (official Java blue)
    MACOS("macos", listOf("macosX64", "macosArm64"), "B0B0B0"),                // macOS Light Grey
    LINUX("linux", listOf("linuxX64", "linuxArm64"), "FF6600"),                // Linux Orange
    WASM_JS("wasmJs", listOf("wasmJs"), "624DE7"),                             // WebAssembly Purple
    JS("js", listOf("js", "js(IR)"), "F7DF1E")                                 // JavaScript Yellow
    ;

    val nameMain = "${targetName}Main"

    companion object {

        /*
         * parser function names from the Targets class
         */
        fun parseParameterName(name: String): Target {
            return when (name.lowercase()) {
                "android" -> Target.ANDROID
                "ios" -> Target.IOS
                "windows" -> Target.JVM
                "macos" -> Target.MACOS
                "linux" -> Target.LINUX
                "wasm" -> Target.WASM_JS
                "js" -> Target.JS
                else -> throw RuntimeException("Unknown parameter name: $name")
            }
        }
    }
}
package com.michaelflisar.kmplibrary.utils

class Script(
    val name: String,
    val script: () -> Unit,
    val details: Map<String, String> = emptyMap()
) {
    internal var step: Int = 0

    fun runWithLogs() {
        runScriptWithLogs(this)
    }

    fun runStep(stepName: String, step: () -> Unit) {
        this.step++
        if (this.step > 1)
            println()
        println("---- Step $step: $stepName ----")
        step()
    }
}

private fun runScriptWithLogs(
    script: Script
) {
    println()
    printScriptRegionStart(script.name)
    try {

        // 1) details
        if (script.details.isNotEmpty()) {
            println()
            printScriptDetails(
                label = "Details",
                map = script.details
            )
        }
        // 2) script
        println()
        script.script()

        println()
        println("Script finished successfully.")
    } catch (e: Exception) {
        println()
        println("Script failed with exception: ${e.message}")
        e.printStackTrace()
    }
    println()
    printScriptRegionEnd(script.name)
}

fun runScriptStep(stepName: String, step: () -> Unit) {
    println(step)
}

private fun printScriptDetails(label: String, map: Map<String, String>) {
    println("$label:")
    val maxKeyLength = map.keys.maxOfOrNull { it.length } ?: 0
    map.forEach { (key, value) ->
        val padding = maxKeyLength - key.length
        val paddedKey = "$key = " + " ".repeat(if (padding > 0) padding else 0)
        println(" - $paddedKey $value")
    }
}

private fun printScriptRegionStart(name: String) {
    println("##################################################")
    println("#### Starting $name")
    println("##################################################")
}

private fun printScriptRegionEnd(name: String) {
    println("##################################################")
    println("#### End of $name")
    println("##################################################")
}
package com.michaelflisar.kmplibrary.utils

class ScriptScope internal constructor(
    val script: Script,
) {

    fun runStep(stepName: String, step: () -> Unit) {
        script.step++
        if (script.step > 1)
            println()
        println("---- Step $step: $stepName ----")
        step()
    }

}

class Script(
    val name: String,
    val details: Map<String, String> = emptyMap(),
) {
    internal var step: Int = 0

    fun runWithLogs(script: () -> Unit) {
        with(ScriptScope(this)) {
            println()
            printScriptRegionStart(name)
            try {

                // 1) details
                if (details.isNotEmpty()) {
                    println()
                    printScriptDetails(
                        label = "Details",
                        map = details
                    )
                }
                // 2) script
                println()
                script()

                println()
                println("Script finished successfully.")

            } catch (e: Exception) {

                println()
                println("Script failed with exception: ${e.message}")
                e.printStackTrace()

            }
            println()
            printScriptRegionEnd(name)
        }


    }
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
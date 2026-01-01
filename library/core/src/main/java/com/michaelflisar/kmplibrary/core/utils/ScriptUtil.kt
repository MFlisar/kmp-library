package com.michaelflisar.kmplibrary.core.utils

class ScriptStep(
    val name: String,
    val action: () -> Unit,
)

object ScriptUtil {

    /**
     * Runs a script with the given name and execute all steps. No user interaction.
     */
    fun runScript(
        name: String,
        steps: List<ScriptStep>,
        scriptInfos: (() -> Unit)? = null,
    ) {
        runScript(name, steps, scriptInfos, "a", false)
    }

    /**
     * Runs a script with the given name and steps. User will be prompted to select which steps to run.
     */
    fun runScriptSteps(
        name: String,
        steps: List<ScriptStep>,
        scriptInfos: (() -> Unit)? = null,
        defaultInput: String = "a"
    ) {
        runScript(name, steps, scriptInfos, defaultInput, true)
    }

    private fun runScript(
        name: String,
        steps: List<ScriptStep>,
        scriptInfos: (() -> Unit)? = null,
        defaultInput: String = "a",
        askForUserInput: Boolean
    ) {
        println()
        printScriptRegionStart(name)
        try {

            // 1) details
            if (scriptInfos != null) {
                println()
                scriptInfos()
            }
            println()

            // 2) print steps
            val input = if (askForUserInput) {

                println("------------------------")
                println("- Available steps")
                println("------------------------")
                steps.forEachIndexed { index, step ->
                    println("--- ${index + 1}: ${step.name}")
                }
                println("------------------------")
                println("- Select actions to run (input: 1, 2, 3 or 1-3, 4 etc. or a to run all)")
                println("------------------------")

                // 3) get user input
                println()
                val input = getUserInput("Steps to run? (default: $defaultInput)").ifEmpty { defaultInput }
                println()
                input

            } else {

                defaultInput
            }

            // 4) determine steps to run
            val stepsToRun = mutableListOf<Int>()
            if (input == "a") {
                // run all
                steps.indices.forEach { stepsToRun.add(it + 1) }
            } else {
                input.split(",").forEach { part ->
                    if (part.contains("-")) {
                        val rangeParts = part.split("-")
                        if (rangeParts.size == 2) {
                            val start = rangeParts[0].toIntOrNull()
                            val end = rangeParts[1].toIntOrNull()
                            if (start != null && end != null) {
                                stepsToRun.addAll((start..end).toList())
                            }
                        }
                    } else {
                        val stepNumber = part.toIntOrNull()
                        if (stepNumber != null) {
                            stepsToRun.add(stepNumber)
                        }
                    }
                }
            }

            // 5) run selected steps
            steps.forEachIndexed { index, step ->
                val number = index + 1
                println("[$number] ${step.name}")
                if (stepsToRun.contains(number)) {
                    step.action()
                } else {
                    println("SKIPPED")
                }
                println()
            }

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

    fun getUserInput(prompt: String): String {
        print("$prompt ")
        return readlnOrNull() ?: ""
    }

    fun printDetails(
        details: Map<String, String>,
        label: String = "Details"
    ) {
        printScriptDetails(
            label = label,
            map = details
        )
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
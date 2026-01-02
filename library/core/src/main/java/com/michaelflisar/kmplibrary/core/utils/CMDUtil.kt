package com.michaelflisar.kmplibrary.core.utils

import java.io.IOException

object CMDUtil {

    /**
     * Helper to run a command and throw if it fails, returns output
     *
     * @param cmd The command to run as list of strings
     * @param errorMsg The error message to include in the exception if the command fails
     * @param ignoreExitCode Optional lambda to ignore certain exit codes, receives exit code and output
     */
    fun runOrThrow(
        cmd: List<String>,
        errorMsg: String,
        ignoreExitCode: ((exitCode: Int, output: String) -> Boolean)? = null,
    ): String {
        val proc = ProcessBuilder(cmd)
            .redirectErrorStream(true)
            .start()
        val output = proc.inputStream.bufferedReader().readText()
        val exit = proc.waitFor()
        if (exit != 0 && (ignoreExitCode == null || !ignoreExitCode(exit, output))) {
            throw IOException("$errorMsg\nExit code: $exit\nOutput:\n$output")
        }
        return output
    }
}
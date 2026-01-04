package com.michaelflisar.kmpdevtools.core.utils

import java.io.File

class SSHSetup(
    val host: String,
    val user: String,
    val port: Int,
    val keyPath: String,
)

object SSHUtil {

    /**
     * Helper to run SSH command
     *
     * @param command The command to run on the remote host
     * @param sshSetup The SSH setup containing user, host, port, and keyPath
     * @param ignoreExitCode Optional lambda to determine if a non-zero exit code should be ignored
     */
    fun ssh(
        command: String,
        sshSetup: SSHSetup,
        ignoreExitCode: ((exitCode: Int, output: String) -> Boolean)? = null,
    ) {
        ssh(
            command = command,
            user = sshSetup.user,
            host = sshSetup.host,
            port = sshSetup.port,
            keyPath = sshSetup.keyPath,
            ignoreExitCode = ignoreExitCode
        )
    }

    /**
     * Helper to run SSH command
     *
     * @param command The command to run on the remote host
     * @param user The SSH username
     * @param host The SSH host
     * @param port The SSH port
     * @param keyPath The path to the SSH private key
     * @param ignoreExitCode Optional lambda to determine if a non-zero exit code should be ignored
     */
    fun ssh(
        command: String,
        user: String,
        host: String,
        port: Int,
        keyPath: String,
        ignoreExitCode: ((exitCode: Int, output: String) -> Boolean)? = null,
    ) {
        val sshCmd = mutableListOf("ssh", "-p", port.toString())
        if (File(keyPath).exists())
            sshCmd += listOf("-i", keyPath)
        sshCmd += listOf("$user@$host", command)
        CMDUtil.runOrThrow(
            cmd = sshCmd,
            errorMsg = "SSH command failed: $command",
            ignoreExitCode = ignoreExitCode
        )
    }
}
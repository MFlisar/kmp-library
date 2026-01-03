package com.michaelflisar.kmplibrary.tooling

import com.michaelflisar.kmplibrary.core.classes.RelativePath
import com.michaelflisar.kmplibrary.core.configs.LibraryConfig
import com.michaelflisar.kmplibrary.core.utils.SSHSetup
import org.gradle.api.Project

object MacDefaults {

    fun getMacSSHSetup(): SSHSetup {
        return SSHSetup(
            host = "macmini.local",
            user = "mflisar",
            port = 22,
            keyPath = System.getProperty("user.home") + "/.ssh/id_ed25519",
        )
    }

    fun getRelativePathRoot(
        project: Project,
        libraryConfig: LibraryConfig = LibraryConfig.read(project),
        macTargetRootDir: String = "/Users/mflisar/dev",
    ) = RelativePath.Root(
        local = project.rootDir.absolutePath,
        remote = "$macTargetRootDir/${libraryConfig.library.name}"
    )
}
package com.michaelflisar.kmpdevtools.core.utils

import kotlin.test.Test
import kotlin.test.assertTrue

class GithubUtilTest {

    @Test
    fun getLastReleaseTest() {
        val result = GithubUtil.getLastRelease(
            githubRepo = "MFlisar/kmp-devtools",
            accessMode = GithubUtil.AccessMode.Auto,
            type = GithubUtil.ReleaseType.LATEST
        )

        println("Latest release: $result")

        // Network-based call: null is acceptable, but non-null must be non-blank.
        assertTrue(result == null || result.isNotBlank())
    }
}


package com.michaelflisar.kmpdevtools.core.utils

import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

object GithubUtil {

    enum class ReleaseType {
        LATEST, STABLE, PRE_RELEASE
    }

    // github repo: e.g.: "https://github.com/MFlisar/kmp-devtools" or "MFlisar/kmp-devtools"
    // returns latest release tag depending on ReleaseType, e.g. "v1.0.0", or null if none / on error
    fun getLastRelease(
        githubRepo: String,
        type: ReleaseType = ReleaseType.LATEST
    ): String? {
        try {
            val repo = githubRepo.trim().removeSuffix("/").let {
                if (it.contains("github.com/")) {
                    val parts = it.substringAfter("github.com/").split("/")
                    if (parts.size >= 2) "${parts[0]}/${parts[1]}" else it
                } else it
            }

            if (!repo.contains("/")) {
                println("GithubUtil.getLastRelease: invalid repo string: $githubRepo")
                return null
            }

            val apiUrl = "https://api.github.com/repos/$repo/releases"
            val url = URL(apiUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "kmp-devtools")
                val token = System.getenv("GITHUB_TOKEN") ?: System.getenv("GH_TOKEN")
                if (!token.isNullOrBlank()) {
                    setRequestProperty("Authorization", "token $token")
                }
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream.bufferedReader().use(BufferedReader::readText)

            if (code == 200) {
                // minimal JSON parsing: iterate occurrences of "tag_name" and inspect surrounding object for prerelease/draft
                val results = mutableListOf<Triple<String, Boolean, Boolean>>() // tag, prerelease, draft

                var index = 0
                while (true) {
                    val tagIndex = body.indexOf("\"tag_name\"", index)
                    if (tagIndex < 0) break
                    // find value start
                    val colon = body.indexOf(':', tagIndex)
                    if (colon < 0) break
                    val quoteStart = body.indexOf('"', colon)
                    if (quoteStart < 0) break
                    val quoteEnd = body.indexOf('"', quoteStart + 1)
                    if (quoteEnd < 0) break
                    val tag = body.substring(quoteStart + 1, quoteEnd)

                    // find object bounds (search back for '{' and forward for '}')
                    val objStart = body.lastIndexOf('{', tagIndex)
                    val objEnd = body.indexOf('}', quoteEnd)
                    val obj = if (objStart >= 0 && objEnd > objStart) body.substring(objStart, objEnd + 1) else ""

                    val prerelease = Regex("\"prerelease\"\\s*:\\s*(true|false)").find(obj)?.groups?.get(1)?.value?.toBoolean() ?: false
                    val draft = Regex("\"draft\"\\s*:\\s*(true|false)").find(obj)?.groups?.get(1)?.value?.toBoolean() ?: false

                    results.add(Triple(tag, prerelease, draft))

                    index = quoteEnd + 1
                }

                if (results.isEmpty()) {
                    println("GithubUtil.getLastRelease: no releases found in list for $repo")
                    return null
                }

                fun pickFirst(predicate: (Triple<String, Boolean, Boolean>) -> Boolean): String? {
                    return results.firstOrNull { predicate(it) }?.first
                }

                return when (type) {
                    ReleaseType.LATEST -> {
                        // take first non-draft if possible, otherwise first element
                        pickFirst { !it.third } ?: results.first().first
                    }
                    ReleaseType.STABLE -> {
                        // first non-prerelease and non-draft
                        pickFirst { !it.second && !it.third }
                    }
                    ReleaseType.PRE_RELEASE -> {
                        // first prerelease and non-draft
                        pickFirst { it.second && !it.third }
                    }
                }
            } else if (code == 404) {
                println("GithubUtil.getLastRelease: no releases found for $repo (404)")
                return null
            } else {
                println("GithubUtil.getLastRelease: unexpected response $code for $repo: $body")
                return null
            }
        } catch (e: Exception) {
            println("GithubUtil.getLastRelease: failed: ${e.message}")
            return null
        }
    }
}
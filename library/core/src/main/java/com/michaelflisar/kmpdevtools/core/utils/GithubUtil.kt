package com.michaelflisar.kmpdevtools.core.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

object GithubUtil {

    enum class ReleaseType {
        LATEST,
        STABLE,
        PRE_RELEASE
    }

    enum class AccessMode {
        RestAPI,
        Auto
    }

    private const val USER_AGENT = "kmp-devtools/1.0 (+https://github.com/MFlisar/kmp-devtools)"

    private val json = Json { ignoreUnknownKeys = true }

    // github repo: e.g.: "https://github.com/MFlisar/kmp-devtools" or "MFlisar/kmp-devtools"
    // returns latest release tag depending on ReleaseType, e.g. "v1.0.0", or null if none / on error
    fun getLastRelease(
        githubRepo: String,
        accessMode: AccessMode,
        type: ReleaseType = ReleaseType.LATEST,
    ): String? {

        // 1) get repo in form "owner/repo" from input string
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

        // 2) check if we can use auto mode (only for latest release, because we need to parse html for stable / pre-release)
        if (accessMode == AccessMode.Auto) {
            if (type == ReleaseType.LATEST) {
                // we use non api method for latest release:
                // resolve url: https://github.com/MFlisar/KMPPlatformContext/releases/latest
                // result: https://github.com/MFlisar/KMPPlatformContext/releases/tag/2.0.2
                // => damit kann man version aus link auslesen
                val finalLink = resolveUrl("https://github.com/$repo/releases/latest")
                val version = finalLink
                    ?.substringAfter("/releases/tag/", missingDelimiterValue = "")
                    ?.substringBefore("?")
                    ?.substringBefore("#")
                    ?.takeIf { it.isNotBlank() }

                return version
            }
        }

        // 3) load data from github via rest api and parse result
        val apiUrl = "https://api.github.com/repos/$repo/releases"
        val body = loadUrl(apiUrl)
        if (body == null) {
            println("GithubUtil.getLastRelease: failed to load releases for $repo")
            return null
        }

        val releases: JsonArray = json.parseToJsonElement(body).jsonArray

        // tag, prerelease, draft
        val results = releases.mapNotNull { element ->
            val obj: JsonObject = element.jsonObject
            val tag = obj["tag_name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val prerelease = obj["prerelease"]?.jsonPrimitive?.booleanOrNull ?: false
            val draft = obj["draft"]?.jsonPrimitive?.booleanOrNull ?: false
            Triple(tag, prerelease, draft)
        }

        if (results.isEmpty()) {
            println("GithubUtil.getLastRelease: no releases found in list for $repo")
            return null
        }

        return when (type) {
            ReleaseType.LATEST -> results.firstOrNull { !it.third }?.first ?: results.first().first
            ReleaseType.STABLE -> results.firstOrNull { !it.second && !it.third }?.first
            ReleaseType.PRE_RELEASE -> results.firstOrNull { it.second && !it.third }?.first
        }
    }

    private fun loadUrl(apiUrl: String): String? {
        return try {
            val url = URL(apiUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", USER_AGENT)
                val token = System.getenv("GITHUB_TOKEN") ?: System.getenv("GH_TOKEN")
                if (!token.isNullOrBlank()) {
                    setRequestProperty("Authorization", "token $token")
                }
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream.bufferedReader().use(BufferedReader::readText)

            if (code == 200) {
                body
            } else {
                println("GithubUtil.loadUrl: unexpected response $code for $apiUrl: $body")
                null
            }
        } catch (e: Exception) {
            println("GithubUtil.loadUrl: failed: ${e.message}")
            null
        }
    }

    private fun resolveUrl(url: String, maxRedirects: Int = 10): String? {
        return try {
            var current = URL(url)
            val visited = mutableSetOf<String>()

            repeat(maxRedirects + 1) {
                val currentStr = current.toString()
                if (!visited.add(currentStr)) {
                    println("GithubUtil.resolveUrl: redirect loop detected at $currentStr")
                    return null
                }

                val conn = (current.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty("User-Agent", USER_AGENT)
                }

                val code = conn.responseCode
                if (code in 300..399) {
                    val location = conn.getHeaderField("Location")
                    if (location.isNullOrBlank()) {
                        println("GithubUtil.resolveUrl: missing Location header for redirect from $currentStr")
                        return null
                    }
                    current = URL(current, location) // supports relative redirects
                } else {
                    return current.toString()
                }
            }

            println("GithubUtil.resolveUrl: too many redirects (>$maxRedirects) for $url")
            null
        } catch (e: Exception) {
            println("GithubUtil.getLastRelease: auto latest resolve failed: ${e.message}")
            null
        }
    }
}
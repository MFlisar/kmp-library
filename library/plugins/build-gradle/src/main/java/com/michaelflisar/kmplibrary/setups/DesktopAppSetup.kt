package com.michaelflisar.kmplibrary.setups

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DesktopAppSetup(
    val appName: String,
    val appVersionName: String,
    val mainClass: String,
    val author: String,
    val ico: String
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    val packageName = appName // entspricht dem exe Name
    val packageVersion = appVersionName
    fun description(date: LocalDateTime) = "$appName - Build at ${date.format(formatter)}"
    fun copyright(date: LocalDateTime) = "©${date.year} $author. All rights reserved."
    val vendor = author
}
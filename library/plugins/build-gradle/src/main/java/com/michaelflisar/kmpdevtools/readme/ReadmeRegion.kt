package com.michaelflisar.kmpdevtools.readme

class ReadmeRegion(
    val image: String,
    val text: String
) {
    fun markdownHeader() = "# :$image: $text"
    fun markdownLink() = ":$image: $text"
        .lowercase()
        .replace(":", "")
        .replace(" ", "-")
        .let { "[${text}](#${it})" }
}
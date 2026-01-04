package com.michaelflisar.kmpdevtools.core.classes

import java.io.File

class RelativePath(
    val path: String,
) {

    fun getName() = path.substringAfterLast("/")

    fun getRemotePath(root: Root) = "${root.remote}/$path"
    fun getRemotePathParent(root: Root) = getRemotePath(root).substringBeforeLast("/")

    fun getLocalPath(root: Root) = "${root.local}/$path"
    fun getLocalFile(root: Root) = File(getLocalPath(root))

    class Root(
        val local: String,
        val remote: String,
    )
}
package net.bullmc.client.core.util

import java.io.File

/** Resolve an untrusted archive or manifest path without escaping the destination. */
internal fun safeDestination(root: File, relativePath: String): File {
    val portablePath = relativePath.replace('\\', '/')
    require(portablePath.isNotBlank() && !portablePath.startsWith('/') &&
        !Regex("^[A-Za-z]:").containsMatchIn(portablePath)) { "Invalid relative path: $relativePath" }
    val base = root.canonicalFile.toPath()
    val destination = File(root, portablePath).canonicalFile
    require(destination.toPath().startsWith(base) && destination.toPath() != base) {
        "Path escapes destination: $relativePath"
    }
    return destination
}

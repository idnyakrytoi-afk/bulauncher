package net.bullmc.client.core.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class VersionState {
    var selectedVersion by mutableStateOf("1.20.4")
        private set

    var availableVersions by mutableStateOf<List<String>>(emptyList())
        private set

    fun selectVersion(version: String) {
        if (version in availableVersions) {
            selectedVersion = version
        }
    }

    fun updateVersions(versions: List<String>) {
        availableVersions = versions
        if (selectedVersion !in versions && versions.isNotEmpty()) {
            selectedVersion = versions.first()
        }
    }
}

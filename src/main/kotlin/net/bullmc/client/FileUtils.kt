package net.bullmc.client

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

object FileUtils {
    fun chooseJavaExecutable(): String? {
        val chooser = JFileChooser()
        chooser.fileFilter = FileNameExtensionFilter(
            "Java Executable (javaw.exe, java.exe, java)",
            "exe"
        )
        chooser.currentDirectory = File(System.getenv("JAVA_HOME") ?: System.getProperty("user.home"))
        
        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile.absolutePath
        } else {
            null
        }
    }
}

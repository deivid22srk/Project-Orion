package com.winlator.cmod.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
data class Shortcut(
    val container: Container,
    val name: String,
    val path: String,
    val file: File,
    val iconFile: File? = null,
    val wmClass: String = "",
    val customCoverArtPath: String? = null
) : Parcelable {
    
    @IgnoredOnParcel
    var icon: Bitmap? = null
        private set
    
    @IgnoredOnParcel
    var coverArt: Bitmap? = null
        private set
    
    init {
        loadIcon()
        loadCoverArt()
    }
    
    private fun loadIcon() {
        iconFile?.let { file ->
            if (file.exists()) {
                icon = BitmapFactory.decodeFile(file.absolutePath)
            }
        }
    }
    
    private fun loadCoverArt() {
        customCoverArtPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                coverArt = BitmapFactory.decodeFile(file.absolutePath)
            }
        }
    }
    
    fun getExecutable(): String {
        return path.substringAfterLast("\\").trim()
    }
    
    companion object {
        fun fromFile(container: Container, file: File): Shortcut? {
            if (!file.exists() || !file.name.endsWith(".desktop")) return null
            
            var execArgs = ""
            var iconFile: File? = null
            var wmClass = ""
            
            file.readLines().forEach { line ->
                when {
                    line.startsWith("Exec=") -> execArgs = line.substringAfter("Exec=")
                    line.startsWith("Icon=") -> {
                        val iconName = line.substringAfter("Icon=")
                        iconFile = findIconFile(container, iconName)
                    }
                    line.startsWith("StartupWMClass=") -> wmClass = line.substringAfter("StartupWMClass=")
                }
            }
            
            val name = file.nameWithoutExtension
            val path = if (execArgs.contains("wine ")) {
                execArgs.substringAfter("wine ").trim()
            } else {
                execArgs.trim()
            }
            
            return Shortcut(
                container = container,
                name = name,
                path = path,
                file = file,
                iconFile = iconFile,
                wmClass = wmClass
            )
        }
        
        private fun findIconFile(container: Container, iconName: String): File? {
            val iconSizes = listOf(64, 48, 32, 16)
            for (size in iconSizes) {
                container.rootDir?.let { rootDir ->
                    val iconDir = File(rootDir, ".local/share/icons/hicolor/${size}x${size}/apps/")
                    listOf("$iconName.png", "$iconName.ico").forEach { fileName ->
                        val iconFile = File(iconDir, fileName)
                        if (iconFile.exists()) return iconFile
                    }
                }
            }
            return null
        }
    }
}

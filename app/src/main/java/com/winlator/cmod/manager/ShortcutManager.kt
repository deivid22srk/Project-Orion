package com.winlator.cmod.manager

import android.content.Context
import android.graphics.Bitmap
import com.winlator.cmod.core.FileUtils
import com.winlator.cmod.data.Container
import com.winlator.cmod.data.Shortcut
import com.winlator.cmod.xenvironment.ImageFs
import java.io.File

class ShortcutManager(private val context: Context) {
    
    fun createShortcut(
        container: Container,
        name: String,
        executablePath: String,
        execArgs: String = "",
        icon: Bitmap? = null
    ): Shortcut? {
        val desktopDir = container.getDesktopDir() ?: return null
        
        if (!desktopDir.exists()) {
            desktopDir.mkdirs()
        }
        
        val desktopFile = File(desktopDir, "$name.desktop")
        
        var iconName = ""
        if (icon != null) {
            iconName = saveIcon(container, name, icon)
        }
        
        val content = buildString {
            appendLine("[Desktop Entry]")
            appendLine("Name=$name")
            appendLine("Exec=wine $executablePath $execArgs")
            appendLine("Type=Application")
            appendLine("StartupNotify=true")
            if (iconName.isNotEmpty()) {
                appendLine("Icon=$iconName")
            }
            appendLine()
            appendLine("[Extra Data]")
            appendLine("execArgs=$execArgs")
        }
        
        if (!FileUtils.writeString(desktopFile, content)) {
            return null
        }
        
        return Shortcut.fromFile(container, desktopFile)
    }
    
    private fun saveIcon(container: Container, name: String, icon: Bitmap): String {
        try {
            container.rootDir?.let { rootDir ->
                val iconDir = File(rootDir, ".local/share/icons/hicolor/64x64/apps/")
                iconDir.mkdirs()
                
                val iconFile = File(iconDir, "$name.png")
                if (FileUtils.saveBitmapToFile(icon, iconFile)) {
                    return name
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }
    
    fun deleteShortcut(shortcut: Shortcut): Boolean {
        val deleted = shortcut.file.delete()
        
        val lnkPath = shortcut.file.path.substringBeforeLast(".") + ".lnk"
        val lnkFile = File(lnkPath)
        if (lnkFile.exists()) {
            lnkFile.delete()
        }
        
        return deleted
    }
    
    fun renameShortcut(shortcut: Shortcut, newName: String): Boolean {
        val parent = shortcut.file.parentFile ?: return false
        val newFile = File(parent, "$newName.desktop")
        
        if (newFile.exists() && newFile != shortcut.file) {
            return false
        }
        
        val content = buildString {
            shortcut.file.readLines().forEach { line ->
                when {
                    line.startsWith("Name=") -> appendLine("Name=$newName")
                    else -> appendLine(line)
                }
            }
        }
        
        FileUtils.writeString(newFile, content)
        
        if (newFile != shortcut.file) {
            shortcut.file.delete()
        }
        
        return true
    }
    
    fun updateShortcutExtra(shortcut: Shortcut, key: String, value: String?) {
        val lines = shortcut.file.readLines().toMutableList()
        var inExtraData = false
        var keyFound = false
        val newLines = mutableListOf<String>()
        
        for (line in lines) {
            when {
                line == "[Extra Data]" -> {
                    inExtraData = true
                    newLines.add(line)
                }
                inExtraData && line.startsWith("$key=") -> {
                    if (value != null) {
                        newLines.add("$key=$value")
                    }
                    keyFound = true
                }
                else -> newLines.add(line)
            }
        }
        
        if (!inExtraData) {
            newLines.add("")
            newLines.add("[Extra Data]")
        }
        
        if (!keyFound && value != null) {
            newLines.add("$key=$value")
        }
        
        FileUtils.writeString(shortcut.file, newLines.joinToString("\n"))
    }
}

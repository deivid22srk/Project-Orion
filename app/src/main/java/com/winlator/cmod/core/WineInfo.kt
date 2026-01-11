package com.winlator.cmod.core

import android.content.Context
import com.winlator.cmod.contents.ContentsManager
import com.winlator.cmod.xenvironment.ImageFs
import java.io.File

data class WineInfo(
    val type: String,
    val version: String,
    val subversion: String? = null,
    val arch: String,
    val path: String? = null
) {
    companion object {
        val MAIN_WINE_VERSION = WineInfo("proton", "9.0", null, "x86_64", null)
        
        fun isMainWineVersion(wineVersion: String): Boolean {
            return wineVersion == MAIN_WINE_VERSION.identifier()
        }
        
        fun fromIdentifier(context: Context, contentsManager: ContentsManager?, identifier: String): WineInfo {
            val imageFs = ImageFs.find(context)
            val parts = identifier.split("-")
            
            if (parts.size >= 3) {
                val type = parts[0]
                val version = parts[1]
                val arch = parts.last()
                val path = File(imageFs.getRootDir(), "/opt/$identifier").absolutePath
                
                return WineInfo(type, version, null, arch, path)
            }
            
            return MAIN_WINE_VERSION.copy(path = File(imageFs.getRootDir(), "/opt/${MAIN_WINE_VERSION.identifier()}").absolutePath)
        }
    }
    
    fun isWin64(): Boolean {
        return arch == "x86_64" || arch == "arm64ec"
    }
    
    fun isArm64EC(): Boolean {
        return arch == "arm64ec"
    }
    
    fun identifier(): String {
        val fullVer = version + (subversion?.let { "-$it" } ?: "")
        return if (type == "proton") {
            "proton-$fullVer-$arch"
        } else {
            "wine-$fullVer-$arch"
        }
    }
    
    fun fullVersion(): String {
        return version + (subversion?.let { "-$it" } ?: "")
    }
    
    override fun toString(): String {
        val label = if (type == "proton") "Proton" else "Wine"
        val mainSuffix = if (this == MAIN_WINE_VERSION) " (Custom)" else ""
        return "$label ${fullVersion()}$mainSuffix"
    }
}

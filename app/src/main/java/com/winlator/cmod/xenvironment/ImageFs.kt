package com.winlator.cmod.xenvironment

import android.content.Context
import com.winlator.cmod.core.FileUtils
import com.winlator.cmod.core.WineInfo
import java.io.File

class ImageFs private constructor(private val rootDir: File) {
    companion object {
        const val USER = "xuser"
        const val HOME_PATH = "/home/$USER"
        const val CACHE_PATH = "$HOME_PATH/.cache"
        const val CONFIG_PATH = "$HOME_PATH/.config"
        const val WINEPREFIX = "$HOME_PATH/.wine"
        
        fun find(context: Context): ImageFs {
            return ImageFs(File(context.filesDir, "imagefs"))
        }
        
        fun find(rootDir: File): ImageFs {
            return ImageFs(rootDir)
        }
    }
    
    var winePath: String = "$rootDir/opt/${WineInfo.MAIN_WINE_VERSION.identifier()}"
    var homePath: String = "$rootDir$HOME_PATH"
    var cachePath: String = "$rootDir$CACHE_PATH"
    var configPath: String = "$rootDir$CONFIG_PATH"
    var wineprefix: String = "$rootDir$WINEPREFIX"
    
    fun getRootDir(): File = rootDir
    
    fun isValid(): Boolean {
        return rootDir.isDirectory && getImgVersionFile().exists()
    }
    
    fun getVersion(): Int {
        val imgVersionFile = getImgVersionFile()
        return if (imgVersionFile.exists()) {
            FileUtils.readLines(imgVersionFile).firstOrNull()?.toIntOrNull() ?: 0
        } else {
            0
        }
    }
    
    fun getFormattedVersion(): String {
        return String.format("%.1f", getVersion().toFloat())
    }
    
    fun createImgVersionFile(version: Int) {
        getConfigDir().mkdirs()
        val file = getImgVersionFile()
        try {
            file.createNewFile()
            FileUtils.writeString(file, version.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun getWinePath(): String = winePath
    
    fun setWinePath(path: String) {
        winePath = path
    }
    
    fun getConfigDir(): File = File(rootDir, ".winlator")
    
    fun getImgVersionFile(): File = File(getConfigDir(), ".img_version")
    
    fun getInstalledWineDir(): File = File(rootDir, "/opt/installed-wine")
    
    fun getTmpDir(): File = File(rootDir, "/usr/tmp")
    
    fun getLibDir(): File = File(rootDir, "/usr/lib")
    
    fun getBinDir(): File = File(rootDir, "/usr/bin")
    
    fun getShareDir(): File = File(rootDir, "/usr/share")
    
    fun getEtcDir(): File = File(rootDir, "/usr/etc")
    
    override fun toString(): String = rootDir.path
}

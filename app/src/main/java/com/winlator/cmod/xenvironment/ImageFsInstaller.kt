package com.winlator.cmod.xenvironment

import android.content.Context
import com.winlator.cmod.core.FileUtils
import com.winlator.cmod.core.TarCompressorUtils
import com.winlator.cmod.core.WineInfo
import com.winlator.cmod.core.DefaultVersion
import com.winlator.cmod.manager.ContainerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicLong

object ImageFsInstaller {
    const val LATEST_VERSION: Byte = 21
    private const val TAG = "ImageFsInstaller"
    
    private fun resetContainerImgVersions(context: Context) {
        val manager = ContainerManager(context)
        for (container in manager.getContainers()) {
            val imgVersion = container.getExtra("imgVersion")
            val wineVersion = container.wineVersion
            
            if (imgVersion.isNotEmpty() && WineInfo.isMainWineVersion(wineVersion)) {
                val version = imgVersion.toShortOrNull() ?: 0
                if (version <= 5) {
                    container.putExtra("wineprefixNeedsUpdate", "t")
                }
            }
            
            container.putExtra("imgVersion", null)
            container.saveData()
        }
    }
    
    private fun installWineFromAssets(context: Context) {
        val imageFs = ImageFs.find(context)
        val rootDir = imageFs.getRootDir()
        
        val versions = arrayOf(WineInfo.MAIN_WINE_VERSION.identifier())
        
        android.util.Log.d(TAG, "Installing Wine versions: ${versions.joinToString()}")
        
        for (version in versions) {
            val outFile = File(rootDir, "/opt/$version")
            outFile.mkdirs()
            
            android.util.Log.d(TAG, "Extracting Wine version: $version")
            TarCompressorUtils.extract(
                TarCompressorUtils.Type.XZ,
                context,
                "$version.txz",
                outFile
            )
        }
    }
    
    private fun installDriversFromAssets(context: Context) {
        val imageFs = ImageFs.find(context)
        val rootDir = imageFs.getRootDir()
        
        android.util.Log.d(TAG, "Installing graphics drivers and runtime components")
        
        // Graphics wrapper (Zink/Vulkan)
        android.util.Log.d(TAG, "Extracting graphics wrapper")
        TarCompressorUtils.extract(
            TarCompressorUtils.Type.ZSTD,
            context,
            "graphics_driver/wrapper.tzst",
            rootDir
        )
        
        // Extra libraries (Mesa, Vulkan layers, etc)
        android.util.Log.d(TAG, "Extracting extra libraries")
        TarCompressorUtils.extract(
            TarCompressorUtils.Type.ZSTD,
            context,
            "graphics_driver/extra_libs.tzst",
            rootDir
        )
        
        // Vulkan layers
        android.util.Log.d(TAG, "Extracting Vulkan layers")
        TarCompressorUtils.extract(
            TarCompressorUtils.Type.ZSTD,
            context,
            "layers.tzst",
            rootDir
        )
        
        // PulseAudio
        android.util.Log.d(TAG, "Extracting PulseAudio")
        TarCompressorUtils.extract(
            TarCompressorUtils.Type.ZSTD,
            context,
            "pulseaudio.tzst",
            rootDir
        )
        
        // Box64
        android.util.Log.d(TAG, "Extracting Box64 version ${DefaultVersion.BOX64}")
        TarCompressorUtils.extract(
            TarCompressorUtils.Type.ZSTD,
            context,
            "box64/box64-${DefaultVersion.BOX64}.tzst",
            rootDir
        )
        
        // FEXCore
        android.util.Log.d(TAG, "Extracting FEXCore version ${DefaultVersion.FEXCORE}")
        TarCompressorUtils.extract(
            TarCompressorUtils.Type.ZSTD,
            context,
            "fexcore/fexcore-${DefaultVersion.FEXCORE}.tzst",
            rootDir
        )
        
        // Adrenotools drivers
        val adrenotoolsDir = File(context.filesDir, "contents/adrenotools")
        adrenotoolsDir.mkdirs()
        
        val adrenotoolsDrivers = arrayOf(DefaultVersion.WRAPPER_ADRENO, "v819")
        for (driver in adrenotoolsDrivers) {
            android.util.Log.d(TAG, "Extracting Adrenotools driver: $driver")
            try {
                TarCompressorUtils.extract(
                    TarCompressorUtils.Type.ZSTD,
                    context,
                    "graphics_driver/adrenotools-$driver.tzst",
                    File(adrenotoolsDir, driver)
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to extract driver $driver: ${e.message}")
            }
        }
        
        android.util.Log.d(TAG, "Graphics driver and runtime installation completed")
    }
    
    private fun clearRootDir(rootDir: File) {
        if (rootDir.isDirectory) {
            rootDir.listFiles()?.forEach { file ->
                if (file.isDirectory && file.name == "home") {
                    return@forEach
                }
                FileUtils.delete(file)
            }
        } else {
            rootDir.mkdirs()
        }
    }
    
    suspend fun installFromAssets(context: Context, onProgress: (Int) -> Unit): Boolean = withContext(Dispatchers.IO) {
        val imageFs = ImageFs.find(context)
        val rootDir = imageFs.getRootDir()
        
        android.util.Log.d(TAG, "Starting ImageFS installation")
        
        clearRootDir(rootDir)
        
        val compressionRatio: Byte = 22
        val contentLength = (FileUtils.getSize(context, "imagefs.txz") * (100.0f / compressionRatio)).toLong()
        val totalSizeRef = AtomicLong()
        
        val success = TarCompressorUtils.extract(
            TarCompressorUtils.Type.XZ,
            context,
            "imagefs.txz",
            rootDir,
            object : TarCompressorUtils.OnExtractFileListener {
                override fun onExtractFile(file: File, size: Long): File? {
                    if (size > 0) {
                        val totalSize = totalSizeRef.addAndGet(size)
                        val progress = ((totalSize.toFloat() / contentLength) * 100).toInt()
                        onProgress(progress)
                    }
                    return file
                }
            }
        )
        
        if (success) {
            android.util.Log.d(TAG, "ImageFS base extracted successfully")
            installWineFromAssets(context)
            installDriversFromAssets(context)
            imageFs.createImgVersionFile(LATEST_VERSION.toInt())
            resetContainerImgVersions(context)
            android.util.Log.d(TAG, "Installation completed successfully")
        } else {
            android.util.Log.e(TAG, "Failed to extract imagefs.txz")
        }
        
        success
    }
    
    fun installIfNeeded(
        context: Context,
        onInstallStart: () -> Unit,
        onProgress: (Int) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        val imageFs = ImageFs.find(context)
        
        android.util.Log.d(TAG, "Checking if installation needed - Valid: ${imageFs.isValid()}, Version: ${imageFs.getVersion()}/$LATEST_VERSION")
        
        if (!imageFs.isValid() || imageFs.getVersion() < LATEST_VERSION) {
            android.util.Log.d(TAG, "Installation required")
            onInstallStart()
            
            CoroutineScope(Dispatchers.IO).launch {
                val success = installFromAssets(context, onProgress)
                withContext(Dispatchers.Main) {
                    onComplete(success)
                }
            }
        } else {
            android.util.Log.d(TAG, "Installation not needed")
            onComplete(true)
        }
    }
}

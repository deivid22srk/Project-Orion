package com.winlator.cmod.core

import android.content.Context
import android.util.Log
import com.winlator.cmod.data.Container
import com.winlator.cmod.xenvironment.ImageFs
import org.json.JSONObject
import java.io.File

/**
 * Helper para extrair componentes Wine (direct3d, directsound, etc)
 * Baseado no XServerDisplayActivity.extractWinComponentFiles()
 */
object WineComponentsHelper {
    private const val TAG = "WineComponentsHelper"
    
    fun extractWinComponentsIfNeeded(context: Context, container: Container) {
        val imageFs = ImageFs.find(context)
        val windowsDir = File(imageFs.getRootDir(), "${ImageFs.WINEPREFIX}/drive_c/windows")
        
        if (!windowsDir.exists()) {
            Log.e(TAG, "Windows directory not found: ${windowsDir.absolutePath}")
            return
        }
        
        val wincomponents = container.wincomponents
        val savedComponents = container.getExtra("wincomponents")
        
        if (wincomponents == savedComponents && savedComponents.isNotEmpty()) {
            Log.d(TAG, "Wine components unchanged, skipping extraction")
            return
        }
        
        Log.d(TAG, "Extracting Wine components: $wincomponents")
        
        try {
            val jsonString = FileUtils.readString(context, "wincomponents/wincomponents.json")
            val wincomponentsJSON = JSONObject(jsonString)
            
            wincomponents.split(",").forEach { component ->
                val parts = component.split("=")
                if (parts.size == 2) {
                    val identifier = parts[0].trim()
                    val useNative = parts[1].trim() == "1"
                    
                    if (useNative) {
                        Log.d(TAG, "Extracting native $identifier")
                        TarCompressorUtils.extract(
                            TarCompressorUtils.Type.ZSTD,
                            context,
                            "wincomponents/$identifier.tzst",
                            windowsDir
                        )
                    } else {
                        Log.d(TAG, "Skipping native $identifier (using Wine builtin)")
                    }
                }
            }
            
            container.putExtra("wincomponents", wincomponents)
            container.saveData()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting wine components: ${e.message}", e)
        }
    }
    
    fun extractDXWrapperIfNeeded(context: Context, container: Container) {
        val imageFs = ImageFs.find(context)
        val windowsDir = File(imageFs.getRootDir(), "${ImageFs.WINEPREFIX}/drive_c/windows")
        
        if (!windowsDir.exists()) {
            Log.e(TAG, "Windows directory not found for DXWrapper extraction")
            return
        }
        
        val dxwrapper = container.dxwrapper
        val savedDxWrapper = container.getExtra("dxwrapper")
        
        if (dxwrapper == savedDxWrapper && savedDxWrapper.isNotEmpty()) {
            Log.d(TAG, "DX wrapper unchanged, skipping extraction")
            return
        }
        
        Log.d(TAG, "Extracting DX wrapper: $dxwrapper")
        
        val parts = dxwrapper.split(";")
        val dxvkWrapper = parts.getOrNull(0) ?: return
        val vkd3dWrapper = parts.getOrNull(1) ?: "None"
        val ddrawWrapper = parts.getOrNull(2) ?: "None"
        
        if (dxvkWrapper.contains("dxvk")) {
            Log.d(TAG, "Extracting DXVK: $dxvkWrapper")
            TarCompressorUtils.extract(
                TarCompressorUtils.Type.ZSTD,
                context,
                "dxwrapper/$dxvkWrapper.tzst",
                windowsDir
            )
            
            if (!dxvkWrapper.contains("2.4") && !dxvkWrapper.contains("2.5")) {
                Log.d(TAG, "Extracting D8VK for compatibility")
                TarCompressorUtils.extract(
                    TarCompressorUtils.Type.ZSTD,
                    context,
                    "dxwrapper/d8vk-${DefaultVersion.D8VK}.tzst",
                    windowsDir
                )
            }
        }
        
        if (!vkd3dWrapper.equals("None", ignoreCase = true)) {
            Log.d(TAG, "Extracting VKD3D: $vkd3dWrapper")
            TarCompressorUtils.extract(
                TarCompressorUtils.Type.ZSTD,
                context,
                "dxwrapper/$vkd3dWrapper.tzst",
                windowsDir
            )
        }
        
        if (!ddrawWrapper.equals("None", ignoreCase = true)) {
            Log.d(TAG, "Extracting DDraw: $ddrawWrapper")
            TarCompressorUtils.extract(
                TarCompressorUtils.Type.ZSTD,
                context,
                "ddrawrapper/$ddrawWrapper.tzst",
                windowsDir
            )
        }
        
        Log.d(TAG, "Extracting nglide")
        TarCompressorUtils.extract(
            TarCompressorUtils.Type.ZSTD,
            context,
            "ddrawrapper/nglide.tzst",
            windowsDir
        )
        
        container.putExtra("dxwrapper", dxwrapper)
        container.saveData()
    }
    
    fun extractZinkDllsIfNeeded(context: Context, container: Container, wineInfo: WineInfo) {
        if (!wineInfo.isArm64EC()) {
            return
        }
        
        val firstBoot = container.getExtra("firstBoot").isEmpty()
        if (!firstBoot) {
            return
        }
        
        val imageFs = ImageFs.find(context)
        val windowsDir = File(imageFs.getRootDir(), "${ImageFs.WINEPREFIX}/drive_c/windows")
        
        Log.d(TAG, "First boot - extracting Zink DLLs for ARM64EC")
        TarCompressorUtils.extract(
            TarCompressorUtils.Type.ZSTD,
            context,
            "graphics_driver/zink_dlls.tzst",
            windowsDir
        )
        
        container.putExtra("firstBoot", "done")
        container.saveData()
    }
}

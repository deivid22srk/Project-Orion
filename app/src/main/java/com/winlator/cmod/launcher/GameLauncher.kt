package com.winlator.cmod.launcher

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.winlator.cmod.core.Callback
import com.winlator.cmod.core.EnvVars
import com.winlator.cmod.core.ProcessHelper
import com.winlator.cmod.core.WineInfo
import com.winlator.cmod.data.Container
import com.winlator.cmod.data.Shortcut
import com.winlator.cmod.xenvironment.ImageFs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Launcher mínimo para executar jogos Wine/Proton
 * Usa apenas os componentes essenciais do Winlator
 */
object GameLauncher {
    private const val TAG = "GameLauncher"
    
    fun launchGame(
        activity: Activity,
        container: Container,
        shortcut: Shortcut?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Toast.makeText(
                    activity,
                    "Iniciando ${shortcut?.name ?: "jogo"}...",
                    Toast.LENGTH_SHORT
                ).show()
                
                withContext(Dispatchers.IO) {
                    executeGame(activity, container, shortcut)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch game", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        activity,
                        "Erro ao iniciar jogo: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun executeGame(
        context: Context,
        container: Container,
        shortcut: Shortcut?
    ) {
        Log.d(TAG, "Launching game: ${shortcut?.name ?: "Container"}")
        
        val imageFs = ImageFs.find(context)
        val wineInfo = WineInfo.fromIdentifier(context, null, container.wineVersion)
        
        com.winlator.cmod.core.WineComponentsHelper.extractWinComponentsIfNeeded(context, container)
        com.winlator.cmod.core.WineComponentsHelper.extractDXWrapperIfNeeded(context, container)
        com.winlator.cmod.core.WineComponentsHelper.extractZinkDllsIfNeeded(context, container, wineInfo)
        
        val screenSize = container.screenSize
        val execPath = shortcut?.path ?: ""
        val execArgs = ""
        
        val command = buildWineCommand(imageFs, wineInfo, screenSize, execPath, execArgs)
        val envVars = buildEnvironmentVariables(context, container, imageFs, wineInfo)
        
        Log.d(TAG, "Executing: $command")
        Log.d(TAG, "Environment: ${envVars.toStringArray().size} vars")
        
        ProcessHelper.exec(
            command,
            envVars.toStringArray(),
            File(imageFs.getRootDir().absolutePath),
            Callback {
                Log.d(TAG, "Game process terminated")
            }
        )
    }
    
    private fun buildWineCommand(
        imageFs: ImageFs,
        wineInfo: WineInfo,
        screenSize: String,
        execPath: String,
        execArgs: String
    ): String {
        val rootDir = imageFs.getRootDir().absolutePath
        
        return if (wineInfo.isArm64EC()) {
            val winePath = "${wineInfo.path}/bin/wine"
            if (execPath.isNotEmpty()) {
                "$winePath explorer /desktop=shell,$screenSize \"$execPath\" $execArgs"
            } else {
                "$winePath explorer /desktop=shell,$screenSize"
            }
        } else {
            val box64Path = "$rootDir/usr/bin/box64"
            val winePath = "${wineInfo.path}/bin/wine"
            if (execPath.isNotEmpty()) {
                "$box64Path $winePath explorer /desktop=shell,$screenSize \"$execPath\" $execArgs"
            } else {
                "$box64Path $winePath explorer /desktop=shell,$screenSize"
            }
        }
    }
    
    private fun buildEnvironmentVariables(
        context: Context,
        container: Container,
        imageFs: ImageFs,
        wineInfo: WineInfo
    ): EnvVars {
        val rootDir = imageFs.getRootDir().absolutePath
        val winePrefix = "$rootDir/home/xuser/.wine"
        
        val envVars = EnvVars()
        
        envVars.put("HOME", "$rootDir/home/xuser")
        envVars.put("TMPDIR", "$rootDir/usr/tmp")
        envVars.put("WINEPREFIX", winePrefix)
        envVars.put("PATH", "${wineInfo.path}/bin:$rootDir/usr/bin:/system/bin:/system/xbin")
        
        envVars.put("LD_LIBRARY_PATH", "$rootDir/usr/lib:/system/lib64:/system/lib")
        envVars.put("XDG_DATA_DIRS", "$rootDir/usr/share")
        envVars.put("XDG_CONFIG_DIRS", "$rootDir/usr/etc/xdg")
        envVars.put("XDG_CACHE_HOME", "$rootDir/home/xuser/.cache")
        
        envVars.put("WINEDLLOVERRIDES", "mscoree,mshtml=")
        envVars.put("WINEESYNC", "1")
        envVars.put("WINE_NO_DUPLICATE_EXPLORER", "1")
        envVars.put("WINE_DISABLE_FULLSCREEN_HACK", "1")
        
        envVars.put("DISPLAY", ":0")
        envVars.put("WINE_X11FORCEGLX", "1")
        envVars.put("WINE_GST_NO_GL", "1")
        
        envVars.put("GALLIUM_DRIVER", "zink")
        envVars.put("VK_ICD_FILENAMES", "$rootDir/usr/share/vulkan/icd.d/wrapper_icd.aarch64.json")
        envVars.put("MESA_SHADER_CACHE_DISABLE", "false")
        envVars.put("mesa_glthread", "true")
        envVars.put("ZINK_DESCRIPTORS", "lazy")
        envVars.put("ZINK_DEBUG", "compact")
        
        envVars.put("BOX64_DYNAREC", "1")
        envVars.put("BOX64_LOG", "0")
        envVars.put("BOX64_SHOWSEGV", "0")
        
        parseDXWrapperConfig(container, envVars)
        
        if (container.envVars.isNotEmpty()) {
            envVars.putAll(container.envVars)
        }
        
        return envVars
    }
    
    private fun parseDXWrapperConfig(container: Container, envVars: EnvVars) {
        val dxwrapper = container.dxwrapper
        
        if (dxwrapper.contains("dxvk")) {
            envVars.put("DXVK_HUD", "fps")
            envVars.put("DXVK_STATE_CACHE", "1")
        }
        
        container.envVars.split(" ").forEach { pair ->
            if (pair.contains("=")) {
                val parts = pair.split("=", limit = 2)
                if (parts.size == 2) {
                    envVars.put(parts[0], parts[1])
                }
            }
        }
    }
}

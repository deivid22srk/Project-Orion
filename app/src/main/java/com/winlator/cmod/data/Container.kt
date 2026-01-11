package com.winlator.cmod.data

import android.os.Environment
import android.os.Parcelable
import com.winlator.cmod.core.FileUtils
import com.winlator.cmod.core.WineInfo
import com.winlator.cmod.xenvironment.ImageFs
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject
import java.io.File

@Parcelize
data class Container(
    val id: Int,
    var name: String = "Container-$id",
    var screenSize: String = DEFAULT_SCREEN_SIZE,
    var envVars: String = DEFAULT_ENV_VARS,
    var graphicsDriver: String = DEFAULT_GRAPHICS_DRIVER,
    var graphicsDriverConfig: String = DEFAULT_GRAPHICSDRIVERCONFIG,
    var dxwrapper: String = DEFAULT_DXWRAPPER,
    var dxwrapperConfig: String = "",
    var audioDriver: String = DEFAULT_AUDIO_DRIVER,
    var drives: String = DEFAULT_DRIVES,
    var wineVersion: String = WineInfo.MAIN_WINE_VERSION.identifier(),
    var showFPS: Boolean = false,
    var fullscreenStretched: Boolean = false,
    var startupSelection: Byte = STARTUP_SELECTION_ESSENTIAL,
    var cpuList: String? = null,
    var cpuListWoW64: String? = null,
    var desktopTheme: String = "default",
    var fexcoreVersion: String? = null,
    var fexcorePreset: String = "INTERMEDIATE",
    var box64Preset: String = "COMPATIBILITY",
    var box64Version: String? = null,
    var emulator: String? = DEFAULT_EMULATOR,
    var wincomponents: String = DEFAULT_WINCOMPONENTS,
    var midiSoundFont: String = "",
    var inputType: Int = 0,
    var lcAll: String = "",
    var primaryController: Int = 1,
    var controllerMapping: String = "",
    var rootDir: File? = null
) : Parcelable {
    
    @IgnoredOnParcel
    private var extraData: JSONObject? = null
    
    companion object {
        const val DEFAULT_ENV_VARS = "WRAPPER_MAX_IMAGE_COUNT=0 ZINK_DESCRIPTORS=lazy ZINK_DEBUG=compact MESA_SHADER_CACHE_DISABLE=false MESA_SHADER_CACHE_MAX_SIZE=512MB mesa_glthread=true WINEESYNC=1 TU_DEBUG=noconform,sysmem DXVK_HUD=devinfo,fps,frametimes,gpuload,version,api"
        const val DEFAULT_SCREEN_SIZE = "1280x720"
        const val DEFAULT_GRAPHICS_DRIVER = "wrapper"
        const val DEFAULT_AUDIO_DRIVER = "alsa"
        const val DEFAULT_EMULATOR = "FEXCore"
        const val DEFAULT_DXWRAPPER = "dxvk+vkd3d"
        const val DEFAULT_GRAPHICSDRIVERCONFIG = "vulkanVersion=1.3;version=;blacklistedExtensions=;maxDeviceMemory=0;presentMode=mailbox;syncFrame=0;disablePresentWait=0;resourceType=auto;bcnEmulation=auto;bcnEmulationType=software;bcnEmulationCache=0"
        const val DEFAULT_WINCOMPONENTS = "direct3d=1,directsound=0,directmusic=0,directshow=0,directplay=0,xaudio=0,vcrun2010=1"
        val DEFAULT_DRIVES = "D:${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}"
        
        const val STARTUP_SELECTION_NORMAL: Byte = 0
        const val STARTUP_SELECTION_ESSENTIAL: Byte = 1
        const val STARTUP_SELECTION_AGGRESSIVE: Byte = 2
        
        fun getFallbackCPUList(): String {
            val numProcessors = Runtime.getRuntime().availableProcessors()
            return (0 until numProcessors).joinToString(",")
        }
        
        fun getFallbackCPUListWoW64(): String {
            val numProcessors = Runtime.getRuntime().availableProcessors()
            return (numProcessors / 2 until numProcessors).joinToString(",")
        }
    }
    
    fun getConfigFile(): File? = rootDir?.let { File(it, ".container") }
    
    fun getDesktopDir(): File? = rootDir?.let { 
        File(it, ".wine/drive_c/users/${ImageFs.USER}/Desktop/") 
    }
    
    fun getStartMenuDir(): File? = rootDir?.let {
        File(it, ".wine/drive_c/ProgramData/Microsoft/Windows/Start Menu/")
    }
    
    fun getIconsDir(size: Int): File? = rootDir?.let {
        File(it, ".local/share/icons/hicolor/${size}x${size}/apps/")
    }
    
    fun getExtra(name: String, fallback: String = ""): String {
        return try {
            if (extraData?.has(name) == true) {
                extraData?.getString(name) ?: fallback
            } else {
                fallback
            }
        } catch (e: JSONException) {
            fallback
        }
    }
    
    fun putExtra(name: String, value: String?) {
        if (extraData == null) extraData = JSONObject()
        try {
            if (value != null) {
                extraData?.put(name, value)
            } else {
                extraData?.remove(name)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }
    
    fun saveData() {
        rootDir?.let { dir ->
            try {
                val data = JSONObject().apply {
                    put("id", id)
                    put("name", name)
                    put("screenSize", screenSize)
                    put("envVars", envVars)
                    put("cpuList", cpuList)
                    put("cpuListWoW64", cpuListWoW64)
                    put("graphicsDriver", graphicsDriver)
                    put("graphicsDriverConfig", graphicsDriverConfig)
                    put("emulator", emulator)
                    put("dxwrapper", dxwrapper)
                    if (dxwrapperConfig.isNotEmpty()) put("dxwrapperConfig", dxwrapperConfig)
                    put("audioDriver", audioDriver)
                    put("wincomponents", wincomponents)
                    put("drives", drives)
                    put("showFPS", showFPS)
                    put("fullscreenStretched", fullscreenStretched)
                    put("inputType", inputType)
                    put("startupSelection", startupSelection)
                    put("box64Version", box64Version)
                    put("fexcorePreset", fexcorePreset)
                    put("fexcoreVersion", fexcoreVersion)
                    put("box64Preset", box64Preset)
                    put("desktopTheme", desktopTheme)
                    if (extraData != null) put("extraData", extraData)
                    put("midiSoundFont", midiSoundFont)
                    put("lc_all", lcAll)
                    put("primaryController", primaryController)
                    put("controllerMapping", controllerMapping)
                    if (!WineInfo.isMainWineVersion(wineVersion)) put("wineVersion", wineVersion)
                }
                
                val configFile = File(dir, ".container")
                FileUtils.writeString(configFile, data.toString())
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }
    
    fun loadData(data: JSONObject) {
        try {
            data.optString("name")?.let { name = it }
            data.optString("screenSize")?.let { screenSize = it }
            data.optString("envVars")?.let { envVars = it }
            data.optString("cpuList")?.let { cpuList = it }
            data.optString("cpuListWoW64")?.let { cpuListWoW64 = it }
            data.optString("graphicsDriver")?.let { graphicsDriver = it }
            data.optString("graphicsDriverConfig")?.let { graphicsDriverConfig = it }
            data.optString("emulator")?.let { emulator = it }
            data.optString("dxwrapper")?.let { dxwrapper = it }
            data.optString("dxwrapperConfig")?.let { dxwrapperConfig = it }
            data.optString("audioDriver")?.let { audioDriver = it }
            data.optString("wincomponents")?.let { wincomponents = it }
            data.optString("drives")?.let { drives = it }
            showFPS = data.optBoolean("showFPS", false)
            fullscreenStretched = data.optBoolean("fullscreenStretched", false)
            inputType = data.optInt("inputType", 0)
            startupSelection = data.optInt("startupSelection", STARTUP_SELECTION_ESSENTIAL.toInt()).toByte()
            data.optString("box64Version")?.let { box64Version = it }
            data.optString("fexcorePreset")?.let { fexcorePreset = it }
            data.optString("fexcoreVersion")?.let { fexcoreVersion = it }
            data.optString("box64Preset")?.let { box64Preset = it }
            data.optString("desktopTheme")?.let { desktopTheme = it }
            data.optString("midiSoundFont")?.let { midiSoundFont = it }
            data.optString("lc_all")?.let { lcAll = it }
            primaryController = data.optInt("primaryController", 1)
            data.optString("controllerMapping")?.let { controllerMapping = it }
            data.optString("wineVersion")?.let { wineVersion = it }
            
            if (data.has("extraData")) {
                extraData = data.getJSONObject("extraData")
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }
    
    fun getCPUList(allowFallback: Boolean = false): String? {
        return cpuList ?: if (allowFallback) getFallbackCPUList() else null
    }
    
    fun getCPUListWoW64(allowFallback: Boolean = false): String? {
        return cpuListWoW64 ?: if (allowFallback) getFallbackCPUListWoW64() else null
    }
}

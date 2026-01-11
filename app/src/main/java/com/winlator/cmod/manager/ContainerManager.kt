package com.winlator.cmod.manager

import android.content.Context
import android.util.Log
import com.winlator.cmod.core.FileUtils
import com.winlator.cmod.core.TarCompressorUtils
import com.winlator.cmod.core.WineInfo
import com.winlator.cmod.data.Container
import com.winlator.cmod.data.Shortcut
import com.winlator.cmod.contents.ContentsManager
import com.winlator.cmod.xenvironment.ImageFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class ContainerManager(private val context: Context) {
    
    private val containers = mutableListOf<Container>()
    private var maxContainerId = 0
    private val homeDir: File
    private var isInitialized = false
    
    init {
        val rootDir = ImageFs.find(context).getRootDir()
        homeDir = File(rootDir, "home")
        homeDir.mkdirs()
        loadContainers()
        isInitialized = true
    }
    
    fun getContainers(): List<Container> = containers.toList()
    
    fun isInitialized(): Boolean = isInitialized
    
    private fun loadContainers() {
        containers.clear()
        maxContainerId = 0
        
        try {
            homeDir.listFiles()?.forEach { file ->
                if (file.isDirectory && file.name.startsWith("${ImageFs.USER}-")) {
                    try {
                        val id = file.name.replace("${ImageFs.USER}-", "").toInt()
                        val container = Container(id = id)
                        
                        val rootDir = File(homeDir, "${ImageFs.USER}-$id")
                        container.rootDir = rootDir
                        
                        val configFile = container.getConfigFile()
                        if (configFile?.exists() == true) {
                            val data = JSONObject(FileUtils.readString(configFile))
                            container.loadData(data)
                            containers.add(container)
                            maxContainerId = maxOf(maxContainerId, id)
                        }
                    } catch (e: Exception) {
                        Log.e("ContainerManager", "Error loading container from ${file.name}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ContainerManager", "Error loading containers", e)
        }
    }
    
    fun activateContainer(container: Container) {
        container.rootDir = File(homeDir, "${ImageFs.USER}-${container.id}")
        val file = File(homeDir, ImageFs.USER)
        file.delete()
        FileUtils.symlink("./${ImageFs.USER}-${container.id}", file.path)
    }
    
    suspend fun createContainerAsync(
        data: JSONObject,
        contentsManager: ContentsManager?
    ): Container? = withContext(Dispatchers.IO) {
        createContainer(data, contentsManager)
    }
    
    private fun createContainer(data: JSONObject, contentsManager: ContentsManager?): Container? {
        return try {
            val id = maxContainerId + 1
            data.put("id", id)
            
            val containerDir = File(homeDir, "${ImageFs.USER}-$id")
            if (!containerDir.mkdirs()) return null
            
            val container = Container(id = id)
            container.rootDir = containerDir
            container.loadData(data)
            
            val wineVersion = data.getString("wineVersion")
            container.wineVersion = wineVersion
            
            if (!extractContainerPatternFile(container, wineVersion, contentsManager, containerDir, null)) {
                FileUtils.delete(containerDir)
                return null
            }
            
            container.saveData()
            maxContainerId++
            containers.add(container)
            container
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun extractContainerPatternFile(
        container: Container,
        wineVersion: String,
        contentsManager: ContentsManager?,
        containerDir: File,
        listener: TarCompressorUtils.OnExtractFileListener?
    ): Boolean {
        val wineInfo = WineInfo.fromIdentifier(context, contentsManager, wineVersion)
        val containerPattern = "${wineVersion}_container_pattern.tzst"
        
        var result = TarCompressorUtils.extract(
            TarCompressorUtils.Type.ZSTD,
            context,
            containerPattern,
            containerDir,
            listener
        )
        
        if (!result) {
            val containerPatternFile = File("${wineInfo.path}/prefixPack.txz")
            result = TarCompressorUtils.extract(
                TarCompressorUtils.Type.XZ,
                containerPatternFile,
                containerDir,
                listener
            )
        }
        
        if (result) {
            try {
                if (wineInfo.isArm64EC()) {
                    extractCommonDlls(wineInfo, "aarch64-windows", "system32", containerDir, listener)
                } else {
                    extractCommonDlls(wineInfo, "x86_64-windows", "system32", containerDir, listener)
                }
                
                extractCommonDlls(wineInfo, "i386-windows", "syswow64", containerDir, listener)
            } catch (e: Exception) {
                return false
            }
        }
        
        return result
    }
    
    private fun extractCommonDlls(
        wineInfo: WineInfo,
        srcName: String,
        dstName: String,
        containerDir: File,
        listener: TarCompressorUtils.OnExtractFileListener?
    ) {
        val srcDir = File("${wineInfo.path}/lib/wine/$srcName")
        
        val srcFiles = srcDir.listFiles { file -> file.isFile } ?: return
        
        for (file in srcFiles) {
            val dllName = file.name
            
            var sourceFile = file
            if (dllName == "iexplore.exe" && wineInfo.isArm64EC() && srcName == "aarch64-windows") {
                sourceFile = File("${wineInfo.path}/lib/wine/i386-windows/iexplore.exe")
            }
            
            if (dllName == "tabtip.exe" || dllName == "icu.dll") continue
            
            var dstFile = File(containerDir, ".wine/drive_c/windows/$dstName/$dllName")
            if (dstFile.exists()) continue
            
            if (listener != null) {
                val result = listener.onExtractFile(dstFile, 0)
                if (result == null) continue
                dstFile = result
            }
            
            FileUtils.copy(sourceFile, dstFile)
        }
    }
    
    suspend fun duplicateContainerAsync(container: Container): Container? = withContext(Dispatchers.IO) {
        duplicateContainer(container)
    }
    
    private fun duplicateContainer(srcContainer: Container): Container? {
        val id = maxContainerId + 1
        
        val dstDir = File(homeDir, "${ImageFs.USER}-$id")
        if (!dstDir.mkdirs()) return null
        
        if (!FileUtils.copy(srcContainer.rootDir!!, dstDir) { file ->
            FileUtils.chmod(file, 0x303)
        }) {
            FileUtils.delete(dstDir)
            return null
        }
        
        val dstContainer = Container(id = id).apply {
            rootDir = dstDir
            name = "${srcContainer.name} (Cópia)"
            screenSize = srcContainer.screenSize
            envVars = srcContainer.envVars
            cpuList = srcContainer.cpuList
            cpuListWoW64 = srcContainer.cpuListWoW64
            graphicsDriver = srcContainer.graphicsDriver
            dxwrapper = srcContainer.dxwrapper
            dxwrapperConfig = srcContainer.dxwrapperConfig
            audioDriver = srcContainer.audioDriver
            wincomponents = srcContainer.wincomponents
            drives = srcContainer.drives
            showFPS = srcContainer.showFPS
            startupSelection = srcContainer.startupSelection
            box64Preset = srcContainer.box64Preset
            desktopTheme = srcContainer.desktopTheme
            wineVersion = srcContainer.wineVersion
        }
        
        dstContainer.saveData()
        
        maxContainerId++
        containers.add(dstContainer)
        
        return dstContainer
    }
    
    suspend fun removeContainerAsync(container: Container): Boolean = withContext(Dispatchers.IO) {
        removeContainer(container)
    }
    
    private fun removeContainer(container: Container): Boolean {
        return if (container.rootDir?.let { FileUtils.delete(it) } == true) {
            containers.remove(container)
            true
        } else {
            false
        }
    }
    
    fun getAllShortcuts(): List<Shortcut> {
        val shortcuts = mutableListOf<Shortcut>()
        
        for (container in containers) {
            val desktopDir = container.getDesktopDir() ?: continue
            
            if (!desktopDir.exists()) continue
            
            desktopDir.listFiles()?.forEach { file ->
                when {
                    file.name.endsWith(".desktop") -> {
                        Shortcut.fromFile(container, file)?.let { shortcuts.add(it) }
                    }
                }
            }
        }
        
        return shortcuts.sortedBy { it.name }
    }
    
    fun getShortcuts(container: Container): List<Shortcut> {
        val shortcuts = mutableListOf<Shortcut>()
        
        val desktopDir = container.getDesktopDir() ?: return shortcuts
        
        desktopDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".desktop")) {
                Shortcut.fromFile(container, file)?.let { shortcuts.add(it) }
            }
        }
        
        return shortcuts.sortedBy { it.name }
    }
    
    fun getNextContainerId(): Int {
        return maxContainerId + 1
    }
    
    fun getContainerById(id: Int): Container? {
        return containers.find { it.id == id }
    }
    
    fun getContainerForShortcut(shortcut: Shortcut): Container? {
        return containers.find { it.id == shortcut.container.id }
    }
}

package com.winlator.cmod.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.winlator.cmod.contents.ContentsManager
import com.winlator.cmod.data.Container
import com.winlator.cmod.manager.ContainerManager
import com.winlator.cmod.ui.screens.ContainerData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class ContainerViewModel(application: Application) : AndroidViewModel(application) {
    
    private val containerManager = ContainerManager(application)
    private val contentsManager = ContentsManager()
    
    private val _containers = MutableStateFlow<List<Container>>(emptyList())
    val containers: StateFlow<List<Container>> = _containers.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadContainers()
    }
    
    fun loadContainers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _containers.value = containerManager.getContainers()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun createContainer(containerData: ContainerData) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = JSONObject().apply {
                    put("name", containerData.name)
                    put("wineVersion", containerData.wineVersion)
                    put("screenSize", containerData.screenSize)
                    put("graphicsDriver", containerData.graphicsDriver)
                    put("graphicsDriverConfig", containerData.graphicsDriverConfig)
                    put("dxwrapper", containerData.dxwrapper)
                    put("dxwrapperConfig", containerData.dxwrapperConfig)
                    put("audioDriver", containerData.audioDriver)
                    put("emulator", containerData.emulator)
                    put("envVars", Container.DEFAULT_ENV_VARS)
                    put("wincomponents", Container.DEFAULT_WINCOMPONENTS)
                    put("drives", Container.DEFAULT_DRIVES)
                    put("showFPS", false)
                    put("fullscreenStretched", false)
                    put("startupSelection", Container.STARTUP_SELECTION_ESSENTIAL)
                    put("box64Preset", "COMPATIBILITY")
                    put("fexcorePreset", "INTERMEDIATE")
                    put("desktopTheme", "default")
                    put("inputType", 0)
                    containerData.fexcoreVersion?.let { put("fexcoreVersion", it) }
                    containerData.box64Version?.let { put("box64Version", it) }
                }
                
                val container = containerManager.createContainerAsync(data, contentsManager)
                if (container != null) {
                    loadContainers()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteContainer(container: Container) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                containerManager.removeContainerAsync(container)
                loadContainers()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun duplicateContainer(container: Container) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                containerManager.duplicateContainerAsync(container)
                loadContainers()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

package com.winlator.cmod.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.winlator.cmod.data.Container
import com.winlator.cmod.data.Shortcut
import com.winlator.cmod.manager.ContainerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GamesViewModel(application: Application) : AndroidViewModel(application) {
    
    private val containerManager = ContainerManager(application)
    
    private val _shortcuts = MutableStateFlow<List<Shortcut>>(emptyList())
    val shortcuts: StateFlow<List<Shortcut>> = _shortcuts.asStateFlow()
    
    private val _containers = MutableStateFlow<List<Container>>(emptyList())
    val containers: StateFlow<List<Container>> = _containers.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadData()
    }
    
    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _containers.value = containerManager.getContainers()
                _shortcuts.value = containerManager.getAllShortcuts()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadShortcuts() = loadData()
}

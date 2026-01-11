package com.winlator.cmod

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.winlator.cmod.ui.components.InstallationDialog
import com.winlator.cmod.ui.navigation.WinlatorNavigation
import com.winlator.cmod.ui.theme.WinlatorTheme
import com.winlator.cmod.xenvironment.ImageFsInstaller
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 1
    }
    
    private var isInstallationComplete by mutableStateOf(false)
    private var installationProgress by mutableStateOf(0)
    private var isInstalling by mutableStateOf(false)
    
    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            checkAndInstallImageFs()
        } else {
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestAppPermissions()
        
        setContent {
            WinlatorTheme(dynamicColor = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!isInstallationComplete) {
                        InstallationDialog(
                            isInstalling = isInstalling,
                            progress = installationProgress
                        )
                    } else {
                        val navController = rememberNavController()
                        WinlatorNavigation(navController = navController)
                    }
                }
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            showAllFilesAccessDialog()
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
            }
        }
    }
    
    private fun checkAndInstallImageFs() {
        ImageFsInstaller.installIfNeeded(
            context = this,
            onInstallStart = {
                isInstalling = true
                installationProgress = 0
            },
            onProgress = { progress ->
                installationProgress = progress
            },
            onComplete = { success ->
                isInstalling = false
                isInstallationComplete = success
                if (!success) {
                    finish()
                }
            }
        )
        
        if (!isInstalling) {
            isInstallationComplete = true
        }
    }
    
    private fun requestAppPermissions() {
        val hasWritePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasReadPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasManageStoragePermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
                Environment.isExternalStorageManager()
        
        if (hasWritePermission && hasReadPermission && hasManageStoragePermission) {
            checkAndInstallImageFs()
            return
        }
        
        if (!hasWritePermission || !hasReadPermission) {
            val permissions = arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            requestPermissionLauncher.launch(permissions)
        }
    }
    
    private fun showAllFilesAccessDialog() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkAndInstallImageFs()
            } else {
                finish()
            }
        }
    }
}

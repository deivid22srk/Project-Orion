package com.winlator.cmod.ui.components

import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePickerDialog(
    initialDirectory: File = Environment.getExternalStorageDirectory(),
    fileExtensions: List<String> = listOf(".exe", ".bat", ".msi"),
    onDismiss: () -> Unit,
    onFileSelected: (File) -> Unit
) {
    var currentDirectory by remember { mutableStateOf(initialDirectory) }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    
    LaunchedEffect(currentDirectory) {
        files = loadDirectory(currentDirectory, fileExtensions)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.8f)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Selecionar Executável",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentDirectory.absolutePath,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                val parent = currentDirectory.parentFile
                                if (parent != null && parent.canRead()) {
                                    currentDirectory = parent
                                }
                            },
                            enabled = currentDirectory.parentFile != null && 
                                     currentDirectory.parentFile?.canRead() == true
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                
                if (files.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Pasta vazia ou sem permissão",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(files) { file ->
                            FileItem(
                                file = file,
                                onClick = {
                                    if (file.isDirectory) {
                                        currentDirectory = file
                                    } else {
                                        onFileSelected(file)
                                        onDismiss()
                                    }
                                }
                            )
                            Divider()
                        }
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}

@Composable
private fun FileItem(
    file: File,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
            contentDescription = null,
            tint = if (file.isDirectory) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondary
            },
            modifier = Modifier.size(32.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (file.isDirectory || isExecutable(file)) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                }
            )
            Text(
                text = if (file.isDirectory) {
                    val itemCount = file.listFiles()?.size ?: 0
                    "$itemCount itens"
                } else {
                    formatSize(file.length())
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

private fun loadDirectory(directory: File, fileExtensions: List<String>): List<File> {
    if (!directory.canRead()) return emptyList()
    
    val fileList = directory.listFiles()?.toList() ?: emptyList()
    
    return fileList.sortedWith(compareBy<File> { !it.isDirectory }
        .thenBy { !isExecutable(it) }
        .thenBy { it.name.lowercase() })
}

private fun isExecutable(file: File): Boolean {
    val name = file.name.lowercase()
    return name.endsWith(".exe") || name.endsWith(".bat") || name.endsWith(".msi")
}

private fun formatSize(size: Long): String {
    if (size < 1024) return "$size B"
    val z = (63 - java.lang.Long.numberOfLeadingZeros(size)) / 10
    return String.format("%.1f %sB", size.toDouble() / (1L shl (z * 10)), " KMGTPE"[z])
}

package com.winlator.cmod.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.winlator.cmod.data.Container
import com.winlator.cmod.manager.ShortcutManager
import com.winlator.cmod.ui.screens.DropdownSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGameDialog(
    containers: List<Container>,
    onDismiss: () -> Unit,
    onGameAdded: () -> Unit
) {
    var gameName by remember { mutableStateOf("") }
    var selectedContainer by remember { mutableStateOf(containers.firstOrNull()) }
    var selectedExecutablePath by remember { mutableStateOf("") }
    var execArgs by remember { mutableStateOf("") }
    var showFilePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val shortcutManager = remember { ShortcutManager(context) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Jogo", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = gameName,
                    onValueChange = { gameName = it },
                    label = { Text("Nome do Jogo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                if (containers.isNotEmpty()) {
                    DropdownSelector(
                        label = "Container",
                        options = containers.map { it.name },
                        selectedOption = selectedContainer?.name ?: "",
                        onOptionSelected = { name ->
                            selectedContainer = containers.find { it.name == name }
                        }
                    )
                }
                
                OutlinedTextField(
                    value = selectedExecutablePath,
                    onValueChange = { selectedExecutablePath = it },
                    label = { Text("Caminho do Executável") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = true,
                    trailingIcon = {
                        TextButton(onClick = { showFilePicker = true }) {
                            Text("Buscar")
                        }
                    }
                )
                
                OutlinedTextField(
                    value = execArgs,
                    onValueChange = { execArgs = it },
                    label = { Text("Argumentos (Opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedContainer?.let { container ->
                        val shortcut = shortcutManager.createShortcut(
                            container = container,
                            name = gameName,
                            executablePath = selectedExecutablePath,
                            execArgs = execArgs
                        )
                        
                        if (shortcut != null) {
                            Toast.makeText(context, "Jogo adicionado com sucesso!", Toast.LENGTH_SHORT).show()
                            onGameAdded()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Erro ao adicionar jogo", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = gameName.isNotEmpty() && selectedExecutablePath.isNotEmpty() && selectedContainer != null
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
    
    if (showFilePicker) {
        FilePickerDialog(
            onDismiss = { showFilePicker = false },
            onFileSelected = { file ->
                selectedExecutablePath = file.absolutePath
                if (gameName.isEmpty()) {
                    gameName = file.name.substringBeforeLast(".")
                }
                showFilePicker = false
            }
        )
    }
}

package com.winlator.cmod.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.winlator.cmod.data.Container
import com.winlator.cmod.data.DXWrapperConfig
import com.winlator.cmod.data.GraphicsDriverConfig
import com.winlator.cmod.core.DefaultVersion
import com.winlator.cmod.viewmodel.ContainerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainersScreen(
    navController: NavHostController,
    viewModel: ContainerViewModel = viewModel()
) {
    val containers by viewModel.containers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Containers", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Adicionar") },
                text = { Text("Novo Container") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                    )
                }
                containers.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(androidx.compose.ui.Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Nenhum container encontrado",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Toque no botão + para criar um",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
                else -> {
                    ContainersList(
                        containers = containers,
                        onContainerClick = { },
                        onDeleteContainer = { viewModel.deleteContainer(it) },
                        onDuplicateContainer = { viewModel.duplicateContainer(it) }
                    )
                }
            }
        }
        
        if (showCreateDialog) {
            CreateContainerDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { containerData ->
                    viewModel.createContainer(containerData)
                    showCreateDialog = false
                }
            )
        }
    }
}

@Composable
fun ContainersList(
    containers: List<Container>,
    onContainerClick: (Container) -> Unit,
    onDeleteContainer: (Container) -> Unit,
    onDuplicateContainer: (Container) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        containers.forEach { container ->
            ContainerCard(
                container = container,
                onClick = { onContainerClick(container) },
                onDelete = { onDeleteContainer(container) },
                onDuplicate = { onDuplicateContainer(container) }
            )
        }
    }
}

@Composable
fun ContainerCard(
    container: Container,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = container.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Wine: ${container.wineVersion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = container.screenSize,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu"
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Duplicar") },
                        onClick = {
                            showMenu = false
                            onDuplicate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Excluir") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CreateContainerDialog(
    onDismiss: () -> Unit,
    onCreate: (ContainerData) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedWineVersion by remember { mutableStateOf("proton-9.0-x86_64") }
    var selectedScreenSize by remember { mutableStateOf("1280x720") }
    var selectedGraphicsDriver by remember { mutableStateOf("wrapper") }
    var selectedDxWrapper by remember { mutableStateOf("dxvk+vkd3d") }
    var selectedAudioDriver by remember { mutableStateOf("alsa") }
    var selectedEmulator by remember { mutableStateOf("FEXCore") }
    
    var selectedTurnipVersion by remember { mutableStateOf(DefaultVersion.WRAPPER) }
    var selectedDxvkVersion by remember { mutableStateOf(DefaultVersion.DXVK) }
    var selectedVkd3dVersion by remember { mutableStateOf(DefaultVersion.VKD3D) }
    var selectedFexVersion by remember { mutableStateOf(DefaultVersion.FEXCORE) }
    var selectedBox64Version by remember { mutableStateOf(DefaultVersion.BOX64) }
    
    val wineVersions = listOf(
        "proton-9.0-x86_64",
        "proton-9.0-arm64ec"
    )
    
    val screenSizes = listOf(
        "800x600",
        "1024x768",
        "1280x720",
        "1280x800",
        "1366x768",
        "1600x900",
        "1920x1080"
    )
    
    val graphicsDrivers = listOf("wrapper", "turnip", "virgl")
    val dxWrappers = listOf("dxvk+vkd3d", "dxvk", "wined3d")
    val audioDrivers = listOf("alsa", "pulseaudio")
    val emulators = listOf("FEXCore", "Box64")
    
    val turnipVersions = listOf("System", "turnip25.1.0", "turnip24.3.0", "turnip24.2.0")
    val dxvkVersions = listOf("2.3.1", "2.3", "2.2", "2.1", "2.0")
    val vkd3dVersions = listOf("None", "2.11", "2.10", "2.9")
    val fexVersions = listOf("2508", "2407", "2306")
    val box64Versions = listOf("0.3.7", "0.3.6", "0.3.5")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Criar Novo Container", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Container") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                DropdownSelector(
                    label = "Versão do Wine",
                    options = wineVersions,
                    selectedOption = selectedWineVersion,
                    onOptionSelected = { selectedWineVersion = it }
                )
                
                DropdownSelector(
                    label = "Resolução da Tela",
                    options = screenSizes,
                    selectedOption = selectedScreenSize,
                    onOptionSelected = { selectedScreenSize = it }
                )
                
                DropdownSelector(
                    label = "Driver Gráfico",
                    options = graphicsDrivers,
                    selectedOption = selectedGraphicsDriver,
                    onOptionSelected = { selectedGraphicsDriver = it }
                )
                
                DropdownSelector(
                    label = "DX Wrapper",
                    options = dxWrappers,
                    selectedOption = selectedDxWrapper,
                    onOptionSelected = { selectedDxWrapper = it }
                )
                
                DropdownSelector(
                    label = "Driver de Áudio",
                    options = audioDrivers,
                    selectedOption = selectedAudioDriver,
                    onOptionSelected = { selectedAudioDriver = it }
                )
                
                DropdownSelector(
                    label = "Emulador",
                    options = emulators,
                    selectedOption = selectedEmulator,
                    onOptionSelected = { selectedEmulator = it }
                )
                
                Text(
                    text = "Configurações Avançadas",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                DropdownSelector(
                    label = "Versão Turnip/Wrapper",
                    options = turnipVersions,
                    selectedOption = selectedTurnipVersion,
                    onOptionSelected = { selectedTurnipVersion = it }
                )
                
                if (selectedDxWrapper.contains("dxvk")) {
                    DropdownSelector(
                        label = "Versão DXVK",
                        options = dxvkVersions,
                        selectedOption = selectedDxvkVersion,
                        onOptionSelected = { selectedDxvkVersion = it }
                    )
                    
                    DropdownSelector(
                        label = "Versão VKD3D",
                        options = vkd3dVersions,
                        selectedOption = selectedVkd3dVersion,
                        onOptionSelected = { selectedVkd3dVersion = it }
                    )
                }
                
                if (selectedEmulator == "FEXCore" && selectedWineVersion.contains("arm64ec")) {
                    DropdownSelector(
                        label = "Versão FEXCore",
                        options = fexVersions,
                        selectedOption = selectedFexVersion,
                        onOptionSelected = { selectedFexVersion = it }
                    )
                }
                
                if (selectedEmulator == "Box64") {
                    DropdownSelector(
                        label = "Versão Box64",
                        options = box64Versions,
                        selectedOption = selectedBox64Version,
                        onOptionSelected = { selectedBox64Version = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val graphicsDriverConfig = GraphicsDriverConfig(
                        version = selectedTurnipVersion
                    )
                    
                    val dxwrapperConfig = DXWrapperConfig(
                        version = selectedDxvkVersion,
                        vkd3dVersion = selectedVkd3dVersion
                    )
                    
                    onCreate(
                        ContainerData(
                            name = name.ifEmpty { "Novo Container" },
                            wineVersion = selectedWineVersion,
                            screenSize = selectedScreenSize,
                            graphicsDriver = selectedGraphicsDriver,
                            graphicsDriverConfig = graphicsDriverConfig.toConfigString(),
                            dxwrapper = selectedDxWrapper,
                            dxwrapperConfig = dxwrapperConfig.toConfigString(),
                            audioDriver = selectedAudioDriver,
                            emulator = selectedEmulator,
                            fexcoreVersion = if (selectedWineVersion.contains("arm64ec")) selectedFexVersion else null,
                            box64Version = if (selectedEmulator == "Box64") selectedBox64Version else null
                        )
                    )
                },
                enabled = name.isNotEmpty()
            ) {
                Text("Criar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

data class ContainerData(
    val name: String,
    val wineVersion: String,
    val screenSize: String,
    val graphicsDriver: String,
    val graphicsDriverConfig: String,
    val dxwrapper: String,
    val dxwrapperConfig: String,
    val audioDriver: String,
    val emulator: String,
    val fexcoreVersion: String?,
    val box64Version: String?
)

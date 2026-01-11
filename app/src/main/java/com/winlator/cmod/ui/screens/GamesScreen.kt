package com.winlator.cmod.ui.screens

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.winlator.cmod.data.Shortcut
import com.winlator.cmod.ui.components.AddGameDialog
import com.winlator.cmod.viewmodel.GamesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreen(
    navController: NavHostController,
    viewModel: GamesViewModel = viewModel()
) {
    val shortcuts by viewModel.shortcuts.collectAsState()
    val containers by viewModel.containers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    var showAddGameDialog by remember { mutableStateOf(false) }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    onNavigateToContainers = {
                        scope.launch { drawerState.close() }
                        navController.navigate("containers")
                    },
                    onNavigateToSettings = {
                        scope.launch { drawerState.close() }
                        navController.navigate("settings")
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Winlator",
                            fontWeight = FontWeight.Bold
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(Icons.Default.Settings, contentDescription = "Configurações")
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
                    onClick = { 
                        if (containers.isEmpty()) {
                            Toast.makeText(context, "Crie um container primeiro!", Toast.LENGTH_LONG).show()
                            navController.navigate("containers")
                        } else {
                            showAddGameDialog = true
                        }
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Adicionar") },
                    text = { Text("Adicionar Jogo") },
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
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    shortcuts.isEmpty() -> {
                        EmptyGamesState(
                            modifier = Modifier.align(Alignment.Center),
                            hasContainers = containers.isNotEmpty()
                        )
                    }
                    else -> {
                        GamesGrid(
                            shortcuts = shortcuts,
                            onGameClick = { shortcut ->
                                com.winlator.cmod.launcher.GameLauncher.launchGame(
                                    context as android.app.Activity,
                                    shortcut.container,
                                    shortcut
                                )
                            }
                        )
                    }
                }
            }
            
            if (showAddGameDialog) {
                AddGameDialog(
                    containers = containers,
                    onDismiss = { showAddGameDialog = false },
                    onGameAdded = { 
                        viewModel.loadData()
                    }
                )
            }
        }
    }
}

@Composable
fun DrawerContent(
    onNavigateToContainers: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Winlator CMOD",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        NavigationDrawerItem(
            label = { Text("Jogos") },
            selected = true,
            onClick = { }
        )
        
        NavigationDrawerItem(
            label = { Text("Containers") },
            selected = false,
            onClick = onNavigateToContainers
        )
        
        NavigationDrawerItem(
            label = { Text("Configurações") },
            selected = false,
            onClick = onNavigateToSettings
        )
    }
}

@Composable
fun EmptyGamesState(
    modifier: Modifier = Modifier,
    hasContainers: Boolean
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (hasContainers) "Nenhum jogo encontrado" else "Nenhum container encontrado",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasContainers) 
                "Toque no botão + para adicionar um jogo" 
            else 
                "Crie um container primeiro para adicionar jogos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun GamesGrid(
    shortcuts: List<Shortcut>,
    onGameClick: (Shortcut) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(shortcuts, key = { it.file.absolutePath }) { shortcut ->
            GameCard(
                shortcut = shortcut,
                onClick = { onGameClick(shortcut) }
            )
        }
    }
}

@Composable
fun GameCard(
    shortcut: Shortcut,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (shortcut.coverArt != null || shortcut.icon != null) {
                AsyncImage(
                    model = shortcut.iconFile?.absolutePath,
                    contentDescription = shortcut.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                ),
                                startY = 100f
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = shortcut.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (shortcut.icon != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = shortcut.container.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (shortcut.icon != null) 
                        Color.White.copy(alpha = 0.8f) 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

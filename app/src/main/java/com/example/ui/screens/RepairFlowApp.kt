package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.RepairFlowTheme
import com.example.ui.viewmodel.RepairFlowViewModel
import com.example.ui.viewmodel.ScreenTab
import com.example.util.AppLanguage
import com.example.util.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepairFlowApp(viewModel: RepairFlowViewModel) {
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val stats by viewModel.dashboardStats.collectAsState()
    val clients by viewModel.rawClients.collectAsState()
    val techniciens by viewModel.techniciens.collectAsState()

    var showAddReparationDialog by remember { mutableStateOf(false) }
    var showAddClientDialog by remember { mutableStateOf(false) }
    var showAddPieceDialog by remember { mutableStateOf(false) }

    if (!isAuthenticated) {
        LoginScreen(viewModel = viewModel)
        return
    }

    RepairFlowTheme(darkTheme = isDarkTheme) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.testTag("app_header_title")
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.repairflow_logo),
                                contentDescription = "Logo RepairFlow",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RepairFlow",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            currentUser?.let { u ->
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = u.role.take(5),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        // Language toggle
                        IconButton(
                            onClick = { viewModel.toggleLanguage() },
                            modifier = Modifier.testTag("top_bar_lang_btn")
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = currentLanguage.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Theme toggle
                        IconButton(
                            onClick = { viewModel.toggleDarkTheme() },
                            modifier = Modifier.testTag("top_bar_theme_btn")
                        ) {
                            Icon(
                                if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Bascule Thème"
                            )
                        }

                        // Logout button
                        IconButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier.testTag("top_bar_logout_btn")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout,
                                contentDescription = if (currentLanguage == AppLanguage.FR) "Déconnexion" else "Logout",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    NavigationBarItem(
                        selected = currentTab == ScreenTab.DASHBOARD,
                        onClick = { viewModel.setScreenTab(ScreenTab.DASHBOARD) },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                        label = { Text(AppStrings.get("dashboard", currentLanguage), style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("nav_dashboard")
                    )

                    NavigationBarItem(
                        selected = currentTab == ScreenTab.REPARATIONS,
                        onClick = { viewModel.setScreenTab(ScreenTab.REPARATIONS) },
                        icon = {
                            BadgedBox(badge = {
                                if (stats.reparationsEnCours > 0) {
                                    Badge { Text("${stats.reparationsEnCours}") }
                                }
                            }) {
                                Icon(Icons.Default.Build, contentDescription = null)
                            }
                        },
                        label = { Text(AppStrings.get("repairs", currentLanguage), style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("nav_reparations")
                    )

                    NavigationBarItem(
                        selected = currentTab == ScreenTab.CLIENTS,
                        onClick = { viewModel.setScreenTab(ScreenTab.CLIENTS) },
                        icon = { Icon(Icons.Default.People, contentDescription = null) },
                        label = { Text(AppStrings.get("clients", currentLanguage), style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("nav_clients")
                    )

                    NavigationBarItem(
                        selected = currentTab == ScreenTab.STOCK,
                        onClick = { viewModel.setScreenTab(ScreenTab.STOCK) },
                        icon = {
                            BadgedBox(badge = {
                                if (stats.piecesEnAlerte > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Text("${stats.piecesEnAlerte}")
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Inventory, contentDescription = null)
                            }
                        },
                        label = { Text(AppStrings.get("stock", currentLanguage), style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("nav_stock")
                    )

                    NavigationBarItem(
                        selected = currentTab == ScreenTab.FACTURATION,
                        onClick = { viewModel.setScreenTab(ScreenTab.FACTURATION) },
                        icon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                        label = { Text(AppStrings.get("billing", currentLanguage), style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("nav_facturation")
                    )

                    NavigationBarItem(
                        selected = currentTab == ScreenTab.ADMIN,
                        onClick = { viewModel.setScreenTab(ScreenTab.ADMIN) },
                        icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                        label = { Text(AppStrings.get("admin", currentLanguage), style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("nav_admin")
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentTab) {
                    ScreenTab.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        lang = currentLanguage,
                        onNavigateToRepairs = { viewModel.setScreenTab(ScreenTab.REPARATIONS) },
                        onNavigateToStock = { viewModel.setScreenTab(ScreenTab.STOCK) },
                        onNavigateToClients = { viewModel.setScreenTab(ScreenTab.CLIENTS) },
                        onNavigateToBilling = { viewModel.setScreenTab(ScreenTab.FACTURATION) },
                        onOpenNewRepairDialog = { showAddReparationDialog = true },
                        onOpenNewClientDialog = { showAddClientDialog = true }
                    )
                    ScreenTab.REPARATIONS -> ReparationsScreen(
                        viewModel = viewModel,
                        lang = currentLanguage,
                        onNavigateToBilling = { viewModel.setScreenTab(ScreenTab.FACTURATION) },
                        onOpenNewRepairDialog = { showAddReparationDialog = true }
                    )
                    ScreenTab.CLIENTS -> ClientsScreen(
                        viewModel = viewModel,
                        lang = currentLanguage,
                        onOpenNewClientDialog = { showAddClientDialog = true }
                    )
                    ScreenTab.STOCK -> StockScreen(
                        viewModel = viewModel,
                        lang = currentLanguage,
                        onOpenNewPieceDialog = { showAddPieceDialog = true }
                    )
                    ScreenTab.FACTURATION -> FacturationScreen(
                        viewModel = viewModel,
                        lang = currentLanguage
                    )
                    ScreenTab.ADMIN -> UtilisateursScreen(
                        viewModel = viewModel,
                        lang = currentLanguage
                    )
                }
            }
        }

        // Global Dialogs
        if (showAddReparationDialog) {
            AddReparationDialog(
                viewModel = viewModel,
                clients = clients,
                techniciens = techniciens,
                lang = currentLanguage,
                onDismiss = { showAddReparationDialog = false }
            )
        }

        if (showAddClientDialog) {
            AddClientDialog(
                lang = currentLanguage,
                onDismiss = { showAddClientDialog = false },
                onConfirm = { nom, prenom, tel, email, adr ->
                    viewModel.createClient(nom, prenom, tel, email, adr) {
                        showAddClientDialog = false
                    }
                }
            )
        }

        if (showAddPieceDialog) {
            AddPieceDialog(
                lang = currentLanguage,
                onDismiss = { showAddPieceDialog = false },
                onConfirm = { des, ref, prix, stock, seuil, cat ->
                    viewModel.createPiece(des, ref, prix, stock, seuil, cat)
                    showAddPieceDialog = false
                }
            )
        }
    }
}

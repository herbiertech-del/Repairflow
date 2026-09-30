package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.entity.PieceDetachee
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.RepairFlowViewModel
import com.example.util.AppLanguage
import com.example.util.AppStrings
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(
    viewModel: RepairFlowViewModel,
    lang: AppLanguage,
    onOpenNewPieceDialog: () -> Unit
) {
    val pieces by viewModel.filteredPieces.collectAsState()
    val allPieces by viewModel.rawPieces.collectAsState()
    val searchQuery by viewModel.stockSearchQuery.collectAsState()

    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var onlyAlerts by remember { mutableStateOf(false) }

    var pieceForRestock by remember { mutableStateOf<PieceDetachee?>(null) }

    val displayedPieces = pieces.filter { p ->
        val matchCat = selectedCategoryFilter == null || p.categorie == selectedCategoryFilter
        val matchAlert = !onlyAlerts || p.estEnAlerte
        matchCat && matchAlert
    }

    val alertCount = allPieces.count { it.estEnAlerte }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenNewPieceDialog,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_piece_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = AppStrings.get("new_part", lang))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("stock_screen")
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setStockSearch(it) },
                placeholder = { Text(if (lang == AppLanguage.FR) "Rechercher une pièce, réf, catégorie..." else "Search spare parts...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setStockSearch("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Effacer")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("stock_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Category & Alert filter chips
            val categories = listOf("Écrans", "Batteries", "Connectique", "Stockage & RAM", "Composants & Puces", "Consommables", "Général")

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == null && !onlyAlerts,
                        onClick = {
                            selectedCategoryFilter = null
                            onlyAlerts = false
                        },
                        label = { Text(AppStrings.get("all", lang)) }
                    )
                }
                item {
                    FilterChip(
                        selected = onlyAlerts,
                        onClick = { onlyAlerts = !onlyAlerts },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (alertCount > 0) {
                                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp), tint = StatusError)
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text("${if (lang == AppLanguage.FR) "Alertes Stock" else "Low Stock"} ($alertCount)")
                            }
                        }
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
                        },
                        label = { Text(cat) }
                    )
                }
            }

            if (displayedPieces.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (lang == AppLanguage.FR) "Aucune pièce trouvée" else "No spare parts found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayedPieces, key = { it.idPiece }) { piece ->
                        PieceStockCard(
                            piece = piece,
                            lang = lang,
                            onAdjust = { delta -> viewModel.adjustStock(piece.idPiece, delta) },
                            onOpenRestock = { pieceForRestock = piece }
                        )
                    }
                }
            }
        }
    }

    // Restock Dialog
    pieceForRestock?.let { targetPiece ->
        var addCountText by remember { mutableStateOf("5") }

        AlertDialog(
            onDismissRequest = { pieceForRestock = null },
            title = {
                Text(
                    "${AppStrings.get("restock", lang)} : ${targetPiece.designation}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${if (lang == AppLanguage.FR) "Stock actuel :" else "Current stock:"} ${targetPiece.quantiteStock} ${AppStrings.get("units", lang)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = addCountText,
                        onValueChange = { addCountText = it },
                        label = { Text(if (lang == AppLanguage.FR) "Quantité reçue à ajouter" else "Units to add") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = addCountText.toIntOrNull() ?: 0
                        if (count > 0) {
                            viewModel.adjustStock(targetPiece.idPiece, count)
                            pieceForRestock = null
                        }
                    },
                    enabled = (addCountText.toIntOrNull() ?: 0) > 0
                ) {
                    Text(if (lang == AppLanguage.FR) "Valider entrée de stock" else "Confirm Restock")
                }
            },
            dismissButton = {
                TextButton(onClick = { pieceForRestock = null }) {
                    Text(AppStrings.get("cancel", lang))
                }
            }
        )
    }
}

@Composable
fun PieceStockCard(
    piece: PieceDetachee,
    lang: AppLanguage,
    onAdjust: (Int) -> Unit,
    onOpenRestock: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("piece_card_${piece.idPiece}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = piece.designation,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (piece.reference.isNotBlank()) {
                        Text(
                            text = "Réf: ${piece.reference} • ${piece.categorie}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Alert badge or OK badge
                if (piece.estEnAlerte) {
                    Surface(
                        color = StatusError.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusError, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (lang == AppLanguage.FR) "Stock Critique" else "Low Stock",
                                color = StatusError,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                } else {
                    Surface(
                        color = StatusSuccess.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (lang == AppLanguage.FR) "En Stock" else "In Stock",
                            color = StatusSuccess,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            // Body: Unit price & Stock management buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = String.format(Locale.FRANCE, "%.2f € HT", piece.prixUnitaire),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Seuil d'alerte : ${piece.seuilAlerte} pcs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Stock Adjustment Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onAdjust(-1) },
                        enabled = piece.quantiteStock > 0,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Retirer une pièce")
                    }

                    Surface(
                        color = if (piece.estEnAlerte) StatusError.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "${piece.quantiteStock}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (piece.estEnAlerte) StatusError else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = { onAdjust(1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Ajouter une pièce")
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    FilledTonalButton(
                        onClick = onOpenRestock,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (lang == AppLanguage.FR) "Appro" else "Restock",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

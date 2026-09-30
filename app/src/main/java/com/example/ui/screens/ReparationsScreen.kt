package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.StatutReparation
import com.example.data.model.ReparationComplet
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusNeutral
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.RepairFlowViewModel
import com.example.util.AppLanguage
import com.example.util.AppStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReparationsScreen(
    viewModel: RepairFlowViewModel,
    lang: AppLanguage,
    onNavigateToBilling: () -> Unit,
    onOpenNewRepairDialog: () -> Unit
) {
    val reparations by viewModel.filteredReparations.collectAsState()
    val rawReparations by viewModel.rawReparations.collectAsState()
    val filterStatus by viewModel.reparationFilterStatus.collectAsState()
    val searchQuery by viewModel.reparationSearchQuery.collectAsState()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)

    var selectedReparationForDetail by remember { mutableStateOf<ReparationComplet?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenNewRepairDialog,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_reparation_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = AppStrings.get("new_repair", lang))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("reparations_screen")
        ) {
            // Header Bar with Quick Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (lang == AppLanguage.FR) "Atelier de Réparations" else "Repair Workshop",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                FilledTonalButton(
                    onClick = onOpenNewRepairDialog,
                    modifier = Modifier.testTag("top_add_reparation_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (lang == AppLanguage.FR) "Nouvelle Fiche ☁️" else "New Ticket ☁️")
                }
            }

            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setReparationSearch(it) },
                placeholder = { Text(AppStrings.get("search_placeholder", lang)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setReparationSearch("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Effacer")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("reparation_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Status Filter Chips
            val statusList = listOf(
                null to AppStrings.get("all", lang),
                StatutReparation.RECUE.name to AppStrings.get("status_received", lang),
                StatutReparation.EN_DIAGNOSTIC.name to AppStrings.get("status_diagnosis", lang),
                StatutReparation.EN_COURS.name to AppStrings.get("status_in_progress", lang),
                StatutReparation.TERMINEE.name to AppStrings.get("status_completed", lang),
                StatutReparation.RESTITUEE.name to AppStrings.get("status_returned", lang)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(statusList) { (statusCode, label) ->
                    val isSelected = filterStatus == statusCode
                    val count = if (statusCode == null) rawReparations.size else rawReparations.count { it.reparation.statut == statusCode }

                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setReparationFilter(statusCode) },
                        label = { Text("$label ($count)") }
                    )
                }
            }

            // Reparations List
            if (reparations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (lang == AppLanguage.FR) "Aucune réparation trouvée" else "No repairs found",
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
                    items(reparations, key = { it.reparation.idReparation }) { item ->
                        ReparationCard(
                            item = item,
                            lang = lang,
                            onClick = { selectedReparationForDetail = item },
                            onAdvanceStatus = { nextStatus ->
                                viewModel.updateReparationStatus(item.reparation, nextStatus)
                            }
                        )
                    }
                }
            }
        }
    }

    // Detail Modal Dialog
    selectedReparationForDetail?.let { currentDetail ->
        // Get fresh state from list if updated
        val freshItem = reparations.find { it.reparation.idReparation == currentDetail.reparation.idReparation } ?: currentDetail
        ReparationDetailDialog(
            item = freshItem,
            viewModel = viewModel,
            lang = lang,
            onDismiss = { selectedReparationForDetail = null },
            onNavigateToBilling = onNavigateToBilling
        )
    }
}

@Composable
fun ReparationCard(
    item: ReparationComplet,
    lang: AppLanguage,
    onClick: () -> Unit,
    onAdvanceStatus: (StatutReparation) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)

    val (badgeBg, badgeText, nextAction) = when (item.reparation.statut) {
        StatutReparation.RECUE.name -> Triple(
            StatusInfo,
            AppStrings.get("status_received", lang),
            StatutReparation.EN_DIAGNOSTIC
        )
        StatutReparation.EN_DIAGNOSTIC.name -> Triple(
            StatusWarning,
            AppStrings.get("status_diagnosis", lang),
            StatutReparation.EN_COURS
        )
        StatutReparation.EN_COURS.name -> Triple(
            MaterialTheme.colorScheme.primary,
            AppStrings.get("status_in_progress", lang),
            StatutReparation.TERMINEE
        )
        StatutReparation.TERMINEE.name -> Triple(
            StatusSuccess,
            AppStrings.get("status_completed", lang),
            StatutReparation.RESTITUEE
        )
        StatutReparation.RESTITUEE.name -> Triple(
            StatusNeutral,
            AppStrings.get("status_returned", lang),
            null
        )
        else -> Triple(StatusNeutral, item.reparation.statut, null)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("reparation_card_${item.reparation.idReparation}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Ticket # and Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${item.reparation.idReparation}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dateFormat.format(Date(item.reparation.dateDepot)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = badgeBg.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeBg,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Device & Client
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Devices,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.appareil.designation,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Text(
                text = "SN: ${item.appareil.numeroSerie}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 22.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${item.client.nomComplet} (${item.client.telephone})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Panne déclarée
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Panne: ${item.reparation.panneDeclaree}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp),
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            // Bottom bar: Price estimation + next action button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (lang == AppLanguage.FR) "Coût estimé TTC" else "Est. Total TTC",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.FRANCE, "%.2f €", item.totalTTC),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (nextAction != null) {
                    val actionLabel = when (nextAction) {
                        StatutReparation.EN_DIAGNOSTIC -> if (lang == AppLanguage.FR) "Passer En Diagnostic" else "Start Diagnosis"
                        StatutReparation.EN_COURS -> if (lang == AppLanguage.FR) "Lancer Réparation" else "Start Repair"
                        StatutReparation.TERMINEE -> if (lang == AppLanguage.FR) "Marquer Terminée" else "Mark Done"
                        StatutReparation.RESTITUEE -> if (lang == AppLanguage.FR) "Restituer Client" else "Return to Client"
                        else -> nextAction.code
                    }

                    OutlinedButton(
                        onClick = { onAdvanceStatus(nextAction) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(actionLabel, style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                } else {
                    Surface(
                        color = StatusSuccess.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (lang == AppLanguage.FR) "Dossier Clôturé" else "Closed Ticket",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusSuccess
                            )
                        }
                    }
                }
            }
        }
    }
}

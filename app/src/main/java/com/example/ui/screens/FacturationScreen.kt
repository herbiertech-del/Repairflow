package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.StatutPaiement
import com.example.data.model.FactureComplet
import com.example.data.model.ReparationComplet
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.RepairFlowViewModel
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.PdfInvoiceGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacturationScreen(
    viewModel: RepairFlowViewModel,
    lang: AppLanguage
) {
    val context = LocalContext.current
    val factures by viewModel.filteredFactures.collectAsState()
    val allFactures by viewModel.rawFactures.collectAsState()
    val allReparations by viewModel.rawReparations.collectAsState()
    val selectedFilter by viewModel.factureFilterStatus.collectAsState()

    var factureForPaiement by remember { mutableStateOf<FactureComplet?>(null) }
    var showCreateInvoiceForRepDialog by remember { mutableStateOf(false) }

    val unbilledReparations = allReparations.filter { it.facture == null }

    val totalFactureTTC = allFactures.sumOf { it.facture.montantTTC }
    val totalPayeTTC = allFactures.sumOf { it.totalPaye }
    val totalRestantTTC = (totalFactureTTC - totalPayeTTC).coerceAtLeast(0.0)

    Scaffold(
        floatingActionButton = {
            if (unbilledReparations.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showCreateInvoiceForRepDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("new_invoice_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = AppStrings.get("generate_invoice", lang))
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("facturation_screen")
        ) {
            // Financial Summary Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (lang == AppLanguage.FR) "Total Émis TTC" else "Total Invoiced",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.FRANCE, "%.2f €", totalFactureTTC),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (lang == AppLanguage.FR) "Encaissé" else "Collected",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.FRANCE, "%.2f €", totalPayeTTC),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StatusSuccess
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (lang == AppLanguage.FR) "Restant dû" else "Pending Balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.FRANCE, "%.2f €", totalRestantTTC),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (totalRestantTTC > 0) StatusWarning else StatusSuccess
                        )
                    }
                }
            }

            // Payment Status Filters
            val filters = listOf(
                null to AppStrings.get("all", lang),
                StatutPaiement.IMPAYEE.name to if (lang == AppLanguage.FR) "Impayées" else "Unpaid",
                StatutPaiement.PARTIEL.name to if (lang == AppLanguage.FR) "Partielles" else "Partial",
                StatutPaiement.PAYEE.name to if (lang == AppLanguage.FR) "Payées" else "Paid"
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(filters) { (status, label) ->
                    val isSelected = selectedFilter == status
                    val count = if (status == null) allFactures.size else allFactures.count { it.facture.statutPaiement == status }
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFactureFilter(status) },
                        label = { Text("$label ($count)") }
                    )
                }
            }

            // Invoices List
            if (factures.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (lang == AppLanguage.FR) "Aucune facture enregistrée" else "No invoices recorded",
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
                    items(factures, key = { it.facture.idFacture }) { facComplet ->
                        FactureItemCard(
                            factureComplet = facComplet,
                            lang = lang,
                            onRecordPayment = { factureForPaiement = facComplet },
                            onSharePdf = {
                                val pdf = PdfInvoiceGenerator.generateInvoicePdf(context, facComplet)
                                PdfInvoiceGenerator.sharePdf(context, pdf)
                            },
                            onViewPdf = {
                                val pdf = PdfInvoiceGenerator.generateInvoicePdf(context, facComplet)
                                PdfInvoiceGenerator.viewPdf(context, pdf)
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal to record payment
    factureForPaiement?.let { targetFac ->
        RecordPaiementDialog(
            factureId = targetFac.facture.idFacture,
            montantRestant = targetFac.resteAPayer,
            lang = lang,
            onDismiss = { factureForPaiement = null },
            onConfirm = { montant, mode ->
                viewModel.recordPaiement(targetFac.facture.idFacture, montant, mode) {
                    Toast.makeText(context, if (lang == AppLanguage.FR) "Paiement enregistré" else "Payment recorded", Toast.LENGTH_SHORT).show()
                    factureForPaiement = null
                }
            }
        )
    }

    // Modal to generate invoice from unbilled repair
    if (showCreateInvoiceForRepDialog) {
        var selectedRep by remember { mutableStateOf(unbilledReparations.firstOrNull()) }
        var repMenuExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCreateInvoiceForRepDialog = false },
            title = { Text(AppStrings.get("generate_invoice", lang), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (lang == AppLanguage.FR) "Choisir une réparation à facturer :" else "Select a repair to invoice:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    ExposedDropdownMenuBox(
                        expanded = repMenuExpanded,
                        onExpandedChange = { repMenuExpanded = !repMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedRep?.let { "#${it.reparation.idReparation} - ${it.appareil.designation} (${it.client.nomComplet})" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Réparation") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = repMenuExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = repMenuExpanded,
                            onDismissRequest = { repMenuExpanded = false }
                        ) {
                            unbilledReparations.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text("#${r.reparation.idReparation} : ${r.appareil.designation} (${r.client.nomComplet})") },
                                    onClick = {
                                        selectedRep = r
                                        repMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val r = selectedRep
                        if (r != null) {
                            viewModel.generateFacture(r.reparation.idReparation, 20.0) {
                                Toast.makeText(context, if (lang == AppLanguage.FR) "Facture créée avec succès" else "Invoice created", Toast.LENGTH_SHORT).show()
                                showCreateInvoiceForRepDialog = false
                            }
                        }
                    },
                    enabled = selectedRep != null
                ) {
                    Text(AppStrings.get("save", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateInvoiceForRepDialog = false }) {
                    Text(AppStrings.get("cancel", lang))
                }
            }
        )
    }
}

@Composable
fun FactureItemCard(
    factureComplet: FactureComplet,
    lang: AppLanguage,
    onRecordPayment: () -> Unit,
    onSharePdf: () -> Unit,
    onViewPdf: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)

    val (badgeBg, badgeText) = when (factureComplet.facture.statutPaiement) {
        StatutPaiement.PAYEE.name -> StatusSuccess to if (lang == AppLanguage.FR) "Payée" else "Paid"
        StatutPaiement.PARTIEL.name -> StatusWarning to if (lang == AppLanguage.FR) "Partielle" else "Partial"
        else -> StatusError to if (lang == AppLanguage.FR) "Impayée" else "Unpaid"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("facture_card_${factureComplet.facture.idFacture}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Facture number & Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = factureComplet.facture.numero,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
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

            Text(
                text = "Émise le ${dateFormat.format(Date(factureComplet.facture.dateFacture))} pour Rep #${factureComplet.reparation.idReparation}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Client and device
            Text(
                text = "${factureComplet.client.nomComplet} • ${factureComplet.appareil.designation}",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Pricing summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Montant HT: ${String.format(Locale.FRANCE, "%.2f €", factureComplet.facture.montantHT)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "TTC: ${String.format(Locale.FRANCE, "%.2f €", factureComplet.facture.montantTTC)}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Encaissé: ${String.format(Locale.FRANCE, "%.2f €", factureComplet.totalPaye)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusSuccess
                )
                Text(
                    text = "Reste à payer: ${String.format(Locale.FRANCE, "%.2f €", factureComplet.resteAPayer)}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (factureComplet.estSoldee) StatusSuccess else StatusError
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onViewPdf,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = onSharePdf) {
                        Icon(Icons.Default.Share, contentDescription = "Partager PDF", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                if (!factureComplet.estSoldee) {
                    FilledTonalButton(
                        onClick = onRecordPayment,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (lang == AppLanguage.FR) "Encaisser" else "Collect", style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (lang == AppLanguage.FR) "Facture Soldeé" else "Settled",
                            color = StatusSuccess,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

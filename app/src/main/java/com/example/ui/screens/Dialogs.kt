package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.Appareil
import com.example.data.entity.Client
import com.example.data.entity.ModePaiement
import com.example.data.entity.PieceDetachee
import com.example.data.entity.StatutReparation
import com.example.data.entity.Technicien
import com.example.data.model.ClientComplet
import com.example.data.model.ReparationComplet
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
fun AddReparationDialog(
    viewModel: RepairFlowViewModel,
    clients: List<ClientComplet>,
    techniciens: List<Technicien>,
    lang: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Saisie Directe & Firestore, 1 = Client Existant

    // --- State for Saisie Directe (User Prompt Request) ---
    var clientNom by remember { mutableStateOf("") }
    var clientTelephone by remember { mutableStateOf("") }
    var appareilModele by remember { mutableStateOf("") }
    var appareilMarque by remember { mutableStateOf("") }
    var appareilType by remember { mutableStateOf("Smartphone") }
    var panneDirecte by remember { mutableStateOf("") }
    var prioriteDirecte by remember { mutableStateOf("Normale") }
    var coutMODirecte by remember { mutableStateOf("35.0") }
    var saveToFirestoreDirecte by remember { mutableStateOf(true) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var prioriteMenuExpandedDirecte by remember { mutableStateOf(false) }

    // --- State for Existing Client Selection ---
    var selectedClient by remember { mutableStateOf(clients.firstOrNull()) }
    var selectedAppareil by remember { mutableStateOf(selectedClient?.appareils?.firstOrNull()) }
    var selectedTech by remember { mutableStateOf(techniciens.firstOrNull()) }
    var panne by remember { mutableStateOf("") }
    var priorite by remember { mutableStateOf("Normale") }
    var coutMO by remember { mutableStateOf("35.0") }
    var saveExistingToFirestore by remember { mutableStateOf(true) }

    var clientMenuExpanded by remember { mutableStateOf(false) }
    var appareilMenuExpanded by remember { mutableStateOf(false) }
    var techMenuExpanded by remember { mutableStateOf(false) }
    var prioriteMenuExpanded by remember { mutableStateOf(false) }

    val priorities = listOf("Basse", "Normale", "Haute", "Urgente")
    val deviceTypes = listOf("Smartphone", "PC Portable", "Tablette", "Console", "Smartwatch", "Autre")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (lang == AppLanguage.FR) "Nouvelle Fiche Réparation" else "New Repair Ticket",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                if (lang == AppLanguage.FR) "Saisie Rapide ☁️" else "Quick & Cloud ☁️",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                if (lang == AppLanguage.FR) "Client existant" else "Existing Client",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedTab == 0) {
                    // --- TAB 0 : SAISIE DIRECTE & FIRESTORE ---

                    // 1. Nom du client
                    OutlinedTextField(
                        value = clientNom,
                        onValueChange = { clientNom = it },
                        label = { Text((if (lang == AppLanguage.FR) "Nom du client" else "Client name") + " *") },
                        placeholder = { Text(if (lang == AppLanguage.FR) "Ex: Thomas Martin" else "e.g., John Smith") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_nom_client")
                    )

                    // 2. Téléphone du client
                    OutlinedTextField(
                        value = clientTelephone,
                        onValueChange = { clientTelephone = it },
                        label = { Text(if (lang == AppLanguage.FR) "Téléphone client" else "Client phone") },
                        placeholder = { Text("06 12 34 56 78") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_telephone_client")
                    )

                    // 3. Modèle de l'appareil
                    OutlinedTextField(
                        value = appareilModele,
                        onValueChange = { appareilModele = it },
                        label = { Text((if (lang == AppLanguage.FR) "Modèle de l'appareil" else "Device model") + " *") },
                        placeholder = { Text(if (lang == AppLanguage.FR) "Ex: iPhone 14 Pro, Galaxy S23..." else "e.g., iPhone 14 Pro...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_modele_appareil")
                    )

                    // 4. Type & Marque de l'appareil
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = typeMenuExpanded,
                            onExpandedChange = { typeMenuExpanded = !typeMenuExpanded },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            OutlinedTextField(
                                value = appareilType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(if (lang == AppLanguage.FR) "Type" else "Type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = typeMenuExpanded,
                                onDismissRequest = { typeMenuExpanded = false }
                            ) {
                                deviceTypes.forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t) },
                                        onClick = {
                                            appareilType = t
                                            typeMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = appareilMarque,
                            onValueChange = { appareilMarque = it },
                            label = { Text(if (lang == AppLanguage.FR) "Marque (opt.)" else "Brand") },
                            placeholder = { Text("Apple") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 5. Description de la panne
                    OutlinedTextField(
                        value = panneDirecte,
                        onValueChange = { panneDirecte = it },
                        label = { Text((if (lang == AppLanguage.FR) "Description de la panne" else "Fault description") + " *") },
                        placeholder = { Text(if (lang == AppLanguage.FR) "Ex: Vitre brisée, problème de charge, batterie gonflée..." else "e.g., Cracked screen, battery draining...") },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_description_panne")
                    )

                    // 6. Priorité & MO
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = prioriteMenuExpandedDirecte,
                            onExpandedChange = { prioriteMenuExpandedDirecte = !prioriteMenuExpandedDirecte },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = prioriteDirecte,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(if (lang == AppLanguage.FR) "Priorité" else "Priority") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = prioriteMenuExpandedDirecte) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = prioriteMenuExpandedDirecte,
                                onDismissRequest = { prioriteMenuExpandedDirecte = false }
                            ) {
                                priorities.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p) },
                                        onClick = {
                                            prioriteDirecte = p
                                            prioriteMenuExpandedDirecte = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = coutMODirecte,
                            onValueChange = { coutMODirecte = it },
                            label = { Text(if (lang == AppLanguage.FR) "Forfait MO (€)" else "Labor (€)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 7. Toggle Sauvegarder dans Firestore
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (lang == AppLanguage.FR) "Sauvegarde Firestore Cloud" else "Cloud Firestore Sync",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = if (lang == AppLanguage.FR) "Synchronisation temps réel" else "Real-time remote persistence",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = saveToFirestoreDirecte,
                                onCheckedChange = { saveToFirestoreDirecte = it }
                            )
                        }
                    }
                } else {
                    // --- TAB 1 : CLIENT EXISTANT ---
                    // Client selector
                    ExposedDropdownMenuBox(
                        expanded = clientMenuExpanded,
                        onExpandedChange = { clientMenuExpanded = !clientMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedClient?.client?.nomComplet ?: if (lang == AppLanguage.FR) "Choisir un client" else "Select client",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (lang == AppLanguage.FR) "Client *" else "Client *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientMenuExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("select_client_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = clientMenuExpanded,
                            onDismissRequest = { clientMenuExpanded = false }
                        ) {
                            clients.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text("${c.client.nomComplet} (${c.appareils.size} app.)") },
                                    onClick = {
                                        selectedClient = c
                                        selectedAppareil = c.appareils.firstOrNull()
                                        clientMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Appareil selector
                    val clientAppareils = selectedClient?.appareils ?: emptyList()
                    ExposedDropdownMenuBox(
                        expanded = appareilMenuExpanded,
                        onExpandedChange = { if (clientAppareils.isNotEmpty()) appareilMenuExpanded = !appareilMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedAppareil?.designation ?: if (clientAppareils.isEmpty()) (if (lang == AppLanguage.FR) "Aucun appareil enregistré" else "No devices registered") else (if (lang == AppLanguage.FR) "Choisir un appareil" else "Select device"),
                            onValueChange = {},
                            readOnly = true,
                            enabled = clientAppareils.isNotEmpty(),
                            label = { Text(if (lang == AppLanguage.FR) "Appareil *" else "Device *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = appareilMenuExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("select_device_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = appareilMenuExpanded,
                            onDismissRequest = { appareilMenuExpanded = false }
                        ) {
                            clientAppareils.forEach { app ->
                                DropdownMenuItem(
                                    text = { Text("${app.designation} - ${app.numeroSerie}") },
                                    onClick = {
                                        selectedAppareil = app
                                        appareilMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Panne déclarée
                    OutlinedTextField(
                        value = panne,
                        onValueChange = { panne = it },
                        label = { Text(AppStrings.get("declared_issue", lang) + " *") },
                        placeholder = { Text(if (lang == AppLanguage.FR) "Ex: Écran noir, ne charge plus..." else "e.g., Black screen, no charge...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("declared_issue_input"),
                        minLines = 2
                    )

                    // Technicien selector
                    ExposedDropdownMenuBox(
                        expanded = techMenuExpanded,
                        onExpandedChange = { techMenuExpanded = !techMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedTech?.nomComplet ?: if (lang == AppLanguage.FR) "Non assigné" else "Unassigned",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(AppStrings.get("assigned_tech", lang)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = techMenuExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = techMenuExpanded,
                            onDismissRequest = { techMenuExpanded = false }
                        ) {
                            techniciens.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text("${t.nomComplet} (${t.specialite})") },
                                    onClick = {
                                        selectedTech = t
                                        techMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Priorité
                        ExposedDropdownMenuBox(
                            expanded = prioriteMenuExpanded,
                            onExpandedChange = { prioriteMenuExpanded = !prioriteMenuExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = priorite,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(if (lang == AppLanguage.FR) "Priorité" else "Priority") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = prioriteMenuExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = prioriteMenuExpanded,
                                onDismissRequest = { prioriteMenuExpanded = false }
                            ) {
                                priorities.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p) },
                                        onClick = {
                                            priorite = p
                                            prioriteMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Cout forfait main d'oeuvre initial
                        OutlinedTextField(
                            value = coutMO,
                            onValueChange = { coutMO = it },
                            label = { Text(if (lang == AppLanguage.FR) "Forfait MO (€)" else "Labor (€)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Firestore toggle for existing
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (lang == AppLanguage.FR) "Synchroniser sur Firestore ☁️" else "Sync to Firestore ☁️",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = saveExistingToFirestore,
                            onCheckedChange = { saveExistingToFirestore = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedTab == 0) {
                        // Saisie directe & Firestore
                        val moVal = coutMODirecte.toDoubleOrNull() ?: 35.0
                        viewModel.addNewFicheReparation(
                            clientNom = clientNom,
                            clientTelephone = clientTelephone,
                            appareilModele = appareilModele,
                            appareilMarque = appareilMarque,
                            appareilType = appareilType,
                            panne = panneDirecte,
                            priorite = prioriteDirecte,
                            coutMainOeuvre = moVal,
                            saveToFirestore = saveToFirestoreDirecte
                        ) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            if (success) {
                                onDismiss()
                            }
                        }
                    } else {
                        // Client existant
                        val app = selectedAppareil
                        if (app != null && panne.isNotBlank()) {
                            val moVal = coutMO.toDoubleOrNull() ?: 0.0
                            viewModel.createReparation(
                                panne = panne,
                                appareilId = app.idAppareil,
                                technicienId = selectedTech?.idTechnicien,
                                priorite = priorite,
                                coutMainOeuvre = moVal,
                                onDone = { repId ->
                                    if (saveExistingToFirestore) {
                                        val c = selectedClient?.client
                                        val fsRep = com.example.data.firestore.FirestoreReparation(
                                            id = "REP_DOSSIER_${repId}",
                                            idLocal = repId,
                                            numeroDossier = "REP-${repId.toString().padStart(4, '0')}",
                                            clientNom = c?.nomComplet ?: "Client",
                                            clientTelephone = c?.telephone ?: "",
                                            clientEmail = c?.email ?: "",
                                            appareilMarque = app.marque,
                                            appareilModele = app.modele,
                                            appareilType = app.type,
                                            panneDeclaree = panne,
                                            statut = "RECUE",
                                            priorite = priorite,
                                            coutMainOeuvre = moVal,
                                            montantTotalHT = moVal,
                                            montantTotalTTC = moVal * 1.20
                                        )
                                        coroutineScope.launch {
                                            viewModel.firestoreReparationService.saveReparation(fsRep)
                                        }
                                    }
                                    Toast.makeText(
                                        context,
                                        "${if (lang == AppLanguage.FR) "Dossier créé #" else "Ticket created #"}$repId",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onDismiss()
                                }
                            )
                        }
                    }
                },
                enabled = if (selectedTab == 0) {
                    clientNom.isNotBlank() && appareilModele.isNotBlank() && panneDirecte.isNotBlank()
                } else {
                    selectedAppareil != null && panne.isNotBlank()
                },
                modifier = Modifier.testTag("submit_reparation_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (selectedTab == 0 && saveToFirestoreDirecte) {
                        if (lang == AppLanguage.FR) "Enregistrer & Sauvegarder dans Firestore" else "Save to Firestore"
                    } else {
                        AppStrings.get("save", lang)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", lang))
            }
        }
    )
}

@Composable
fun AddClientDialog(
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (nom: String, prenom: String, tel: String, email: String, adresse: String) -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var prenom by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var adresse by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.get("new_client", lang), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text(if (lang == AppLanguage.FR) "Nom *" else "Last Name *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("client_nom_input")
                )
                OutlinedTextField(
                    value = prenom,
                    onValueChange = { prenom = it },
                    label = { Text(if (lang == AppLanguage.FR) "Prénom *" else "First Name *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("client_prenom_input")
                )
                OutlinedTextField(
                    value = telephone,
                    onValueChange = { telephone = it },
                    label = { Text(if (lang == AppLanguage.FR) "Téléphone *" else "Phone *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("client_phone_input")
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = adresse,
                    onValueChange = { adresse = it },
                    label = { Text(if (lang == AppLanguage.FR) "Adresse" else "Address") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nom.isNotBlank() && prenom.isNotBlank()) {
                        onConfirm(nom.trim(), prenom.trim(), telephone.trim(), email.trim(), adresse.trim())
                    }
                },
                enabled = nom.isNotBlank() && prenom.isNotBlank(),
                modifier = Modifier.testTag("save_client_button")
            ) {
                Text(AppStrings.get("save", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", lang))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppareilDialog(
    clientId: Long,
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (type: String, marque: String, modele: String, sn: String) -> Unit
) {
    var type by remember { mutableStateOf("Smartphone") }
    var marque by remember { mutableStateOf("") }
    var modele by remember { mutableStateOf("") }
    var numeroSerie by remember { mutableStateOf("") }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    val types = listOf("Smartphone", "PC Portable", "Tablette", "Console de jeux", "Écran / TV", "Audio & Électronique", "Autre")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.get("new_device", lang), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = typeMenuExpanded,
                    onExpandedChange = { typeMenuExpanded = !typeMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (lang == AppLanguage.FR) "Type d'appareil *" else "Device Type *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false }
                    ) {
                        types.forEach { t ->
                            DropdownMenuItem(text = { Text(t) }, onClick = { type = t; typeMenuExpanded = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = marque,
                    onValueChange = { marque = it },
                    label = { Text(if (lang == AppLanguage.FR) "Marque (ex: Apple, Samsung, Dell) *" else "Brand *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_brand_input")
                )
                OutlinedTextField(
                    value = modele,
                    onValueChange = { modele = it },
                    label = { Text(if (lang == AppLanguage.FR) "Modèle (ex: iPhone 13, XPS 15) *" else "Model *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_model_input")
                )
                OutlinedTextField(
                    value = numeroSerie,
                    onValueChange = { numeroSerie = it },
                    label = { Text(if (lang == AppLanguage.FR) "N° de Série ou IMEI" else "Serial or IMEI") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (marque.isNotBlank() && modele.isNotBlank()) {
                        onConfirm(type, marque.trim(), modele.trim(), numeroSerie.trim())
                    }
                },
                enabled = marque.isNotBlank() && modele.isNotBlank(),
                modifier = Modifier.testTag("save_device_button")
            ) {
                Text(AppStrings.get("save", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", lang))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPieceDialog(
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (designation: String, ref: String, prix: Double, stock: Int, seuil: Int, categorie: String) -> Unit
) {
    var designation by remember { mutableStateOf("") }
    var ref by remember { mutableStateOf("") }
    var prix by remember { mutableStateOf("25.0") }
    var stock by remember { mutableStateOf("5") }
    var seuil by remember { mutableStateOf("3") }
    var categorie by remember { mutableStateOf("Écrans") }
    var catMenuExpanded by remember { mutableStateOf(false) }

    val categories = listOf("Écrans", "Batteries", "Connectique", "Stockage & RAM", "Composants & Puces", "Consommables", "Général")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.get("new_part", lang), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text(if (lang == AppLanguage.FR) "Désignation de la pièce *" else "Part Name *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("part_name_input")
                )
                OutlinedTextField(
                    value = ref,
                    onValueChange = { ref = it },
                    label = { Text(if (lang == AppLanguage.FR) "Référence / SKU" else "SKU / Reference") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = catMenuExpanded,
                    onExpandedChange = { catMenuExpanded = !catMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = categorie,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (lang == AppLanguage.FR) "Catégorie" else "Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = catMenuExpanded,
                        onDismissRequest = { catMenuExpanded = false }
                    ) {
                        categories.forEach { c ->
                            DropdownMenuItem(text = { Text(c) }, onClick = { categorie = c; catMenuExpanded = false })
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = prix,
                        onValueChange = { prix = it },
                        label = { Text(if (lang == AppLanguage.FR) "Prix HT (€)" else "Unit Price (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it },
                        label = { Text(if (lang == AppLanguage.FR) "Stock initial" else "Initial Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = seuil,
                    onValueChange = { seuil = it },
                    label = { Text(if (lang == AppLanguage.FR) "Seuil d'alerte stock bas" else "Alert Threshold") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = prix.toDoubleOrNull() ?: 0.0
                    val s = stock.toIntOrNull() ?: 0
                    val th = seuil.toIntOrNull() ?: 3
                    if (designation.isNotBlank()) {
                        onConfirm(designation.trim(), ref.trim(), p, s, th, categorie)
                    }
                },
                enabled = designation.isNotBlank(),
                modifier = Modifier.testTag("save_part_button")
            ) {
                Text(AppStrings.get("save", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", lang))
            }
        }
    )
}

@Composable
fun RecordPaiementDialog(
    factureId: Long,
    montantRestant: Double,
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (montant: Double, mode: String) -> Unit
) {
    var montantText by remember { mutableStateOf(String.format(Locale.US, "%.2f", montantRestant)) }
    var selectedMode by remember { mutableStateOf(ModePaiement.CARTE.libelle) }
    val modes = listOf(
        ModePaiement.CARTE.libelle,
        ModePaiement.ESPECES.libelle,
        ModePaiement.VIREMENT.libelle,
        ModePaiement.CHEQUE.libelle
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                AppStrings.get("pay_invoice", lang),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${if (lang == AppLanguage.FR) "Solde restant dû" else "Remaining Balance"}: ${String.format(Locale.FRANCE, "%.2f €", montantRestant)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = montantText,
                    onValueChange = { montantText = it },
                    label = { Text(if (lang == AppLanguage.FR) "Montant à encaisser (€) *" else "Amount to collect (€) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input")
                )

                Text(
                    if (lang == AppLanguage.FR) "Mode de règlement :" else "Payment method:",
                    style = MaterialTheme.typography.labelLarge
                )

                modes.forEach { mode ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedMode == mode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMode = mode }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(mode, fontWeight = if (selectedMode == mode) FontWeight.Bold else FontWeight.Normal)
                            if (selectedMode == mode) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val m = montantText.toDoubleOrNull() ?: 0.0
                    if (m > 0) {
                        onConfirm(m, selectedMode)
                    }
                },
                enabled = (montantText.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.testTag("confirm_payment_button")
            ) {
                Text(if (lang == AppLanguage.FR) "Valider le paiement" else "Confirm Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", lang))
            }
        }
    )
}

// Complete detail & workflow dialog for a reparation ticket
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReparationDetailDialog(
    item: ReparationComplet,
    viewModel: RepairFlowViewModel,
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onNavigateToBilling: () -> Unit
) {
    val context = LocalContext.current
    var diagnosticText by remember { mutableStateOf(item.reparation.diagnostic) }
    var selectedTechId by remember { mutableStateOf(item.reparation.idTechnicien) }
    var coutMOText by remember { mutableStateOf(item.reparation.coutMainOeuvre.toString()) }

    var showAddIntervention by remember { mutableStateOf(false) }
    var showAddPiece by remember { mutableStateOf(false) }
    var showGenerateFactureConfirm by remember { mutableStateOf(false) }

    val techniciens by viewModel.techniciens.collectAsState()
    val availablePieces by viewModel.rawPieces.collectAsState()
    val interventions by viewModel.repository.getInterventionsForReparation(item.reparation.idReparation).collectAsState(initial = emptyList())
    val piecesUtilisees by viewModel.repository.getPiecesUtiliseesForReparation(item.reparation.idReparation).collectAsState(initial = emptyList())

    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(720.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Fiche Réparation #${item.reparation.idReparation}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${item.appareil.designation} — ${item.client.nomComplet}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Workflow status progress selector
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                if (lang == AppLanguage.FR) "Statut actuel de l'intervention :" else "Current Status:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                StatutReparation.values().forEach { st ->
                                    val isSelected = item.reparation.statut == st.name
                                    val btnColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(btnColor, RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.updateReparationStatus(item.reparation, st)
                                            }
                                            .padding(vertical = 8.dp, horizontal = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            st.code,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = contentColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Client & Device Details Card
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                if (lang == AppLanguage.FR) "Informations Client & Appareil" else "Customer & Device Info",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text("Client: ${item.client.nomComplet} | Tél: ${item.client.telephone}")
                            Text("Email: ${item.client.email}")
                            Text("Appareil: ${item.appareil.designation} (SN: ${item.appareil.numeroSerie})")
                            Text("Dépôt: ${dateFormat.format(Date(item.reparation.dateDepot))}")
                        }
                    }

                    // Panne déclarée & Diagnostic technique
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                AppStrings.get("declared_issue", lang),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                item.reparation.panneDeclaree,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                AppStrings.get("tech_diagnostic", lang),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            OutlinedTextField(
                                value = diagnosticText,
                                onValueChange = { diagnosticText = it },
                                placeholder = { Text(if (lang == AppLanguage.FR) "Saisir les observations et le diagnostic..." else "Enter technician observations...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = coutMOText,
                                    onValueChange = { coutMOText = it },
                                    label = { Text(if (lang == AppLanguage.FR) "Forfait MO (€)" else "Labor Fee (€)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        val mo = coutMOText.toDoubleOrNull() ?: 0.0
                                        viewModel.updateReparationDiagnostic(
                                            item.reparation,
                                            diagnosticText,
                                            selectedTechId,
                                            mo
                                        )
                                        Toast.makeText(context, if (lang == AppLanguage.FR) "Diagnostic sauvegardé" else "Diagnosis saved", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text(AppStrings.get("save", lang))
                                }
                            }
                        }
                    }

                    // Interventions Techniques Section
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (lang == AppLanguage.FR) "Interventions & Main d'Œuvre" else "Labor & Interventions",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                FilledTonalButton(onClick = { showAddIntervention = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (lang == AppLanguage.FR) "Ajouter" else "Add")
                                }
                            }

                            if (interventions.isEmpty()) {
                                Text(
                                    if (lang == AppLanguage.FR) "Aucune intervention spécifique enregistrée" else "No specific interventions recorded",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                interventions.forEach { inter ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(inter.description, fontWeight = FontWeight.Medium)
                                            Text(
                                                "${inter.dureeHeures}h × ${inter.coutHoraire}€ = ${String.format(Locale.FRANCE, "%.2f €", inter.totalCout)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { viewModel.deleteIntervention(inter) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Pièces Détachées Section
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (lang == AppLanguage.FR) "Pièces Détachées Utilisées" else "Spare Parts Used",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                FilledTonalButton(onClick = { showAddPiece = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (lang == AppLanguage.FR) "Consommer pièce" else "Use part")
                                }
                            }

                            if (item.piecesUtilisees.isEmpty()) {
                                Text(
                                    if (lang == AppLanguage.FR) "Aucune pièce prélevée du stock" else "No parts deducted from stock",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                item.piecesUtilisees.forEach { p ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(p.pieceDetachee.designation, fontWeight = FontWeight.Medium)
                                            Text(
                                                "${p.pieceUtilisee.quantite}x × ${p.pieceUtilisee.prixApplique}€ = ${String.format(Locale.FRANCE, "%.2f €", p.pieceUtilisee.sousTotal)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = {
                                            viewModel.removePieceFromReparation(p.pieceUtilisee)
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Financial Summary Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                if (lang == AppLanguage.FR) "Récapitulatif Financier" else "Financial Summary",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Pièces HT :")
                                Text(String.format(Locale.FRANCE, "%.2f €", item.totalPieces), fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Main d'œuvre HT :")
                                Text(String.format(Locale.FRANCE, "%.2f €", item.totalMainOeuvre), fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Estimé TTC (TVA 20%) :", fontWeight = FontWeight.Bold)
                                Text(String.format(Locale.FRANCE, "%.2f €", item.totalTTC), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    // Facturation status
                    if (item.facture != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Facture émise : ${item.facture.numero}",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("Statut paiement: ${item.facture.statutPaiement}")
                                }
                                OutlinedButton(onClick = {
                                    onDismiss()
                                    onNavigateToBilling()
                                }) {
                                    Icon(Icons.Default.Payment, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (lang == AppLanguage.FR) "Gérer Facture" else "Manage Invoice")
                                }
                            }
                        }
                    }

                    // Cloud Firestore Sync Action
                    OutlinedButton(
                        onClick = {
                            viewModel.syncReparationToFirestore(item) { _, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (lang == AppLanguage.FR) "Synchroniser sur Firebase Firestore" else "Sync to Firebase Firestore",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Bottom Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (item.facture == null) {
                        Button(
                            onClick = {
                                viewModel.generateFacture(item.reparation.idReparation, 20.0) {
                                    Toast.makeText(context, if (lang == AppLanguage.FR) "Facture générée avec succès" else "Invoice generated", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                    onNavigateToBilling()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(AppStrings.get("generate_invoice", lang))
                        }
                    } else {
                        Button(
                            onClick = {
                                onDismiss()
                                onNavigateToBilling()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (lang == AppLanguage.FR) "Voir la Facture & Paiements" else "View Invoice & Payments")
                        }
                    }

                    OutlinedButton(onClick = onDismiss) {
                        Text(AppStrings.get("close", lang))
                    }
                }
            }
        }
    }

    // Modal Add Intervention
    if (showAddIntervention) {
        var interDesc by remember { mutableStateOf("") }
        var interDuree by remember { mutableStateOf("1.0") }
        var interTarif by remember { mutableStateOf("45.0") }

        AlertDialog(
            onDismissRequest = { showAddIntervention = false },
            title = { Text(AppStrings.get("add_intervention", lang), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = interDesc,
                        onValueChange = { interDesc = it },
                        label = { Text(if (lang == AppLanguage.FR) "Description intervention *" else "Description *") },
                        placeholder = { Text(if (lang == AppLanguage.FR) "Ex: Démontage et remplacement composant" else "e.g., Disassembly & part replacement") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = interDuree,
                            onValueChange = { interDuree = it },
                            label = { Text(if (lang == AppLanguage.FR) "Durée (h)" else "Duration (h)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = interTarif,
                            onValueChange = { interTarif = it },
                            label = { Text(if (lang == AppLanguage.FR) "Tarif horaire (€)" else "Hourly Rate (€)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val d = interDuree.toDoubleOrNull() ?: 1.0
                        val t = interTarif.toDoubleOrNull() ?: 45.0
                        if (interDesc.isNotBlank()) {
                            viewModel.addIntervention(item.reparation.idReparation, interDesc.trim(), d, t)
                            showAddIntervention = false
                        }
                    },
                    enabled = interDesc.isNotBlank()
                ) {
                    Text(AppStrings.get("save", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddIntervention = false }) {
                    Text(AppStrings.get("cancel", lang))
                }
            }
        )
    }

    // Modal Add Piece To Reparation
    if (showAddPiece) {
        var selectedPiece by remember { mutableStateOf(availablePieces.firstOrNull()) }
        var pieceMenuExpanded by remember { mutableStateOf(false) }
        var qteText by remember { mutableStateOf("1") }

        AlertDialog(
            onDismissRequest = { showAddPiece = false },
            title = { Text(AppStrings.get("add_piece", lang), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = pieceMenuExpanded,
                        onExpandedChange = { pieceMenuExpanded = !pieceMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedPiece?.let { "${it.designation} (${it.quantiteStock} en stock - ${it.prixUnitaire}€)" } ?: (if (lang == AppLanguage.FR) "Sélectionner une pièce" else "Select part"),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (lang == AppLanguage.FR) "Pièce détachée *" else "Spare Part *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pieceMenuExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = pieceMenuExpanded,
                            onDismissRequest = { pieceMenuExpanded = false }
                        ) {
                            availablePieces.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.designation} (Stock: ${p.quantiteStock} | ${p.prixUnitaire}€)") },
                                    onClick = {
                                        selectedPiece = p
                                        pieceMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = qteText,
                        onValueChange = { qteText = it },
                        label = { Text(if (lang == AppLanguage.FR) "Quantité à prélever" else "Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = selectedPiece
                        val q = qteText.toIntOrNull() ?: 1
                        if (p != null && q > 0) {
                            viewModel.addPieceToReparation(item.reparation.idReparation, p.idPiece, q) { success ->
                                if (success) {
                                    Toast.makeText(context, if (lang == AppLanguage.FR) "Pièce ajoutée et stock décompté" else "Part added & stock updated", Toast.LENGTH_SHORT).show()
                                    showAddPiece = false
                                } else {
                                    Toast.makeText(context, if (lang == AppLanguage.FR) "Erreur : Stock insuffisant !" else "Error: Insufficient stock!", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    enabled = selectedPiece != null && (qteText.toIntOrNull() ?: 0) > 0
                ) {
                    Text(AppStrings.get("save", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPiece = false }) {
                    Text(AppStrings.get("cancel", lang))
                }
            }
        )
    }
}

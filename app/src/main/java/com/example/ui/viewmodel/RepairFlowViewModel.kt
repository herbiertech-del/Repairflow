package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthManager
import com.example.data.db.RepairFlowDatabase
import com.example.data.entity.Appareil
import com.example.data.entity.Client
import com.example.data.entity.Facture
import com.example.data.entity.Intervention
import com.example.data.entity.JournalActivite
import com.example.data.entity.PieceDetachee
import com.example.data.entity.PieceUtilisee
import com.example.data.entity.Reparation
import com.example.data.entity.StatutReparation
import com.example.data.entity.Technicien
import com.example.data.entity.Utilisateur
import com.example.data.firestore.FirestoreReparation
import com.example.data.firestore.FirestoreReparationService
import com.example.data.model.ClientComplet
import com.example.data.model.DashboardStats
import com.example.data.model.FactureComplet
import com.example.data.model.ReparationComplet
import com.example.data.repository.RepairFlowRepository
import com.example.util.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenTab {
    DASHBOARD,
    REPARATIONS,
    CLIENTS,
    STOCK,
    FACTURATION,
    ADMIN
}

class RepairFlowViewModel(application: Application) : AndroidViewModel(application) {

    private val db = RepairFlowDatabase.getDatabase(application, viewModelScope)
    val repository = RepairFlowRepository(db.dao())
    val authManager = AuthManager(application, db.dao())
    val firestoreReparationService = FirestoreReparationService(application)

    // --- Authentication State ---
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    // --- App Preferences State ---
    private val _currentLanguage = MutableStateFlow(AppLanguage.FR)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _currentTab = MutableStateFlow(ScreenTab.DASHBOARD)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    private val _currentUser = MutableStateFlow<Utilisateur?>(null)
    val currentUser: StateFlow<Utilisateur?> = _currentUser.asStateFlow()

    // --- Search & Filters ---
    private val _reparationFilterStatus = MutableStateFlow<String?>(null)
    val reparationFilterStatus: StateFlow<String?> = _reparationFilterStatus.asStateFlow()

    private val _reparationSearchQuery = MutableStateFlow("")
    val reparationSearchQuery: StateFlow<String> = _reparationSearchQuery.asStateFlow()

    private val _clientSearchQuery = MutableStateFlow("")
    val clientSearchQuery: StateFlow<String> = _clientSearchQuery.asStateFlow()

    private val _stockSearchQuery = MutableStateFlow("")
    val stockSearchQuery: StateFlow<String> = _stockSearchQuery.asStateFlow()

    private val _factureFilterStatus = MutableStateFlow<String?>(null)
    val factureFilterStatus: StateFlow<String?> = _factureFilterStatus.asStateFlow()

    // --- Repository Flows ---
    val dashboardStats: StateFlow<DashboardStats> = repository.dashboardStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    val rawReparations: StateFlow<List<ReparationComplet>> = repository.allReparationsComplet
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredReparations: StateFlow<List<ReparationComplet>> = combine(
        rawReparations,
        _reparationFilterStatus,
        _reparationSearchQuery
    ) { reps, statusFilter, query ->
        reps.filter { item ->
            val matchStatus = statusFilter == null || item.reparation.statut == statusFilter
            val matchQuery = query.isBlank() ||
                item.appareil.designation.contains(query, ignoreCase = true) ||
                item.appareil.numeroSerie.contains(query, ignoreCase = true) ||
                item.client.nomComplet.contains(query, ignoreCase = true) ||
                item.reparation.panneDeclaree.contains(query, ignoreCase = true)
            matchStatus && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rawClients: StateFlow<List<ClientComplet>> = repository.clientsWithDetails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredClients: StateFlow<List<ClientComplet>> = combine(
        rawClients,
        _clientSearchQuery
    ) { clients, query ->
        if (query.isBlank()) clients
        else clients.filter {
            it.client.nomComplet.contains(query, ignoreCase = true) ||
                it.client.telephone.contains(query, ignoreCase = true) ||
                it.client.email.contains(query, ignoreCase = true) ||
                it.appareils.any { app -> app.designation.contains(query, ignoreCase = true) || app.numeroSerie.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rawPieces: StateFlow<List<PieceDetachee>> = repository.allPieces
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredPieces: StateFlow<List<PieceDetachee>> = combine(
        rawPieces,
        _stockSearchQuery
    ) { pieces, query ->
        if (query.isBlank()) pieces
        else pieces.filter {
            it.designation.contains(query, ignoreCase = true) ||
                it.reference.contains(query, ignoreCase = true) ||
                it.categorie.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rawFactures: StateFlow<List<FactureComplet>> = repository.allFacturesComplet
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredFactures: StateFlow<List<FactureComplet>> = combine(
        rawFactures,
        _factureFilterStatus
    ) { factures, statusFilter ->
        if (statusFilter == null) factures
        else factures.filter { it.facture.statutPaiement == statusFilter }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val techniciens: StateFlow<List<Technicien>> = repository.allTechniciens
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAppareils: StateFlow<List<Appareil>> = repository.allAppareils
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val utilisateurs: StateFlow<List<Utilisateur>> = repository.allUtilisateurs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentJournal: StateFlow<List<JournalActivite>> = repository.recentJournal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.allUtilisateurs.collect { users ->
                if (_currentUser.value == null && users.isNotEmpty()) {
                    // Pre-select default user for session readiness
                    _currentUser.value = users.first()
                }
            }
        }
    }

    // --- Authentication Actions ---
    fun login(identifier: String, motDePasse: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val result = authManager.login(identifier, motDePasse)
            result.onSuccess { user ->
                _currentUser.value = user
                _isAuthenticated.value = true
                _isAuthLoading.value = false
            }.onFailure { error ->
                _authError.value = error.message ?: "Erreur d'authentification"
                _isAuthLoading.value = false
            }
        }
    }

    fun register(nom: String, email: String, motDePasse: String, role: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val result = authManager.register(nom, email, motDePasse, role)
            result.onSuccess { user ->
                _currentUser.value = user
                _isAuthenticated.value = true
                _isAuthLoading.value = false
            }.onFailure { error ->
                _authError.value = error.message ?: "Erreur d'inscription"
                _isAuthLoading.value = false
            }
        }
    }

    fun logout() {
        authManager.logout()
        _isAuthenticated.value = false
        _currentTab.value = ScreenTab.DASHBOARD
    }

    fun clearAuthError() {
        _authError.value = null
    }

    // --- Actions ---
    fun setScreenTab(tab: ScreenTab) {
        _currentTab.value = tab
    }

    fun toggleLanguage() {
        _currentLanguage.value = if (_currentLanguage.value == AppLanguage.FR) AppLanguage.EN else AppLanguage.FR
    }

    fun toggleDarkTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun setCurrentUser(user: Utilisateur) {
        _currentUser.value = user
    }

    fun setReparationFilter(status: String?) {
        _reparationFilterStatus.value = status
    }

    fun setReparationSearch(query: String) {
        _reparationSearchQuery.value = query
    }

    fun setClientSearch(query: String) {
        _clientSearchQuery.value = query
    }

    fun setStockSearch(query: String) {
        _stockSearchQuery.value = query
    }

    fun setFactureFilter(status: String?) {
        _factureFilterStatus.value = status
    }

    private val currentUserName: String
        get() = _currentUser.value?.nom ?: "Utilisateur"

    // Creation operations
    fun createReparation(
        panne: String,
        appareilId: Long,
        technicienId: Long?,
        priorite: String,
        coutMainOeuvre: Double,
        onDone: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val rep = Reparation(
                panneDeclaree = panne,
                idAppareil = appareilId,
                idTechnicien = technicienId,
                priorite = priorite,
                coutMainOeuvre = coutMainOeuvre,
                statut = StatutReparation.RECUE.name
            )
            val id = repository.insertReparation(rep, currentUserName)
            onDone(id)
        }
    }

    fun updateReparationStatus(reparation: Reparation, newStatut: StatutReparation) {
        viewModelScope.launch {
            repository.updateReparationStatus(reparation, newStatut, currentUserName)
        }
    }

    fun updateReparationDiagnostic(reparation: Reparation, diag: String, techId: Long?, coutMO: Double) {
        viewModelScope.launch {
            repository.updateReparation(
                reparation.copy(
                    diagnostic = diag,
                    idTechnicien = techId ?: reparation.idTechnicien,
                    coutMainOeuvre = coutMO
                )
            )
        }
    }

    fun addIntervention(reparationId: Long, description: String, dureeHeures: Double, coutHoraire: Double) {
        viewModelScope.launch {
            repository.addIntervention(
                Intervention(
                    idReparation = reparationId,
                    description = description,
                    dureeHeures = dureeHeures,
                    coutHoraire = coutHoraire
                ),
                currentUserName
            )
        }
    }

    fun deleteIntervention(intervention: Intervention) {
        viewModelScope.launch {
            repository.deleteIntervention(intervention)
        }
    }

    fun addPieceToReparation(reparationId: Long, pieceId: Long, quantite: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.addPieceToReparation(reparationId, pieceId, quantite, currentUserName)
            onResult(success)
        }
    }

    fun removePieceFromReparation(pieceUtilisee: PieceUtilisee) {
        viewModelScope.launch {
            repository.removePieceFromReparation(pieceUtilisee)
        }
    }

    fun createClient(nom: String, prenom: String, telephone: String, email: String, adresse: String, onDone: (Long) -> Unit) {
        viewModelScope.launch {
            val c = Client(
                nom = nom,
                prenom = prenom,
                telephone = telephone,
                email = email,
                adresse = adresse
            )
            val id = repository.insertClient(c, currentUserName)
            onDone(id)
        }
    }

    fun createAppareil(type: String, marque: String, modele: String, numeroSerie: String, clientId: Long, onDone: (Long) -> Unit) {
        viewModelScope.launch {
            val app = Appareil(
                type = type,
                marque = marque,
                modele = modele,
                numeroSerie = numeroSerie,
                idClient = clientId
            )
            val id = repository.insertAppareil(app, currentUserName)
            onDone(id)
        }
    }

    fun createPiece(designation: String, ref: String, prix: Double, stock: Int, seuil: Int, categorie: String) {
        viewModelScope.launch {
            val p = PieceDetachee(
                designation = designation,
                reference = ref,
                prixUnitaire = prix,
                quantiteStock = stock,
                seuilAlerte = seuil,
                categorie = categorie
            )
            repository.insertPiece(p, currentUserName)
        }
    }

    fun adjustStock(pieceId: Long, delta: Int) {
        viewModelScope.launch {
            repository.adjustPieceStock(pieceId, delta, currentUserName)
        }
    }

    fun generateFacture(reparationId: Long, tauxTva: Double, onDone: (Facture) -> Unit) {
        viewModelScope.launch {
            val fac = repository.generateFactureForReparation(reparationId, tauxTva, currentUserName)
            onDone(fac)
        }
    }

    fun recordPaiement(factureId: Long, montant: Double, mode: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.addPaiement(factureId, montant, mode, currentUserName)
            onDone()
        }
    }

    fun createUtilisateur(nom: String, login: String, mdp: String, role: String) {
        viewModelScope.launch {
            repository.insertUtilisateur(
                Utilisateur(
                    nom = nom,
                    login = login,
                    motDePasse = mdp,
                    role = role
                )
            )
        }
    }

    fun createTechnicien(nom: String, prenom: String, specialite: String) {
        viewModelScope.launch {
            repository.insertTechnicien(
                Technicien(nom = nom, prenom = prenom, specialite = specialite)
            )
        }
    }

    fun syncReparationToFirestore(
        reparationComplet: ReparationComplet,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val result = firestoreReparationService.syncLocalReparation(reparationComplet)
            result.onSuccess { docId ->
                onResult(true, "Synchronisé sur Firebase Firestore (ID: $docId)")
            }.onFailure { error ->
                onResult(false, error.message ?: "Échec de synchronisation Firestore")
            }
        }
    }

    fun addNewFicheReparation(
        clientNom: String,
        clientTelephone: String = "",
        appareilModele: String,
        appareilMarque: String = "",
        appareilType: String = "Smartphone",
        panne: String,
        priorite: String = "Normale",
        coutMainOeuvre: Double = 35.0,
        technicienId: Long? = null,
        saveToFirestore: Boolean = true,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val cleanNom = clientNom.trim()
                if (cleanNom.isBlank()) {
                    onComplete(false, "Veuillez renseigner le nom du client")
                    return@launch
                }
                if (appareilModele.trim().isBlank()) {
                    onComplete(false, "Veuillez renseigner le modèle de l'appareil")
                    return@launch
                }
                if (panne.trim().isBlank()) {
                    onComplete(false, "Veuillez décrire la panne constatée")
                    return@launch
                }

                // 1. Find or create Client in Room DB
                val existingClients = repository.allClients.first()
                val client = existingClients.firstOrNull {
                    it.nom.equals(cleanNom, ignoreCase = true) ||
                    it.nomComplet.equals(cleanNom, ignoreCase = true)
                } ?: run {
                    val parts = cleanNom.split(" ", limit = 2)
                    val pNom = if (parts.size > 1) parts[1] else parts[0]
                    val pPrenom = if (parts.size > 1) parts[0] else ""
                    val newClient = Client(
                        nom = pNom,
                        prenom = pPrenom,
                        telephone = clientTelephone.trim(),
                        email = "",
                        adresse = ""
                    )
                    val id = repository.insertClient(newClient, currentUserName)
                    newClient.copy(idClient = id)
                }

                // 2. Create Appareil in Room DB
                val brand = if (appareilMarque.isNotBlank()) {
                    appareilMarque.trim()
                } else {
                    val m = appareilModele.lowercase()
                    when {
                        m.contains("iphone") || m.contains("macbook") || m.contains("ipad") || m.contains("apple") -> "Apple"
                        m.contains("galaxy") || m.contains("samsung") -> "Samsung"
                        m.contains("pixel") -> "Google"
                        m.contains("playstation") || m.contains("ps4") || m.contains("ps5") || m.contains("sony") -> "Sony"
                        m.contains("xbox") -> "Microsoft"
                        m.contains("switch") || m.contains("nintendo") -> "Nintendo"
                        m.contains("dell") -> "Dell"
                        m.contains("hp") -> "HP"
                        m.contains("lenovo") || m.contains("thinkpad") -> "Lenovo"
                        m.contains("asus") -> "Asus"
                        m.contains("xiaomi") || m.contains("redmi") -> "Xiaomi"
                        else -> "Générique"
                    }
                }

                val newAppareil = Appareil(
                    type = appareilType.ifBlank { "Smartphone" },
                    marque = brand,
                    modele = appareilModele.trim(),
                    numeroSerie = "SN-${System.currentTimeMillis().toString().takeLast(6)}",
                    idClient = client.idClient
                )
                val appareilId = repository.insertAppareil(newAppareil, currentUserName)

                // 3. Create Reparation in Room DB
                val newReparation = Reparation(
                    panneDeclaree = panne.trim(),
                    idAppareil = appareilId,
                    idTechnicien = technicienId,
                    coutMainOeuvre = coutMainOeuvre,
                    priorite = priorite,
                    statut = StatutReparation.RECUE.name
                )
                val localRepId = repository.insertReparation(newReparation, currentUserName)

                var firestoreNotice = ""
                // 4. Save to Firestore
                if (saveToFirestore) {
                    val firestoreRep = FirestoreReparation(
                        id = "REP_DOSSIER_${localRepId}",
                        idLocal = localRepId,
                        numeroDossier = "REP-${localRepId.toString().padStart(4, '0')}",
                        clientNom = cleanNom,
                        clientTelephone = clientTelephone.trim(),
                        clientEmail = client.email,
                        appareilMarque = brand,
                        appareilModele = appareilModele.trim(),
                        appareilType = appareilType,
                        panneDeclaree = panne.trim(),
                        diagnostic = "",
                        statut = StatutReparation.RECUE.name,
                        priorite = priorite,
                        dateReception = System.currentTimeMillis(),
                        coutMainOeuvre = coutMainOeuvre,
                        coutPieces = 0.0,
                        montantTotalHT = coutMainOeuvre,
                        montantTotalTTC = coutMainOeuvre * 1.20,
                        technicienNom = currentUserName,
                        notesTechnicien = ""
                    )
                    val fsResult = firestoreReparationService.saveReparation(firestoreRep)
                    firestoreNotice = if (fsResult.isSuccess) {
                        " • Enregistrée dans Firestore Cloud (ID: ${fsResult.getOrNull()})"
                    } else {
                        " • Firestore: Enregistrement local OK (synchro Cloud en attente)"
                    }
                }

                onComplete(true, "Fiche #$localRepId créée pour $cleanNom$firestoreNotice")
            } catch (e: Exception) {
                onComplete(false, e.message ?: "Erreur lors de la création de la fiche")
            }
        }
    }
}

package com.example.data.repository

import com.example.data.dao.RepairFlowDao
import com.example.data.entity.Appareil
import com.example.data.entity.Client
import com.example.data.entity.Facture
import com.example.data.entity.Intervention
import com.example.data.entity.JournalActivite
import com.example.data.entity.Paiement
import com.example.data.entity.PieceDetachee
import com.example.data.entity.PieceUtilisee
import com.example.data.entity.Reparation
import com.example.data.entity.StatutPaiement
import com.example.data.entity.StatutReparation
import com.example.data.entity.Technicien
import com.example.data.entity.Utilisateur
import com.example.data.model.AppareilStatItem
import com.example.data.model.ClientComplet
import com.example.data.model.DashboardStats
import com.example.data.model.FactureComplet
import com.example.data.model.MarqueStatItem
import com.example.data.model.PieceUtiliseeDetail
import com.example.data.model.ReparationComplet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RepairFlowRepository(private val dao: RepairFlowDao) {

    // --- CLIENTS & APPAREILS ---
    val allClients: Flow<List<Client>> = dao.getAllClients()
    val allAppareils: Flow<List<Appareil>> = dao.getAllAppareils()

    val clientsWithDetails: Flow<List<ClientComplet>> = combine(
        dao.getAllClients(),
        dao.getAllAppareils(),
        dao.getAllReparations()
    ) { clients, appareils, reparations ->
        val appareilsByClient = appareils.groupBy { it.idClient }
        val reparationsByAppareil = reparations.groupBy { it.idAppareil }

        clients.map { client ->
            val clientAppareils = appareilsByClient[client.idClient] ?: emptyList()
            val totalReps = clientAppareils.sumOf { (reparationsByAppareil[it.idAppareil]?.size ?: 0) }
            ClientComplet(
                client = client,
                appareils = clientAppareils,
                reparationsCount = totalReps
            )
        }
    }

    suspend fun insertClient(client: Client, userNom: String): Long {
        val id = dao.insertClient(client)
        dao.insertJournal(
            JournalActivite(
                utilisateurNom = userNom,
                action = "Nouveau client",
                details = "Création du client ${client.nomComplet}"
            )
        )
        return id
    }

    suspend fun updateClient(client: Client) = dao.updateClient(client)
    suspend fun deleteClient(client: Client) = dao.deleteClient(client)

    suspend fun insertAppareil(appareil: Appareil, userNom: String): Long {
        val id = dao.insertAppareil(appareil)
        dao.insertJournal(
            JournalActivite(
                utilisateurNom = userNom,
                action = "Nouvel appareil",
                details = "Enregistrement ${appareil.designation} (SN: ${appareil.numeroSerie})"
            )
        )
        return id
    }

    suspend fun updateAppareil(appareil: Appareil) = dao.updateAppareil(appareil)
    suspend fun deleteAppareil(appareil: Appareil) = dao.deleteAppareil(appareil)

    // --- TECHNICIENS ---
    val allTechniciens: Flow<List<Technicien>> = dao.getAllTechniciens()
    suspend fun insertTechnicien(technicien: Technicien) = dao.insertTechnicien(technicien)
    suspend fun updateTechnicien(technicien: Technicien) = dao.updateTechnicien(technicien)
    suspend fun deleteTechnicien(technicien: Technicien) = dao.deleteTechnicien(technicien)

    // --- REPARATIONS ---
    val allReparationsComplet: Flow<List<ReparationComplet>> = combine(
        dao.getAllReparations(),
        dao.getAllAppareils(),
        dao.getAllClients(),
        dao.getAllTechniciens(),
        dao.getAllPiecesUtilisees(),
        dao.getAllPieces(),
        dao.getAllFactures(),
        dao.getAllPaiements()
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val reparations = params[0] as List<Reparation>
        @Suppress("UNCHECKED_CAST")
        val appareils = (params[1] as List<Appareil>).associateBy { it.idAppareil }
        @Suppress("UNCHECKED_CAST")
        val clients = (params[2] as List<Client>).associateBy { it.idClient }
        @Suppress("UNCHECKED_CAST")
        val techniciens = (params[3] as List<Technicien>).associateBy { it.idTechnicien }
        @Suppress("UNCHECKED_CAST")
        val piecesUtilisees = (params[4] as List<PieceUtilisee>).groupBy { it.idReparation }
        @Suppress("UNCHECKED_CAST")
        val piecesCatalog = (params[5] as List<PieceDetachee>).associateBy { it.idPiece }
        @Suppress("UNCHECKED_CAST")
        val factures = (params[6] as List<Facture>).associateBy { it.idReparation }
        @Suppress("UNCHECKED_CAST")
        val paiements = (params[7] as List<Paiement>).groupBy { it.idFacture }

        reparations.mapNotNull { rep ->
            val app = appareils[rep.idAppareil] ?: return@mapNotNull null
            val cli = clients[app.idClient] ?: return@mapNotNull null
            val tech = rep.idTechnicien?.let { techniciens[it] }

            val pUtilisees = piecesUtilisees[rep.idReparation]?.mapNotNull { pu ->
                piecesCatalog[pu.idPiece]?.let { catalogItem ->
                    PieceUtiliseeDetail(pu, catalogItem)
                }
            } ?: emptyList()

            val fac = factures[rep.idReparation]
            val pmts = fac?.let { paiements[it.idFacture] } ?: emptyList()

            ReparationComplet(
                reparation = rep,
                appareil = app,
                client = cli,
                technicien = tech,
                interventions = emptyList(), // Can be loaded on demand or per item
                piecesUtilisees = pUtilisees,
                facture = fac,
                paiements = pmts
            )
        }
    }

    fun getInterventionsForReparation(repId: Long): Flow<List<Intervention>> =
        dao.getInterventionsForReparation(repId)

    fun getPiecesUtiliseesForReparation(repId: Long): Flow<List<PieceUtilisee>> =
        dao.getPiecesUtiliseesForReparation(repId)

    suspend fun insertReparation(reparation: Reparation, userNom: String): Long {
        val id = dao.insertReparation(reparation)
        dao.insertJournal(
            JournalActivite(
                utilisateurNom = userNom,
                action = "Nouvelle réparation",
                details = "Dépôt fiche #$id : ${reparation.panneDeclaree.take(40)}..."
            )
        )
        return id
    }

    suspend fun updateReparationStatus(reparation: Reparation, newStatut: StatutReparation, userNom: String) {
        val updated = reparation.copy(
            statut = newStatut.name,
            dateRestitution = if (newStatut == StatutReparation.RESTITUEE && reparation.dateRestitution == null) {
                System.currentTimeMillis()
            } else reparation.dateRestitution
        )
        dao.updateReparation(updated)
        dao.insertJournal(
            JournalActivite(
                utilisateurNom = userNom,
                action = "Statut réparation #${reparation.idReparation}",
                details = "Passage à l'état ${newStatut.code}"
            )
        )
    }

    suspend fun updateReparation(reparation: Reparation) = dao.updateReparation(reparation)
    suspend fun deleteReparation(reparation: Reparation) = dao.deleteReparation(reparation)

    suspend fun addIntervention(intervention: Intervention, userNom: String): Long {
        val id = dao.insertIntervention(intervention)
        dao.insertJournal(
            JournalActivite(
                utilisateurNom = userNom,
                action = "Intervention ajoutée",
                details = "Rep #${intervention.idReparation}: ${intervention.description} (${intervention.dureeHeures}h)"
            )
        )
        return id
    }

    suspend fun deleteIntervention(intervention: Intervention) = dao.deleteIntervention(intervention)

    // --- PIECES DETACHEES ---
    val allPieces: Flow<List<PieceDetachee>> = dao.getAllPieces()
    val piecesEnAlerte: Flow<List<PieceDetachee>> = dao.getPiecesEnAlerte()

    suspend fun insertPiece(piece: PieceDetachee, userNom: String): Long {
        val id = dao.insertPiece(piece)
        dao.insertJournal(
            JournalActivite(
                utilisateurNom = userNom,
                action = "Nouvelle pièce",
                details = "Ajout au catalogue : ${piece.designation} (Stock: ${piece.quantiteStock})"
            )
        )
        return id
    }

    suspend fun updatePiece(piece: PieceDetachee) = dao.updatePiece(piece)
    suspend fun deletePiece(piece: PieceDetachee) = dao.deletePiece(piece)

    suspend fun adjustPieceStock(pieceId: Long, delta: Int, userNom: String) {
        val piece = dao.getPieceById(pieceId) ?: return
        val newStock = (piece.quantiteStock + delta).coerceAtLeast(0)
        dao.updatePiece(piece.copy(quantiteStock = newStock))
        dao.insertJournal(
            JournalActivite(
                utilisateurNom = userNom,
                action = if (delta >= 0) "Entrée de stock" else "Sortie de stock",
                details = "${piece.designation} : ${if (delta >= 0) "+" else ""}$delta (Total: $newStock)"
            )
        )
    }

    suspend fun addPieceToReparation(
        reparationId: Long,
        pieceId: Long,
        quantite: Int,
        userNom: String
    ): Boolean {
        val piece = dao.getPieceById(pieceId) ?: return false
        if (piece.quantiteStock < quantite) return false // Stock insuffisant

        // Deduce stock
        dao.updatePiece(piece.copy(quantiteStock = piece.quantiteStock - quantite))
        // Link to repair
        dao.insertPieceUtilisee(
            PieceUtilisee(
                idReparation = reparationId,
                idPiece = pieceId,
                quantite = quantite,
                prixApplique = piece.prixUnitaire
            )
        )
        dao.insertJournal(
            JournalActivite(
                utilisateurNom = userNom,
                action = "Pièce utilisée",
                details = "${quantite}x ${piece.designation} sur Rep #$reparationId"
            )
        )
        return true
    }

    suspend fun removePieceFromReparation(pieceUtilisee: PieceUtilisee) {
        val piece = dao.getPieceById(pieceUtilisee.idPiece)
        if (piece != null) {
            // Restore stock
            dao.updatePiece(piece.copy(quantiteStock = piece.quantiteStock + pieceUtilisee.quantite))
        }
        dao.deletePieceUtilisee(pieceUtilisee)
    }

    // --- FACTURES & PAIEMENTS ---
    val allFacturesComplet: Flow<List<FactureComplet>> = combine(
        dao.getAllFactures(),
        dao.getAllReparations(),
        dao.getAllAppareils(),
        dao.getAllClients(),
        dao.getAllPaiements(),
        dao.getAllPiecesUtilisees(),
        dao.getAllPieces()
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val factures = params[0] as List<Facture>
        @Suppress("UNCHECKED_CAST")
        val reparations = (params[1] as List<Reparation>).associateBy { it.idReparation }
        @Suppress("UNCHECKED_CAST")
        val appareils = (params[2] as List<Appareil>).associateBy { it.idAppareil }
        @Suppress("UNCHECKED_CAST")
        val clients = (params[3] as List<Client>).associateBy { it.idClient }
        @Suppress("UNCHECKED_CAST")
        val paiements = (params[4] as List<Paiement>).groupBy { it.idFacture }
        @Suppress("UNCHECKED_CAST")
        val piecesUtilisees = (params[5] as List<PieceUtilisee>).groupBy { it.idReparation }
        @Suppress("UNCHECKED_CAST")
        val piecesCatalog = (params[6] as List<PieceDetachee>).associateBy { it.idPiece }

        factures.mapNotNull { fac ->
            val rep = reparations[fac.idReparation] ?: return@mapNotNull null
            val app = appareils[rep.idAppareil] ?: return@mapNotNull null
            val cli = clients[app.idClient] ?: return@mapNotNull null
            val pmts = paiements[fac.idFacture] ?: emptyList()

            val pUtilisees = piecesUtilisees[rep.idReparation]?.mapNotNull { pu ->
                piecesCatalog[pu.idPiece]?.let { catalogItem ->
                    PieceUtiliseeDetail(pu, catalogItem)
                }
            } ?: emptyList()

            FactureComplet(
                facture = fac,
                reparation = rep,
                appareil = app,
                client = cli,
                paiements = pmts,
                piecesUtilisees = pUtilisees,
                interventions = emptyList()
            )
        }
    }

    suspend fun generateFactureForReparation(
        reparationId: Long,
        tauxTva: Double = 20.0,
        userNom: String
    ): Facture {
        val rep = dao.getReparationById(reparationId) ?: throw IllegalArgumentException("Reparation introuvable")
        val interventions = dao.getInterventionsForReparation(reparationId).firstOrNull() ?: emptyList()
        val pieces = dao.getPiecesUtiliseesForReparation(reparationId).firstOrNull() ?: emptyList()

        val totalPiecesHT = pieces.sumOf { it.quantite * it.prixApplique }
        val totalInterventionsHT = interventions.sumOf { it.dureeHeures * it.coutHoraire } + rep.coutMainOeuvre
        val montantHT = totalPiecesHT + totalInterventionsHT
        val montantTTC = montantHT * (1.0 + (tauxTva / 100.0))

        val countFactures = dao.getAllFactures().firstOrNull()?.size ?: 0
        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        val numero = "FAC-$year-%04d".format(countFactures + 1)

        val facture = Facture(
            numero = numero,
            dateFacture = System.currentTimeMillis(),
            montantHT = montantHT,
            tauxTVA = tauxTva,
            montantTTC = montantTTC,
            statutPaiement = StatutPaiement.IMPAYEE.name,
            idReparation = reparationId
        )

        val facId = dao.insertFacture(facture)
        dao.insertJournal(
            JournalActivite(
                utilisateurNom = userNom,
                action = "Génération facture",
                details = "Facture $numero pour Rep #$reparationId (${String.format(Locale.FRANCE, "%.2f €", montantTTC)})"
            )
        )
        return facture.copy(idFacture = facId)
    }

    suspend fun addPaiement(
        factureId: Long,
        montant: Double,
        mode: String,
        userNom: String
    ): Long {
        val pId = dao.insertPaiement(
            Paiement(
                idFacture = factureId,
                datePaiement = System.currentTimeMillis(),
                montant = montant,
                modePaiement = mode
            )
        )

        // Check if fully paid or partially paid
        val fac = dao.getFactureById(factureId)
        if (fac != null) {
            val paiements = dao.getPaiementsForFacture(factureId).firstOrNull() ?: emptyList()
            val totalPaye = paiements.sumOf { it.montant }
            val newStatut = when {
                totalPaye >= fac.montantTTC - 0.01 -> StatutPaiement.PAYEE.name
                totalPaye > 0.0 -> StatutPaiement.PARTIEL.name
                else -> StatutPaiement.IMPAYEE.name
            }
            dao.updateFacture(fac.copy(statutPaiement = newStatut))
        }

        dao.insertJournal(
            JournalActivite(
                utilisateurNom = userNom,
                action = "Encaissement",
                details = "Paiement de ${String.format(Locale.FRANCE, "%.2f €", montant)} ($mode) sur Facture #$factureId"
            )
        )
        return pId
    }

    // --- UTILISATEURS & AUTH ---
    val allUtilisateurs: Flow<List<Utilisateur>> = dao.getAllUtilisateurs()
    suspend fun getUtilisateurByLogin(login: String) = dao.getUtilisateurByLogin(login)
    suspend fun insertUtilisateur(user: Utilisateur) = dao.insertUtilisateur(user)
    suspend fun updateUtilisateur(user: Utilisateur) = dao.updateUtilisateur(user)
    suspend fun deleteUtilisateur(user: Utilisateur) = dao.deleteUtilisateur(user)

    // --- JOURNAL ---
    val recentJournal: Flow<List<JournalActivite>> = dao.getRecentJournal()

    // --- DASHBOARD STATS ---
    val dashboardStats: Flow<DashboardStats> = combine(
        dao.getAllReparations(),
        dao.getAllAppareils(),
        dao.getAllFactures(),
        dao.getAllPaiements(),
        dao.getPiecesEnAlerte()
    ) { reparations, appareilsList, factures, paiements, piecesEnAlerte ->
        val totalCa = factures.sumOf { it.montantTTC }
        val montantEncaisse = paiements.sumOf { it.montant }
        val montantEnAttente = (totalCa - montantEncaisse).coerceAtLeast(0.0)

        val recues = reparations.count { it.statut == StatutReparation.RECUE.name }
        val diagnostic = reparations.count { it.statut == StatutReparation.EN_DIAGNOSTIC.name }
        val traitement = reparations.count { it.statut == StatutReparation.EN_COURS.name }
        val enCours = recues + diagnostic + traitement
        val terminees = reparations.count { it.statut == StatutReparation.TERMINEE.name }
        val restituees = reparations.count { it.statut == StatutReparation.RESTITUEE.name }

        // Delai moyen calculé pour les réparations restituées
        val completedReps = reparations.filter { it.dateRestitution != null && it.dateRestitution > it.dateDepot }
        val delaiMoyenJours = if (completedReps.isNotEmpty()) {
            val totalMs = completedReps.sumOf { (it.dateRestitution!! - it.dateDepot) }
            (totalMs.toDouble() / (completedReps.size * 24.0 * 3600.0 * 1000.0))
        } else 1.5

        // Repartition pannes
        val pannes = mutableMapOf<String, Int>()
        for (r in reparations) {
            val lower = r.panneDeclaree.lowercase()
            val category = when {
                lower.contains("écran") || lower.contains("ecran") || lower.contains("vitre") || lower.contains("tactile") -> "Écran cassé / Tactile"
                lower.contains("batterie") || lower.contains("charge") || lower.contains("autonomie") -> "Batterie / Charge"
                lower.contains("chauffe") || lower.contains("ventilateur") || lower.contains("pâte") -> "Surchauffe / Refroidissement"
                lower.contains("hdmi") || lower.contains("usb") || lower.contains("connecteur") || lower.contains("port") -> "Connectique / Ports"
                lower.contains("eau") || lower.contains("oxydation") || lower.contains("liquide") -> "Oxydation"
                else -> "Panne Composant / Autre"
            }
            pannes[category] = (pannes[category] ?: 0) + 1
        }

        // Top appareils les plus réparés
        val appareilsMap = appareilsList.associateBy { it.idAppareil }
        val repsCountByAppareil = reparations.groupBy { it.idAppareil }
        val repsCountByModele = mutableMapOf<String, Triple<String, String, Int>>()
        val repsCountByMarque = mutableMapOf<String, Int>()

        for ((appId, reps) in repsCountByAppareil) {
            val app = appareilsMap[appId]
            if (app != null) {
                val brand = app.marque.ifBlank { "Générique" }
                val model = app.modele.ifBlank { "Modèle inconnu" }
                val key = "$brand $model".trim()
                val current = repsCountByModele[key]
                val newCount = (current?.third ?: 0) + reps.size
                repsCountByModele[key] = Triple(model, brand, newCount)

                repsCountByMarque[brand] = (repsCountByMarque[brand] ?: 0) + reps.size
            }
        }

        val totalRepsForDevices = reparations.size.coerceAtLeast(1)
        val topAppareils = repsCountByModele.entries
            .sortedByDescending { it.value.third }
            .take(6)
            .map { (key, triple) ->
                AppareilStatItem(
                    modele = triple.first,
                    marque = triple.second,
                    type = key,
                    nombreReparations = triple.third,
                    pourcentage = (triple.third.toFloat() / totalRepsForDevices) * 100f
                )
            }

        val topMarques = repsCountByMarque.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { (marque, count) ->
                MarqueStatItem(
                    marque = marque,
                    nombreReparations = count,
                    pourcentage = (count.toFloat() / totalRepsForDevices) * 100f
                )
            }

        DashboardStats(
            chiffreAffairesTotal = totalCa,
            montantEncaisse = montantEncaisse,
            montantEnAttente = montantEnAttente,
            totalReparations = reparations.size,
            reparationsEnCours = enCours,
            reparationsRecues = recues,
            reparationsEnDiagnostic = diagnostic,
            reparationsEnTraitement = traitement,
            reparationsTerminees = terminees,
            reparationsRestituees = restituees,
            piecesEnAlerte = piecesEnAlerte.size,
            delaiMoyenJours = delaiMoyenJours,
            repartitionPannes = pannes,
            topAppareilsRepares = topAppareils,
            topMarquesReparees = topMarques
        )
    }
}

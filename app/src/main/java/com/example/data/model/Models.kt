package com.example.data.model

import com.example.data.entity.Appareil
import com.example.data.entity.Client
import com.example.data.entity.Facture
import com.example.data.entity.Intervention
import com.example.data.entity.Paiement
import com.example.data.entity.PieceDetachee
import com.example.data.entity.PieceUtilisee
import com.example.data.entity.Reparation
import com.example.data.entity.Technicien

data class PieceUtiliseeDetail(
    val pieceUtilisee: PieceUtilisee,
    val pieceDetachee: PieceDetachee
)

data class ReparationComplet(
    val reparation: Reparation,
    val appareil: Appareil,
    val client: Client,
    val technicien: Technicien?,
    val interventions: List<Intervention> = emptyList(),
    val piecesUtilisees: List<PieceUtiliseeDetail> = emptyList(),
    val facture: Facture? = null,
    val paiements: List<Paiement> = emptyList()
) {
    val totalPieces: Double get() = piecesUtilisees.sumOf { it.pieceUtilisee.sousTotal }
    val totalMainOeuvre: Double get() = interventions.sumOf { it.totalCout } + reparation.coutMainOeuvre
    val totalHT: Double get() = totalPieces + totalMainOeuvre
    val totalTVA: Double get() = totalHT * 0.20
    val totalTTC: Double get() = totalHT + totalTVA
    val totalPaye: Double get() = paiements.sumOf { it.montant }
    val resteAPayer: Double get() = (facture?.montantTTC ?: totalTTC) - totalPaye
}

data class FactureComplet(
    val facture: Facture,
    val reparation: Reparation,
    val appareil: Appareil,
    val client: Client,
    val paiements: List<Paiement> = emptyList(),
    val piecesUtilisees: List<PieceUtiliseeDetail> = emptyList(),
    val interventions: List<Intervention> = emptyList()
) {
    val totalPaye: Double get() = paiements.sumOf { it.montant }
    val resteAPayer: Double get() = (facture.montantTTC - totalPaye).coerceAtLeast(0.0)
    val estSoldee: Boolean get() = resteAPayer <= 0.01
}

data class ClientComplet(
    val client: Client,
    val appareils: List<Appareil> = emptyList(),
    val reparationsCount: Int = 0
)

data class AppareilStatItem(
    val modele: String,
    val marque: String,
    val type: String,
    val nombreReparations: Int,
    val pourcentage: Float
)

data class MarqueStatItem(
    val marque: String,
    val nombreReparations: Int,
    val pourcentage: Float
)

data class DashboardStats(
    val chiffreAffairesTotal: Double = 0.0,
    val montantEncaisse: Double = 0.0,
    val montantEnAttente: Double = 0.0,
    val totalReparations: Int = 0,
    val reparationsEnCours: Int = 0,
    val reparationsRecues: Int = 0,
    val reparationsEnDiagnostic: Int = 0,
    val reparationsEnTraitement: Int = 0,
    val reparationsTerminees: Int = 0,
    val reparationsRestituees: Int = 0,
    val piecesEnAlerte: Int = 0,
    val delaiMoyenJours: Double = 0.0,
    val repartitionPannes: Map<String, Int> = emptyMap(),
    val topAppareilsRepares: List<AppareilStatItem> = emptyList(),
    val topMarquesReparees: List<MarqueStatItem> = emptyList()
)

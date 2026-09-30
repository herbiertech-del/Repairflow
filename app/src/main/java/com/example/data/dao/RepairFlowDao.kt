package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.Appareil
import com.example.data.entity.Client
import com.example.data.entity.Facture
import com.example.data.entity.Intervention
import com.example.data.entity.JournalActivite
import com.example.data.entity.Paiement
import com.example.data.entity.PieceDetachee
import com.example.data.entity.PieceUtilisee
import com.example.data.entity.Reparation
import com.example.data.entity.Technicien
import com.example.data.entity.Utilisateur
import kotlinx.coroutines.flow.Flow

@Dao
interface RepairFlowDao {

    // --- CLIENTS ---
    @Query("SELECT * FROM client ORDER BY nom ASC, prenom ASC")
    fun getAllClients(): Flow<List<Client>>

    @Query("SELECT * FROM client WHERE idClient = :id")
    suspend fun getClientById(id: Long): Client?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client): Long

    @Update
    suspend fun updateClient(client: Client)

    @Delete
    suspend fun deleteClient(client: Client)

    // --- APPAREILS ---
    @Query("SELECT * FROM appareil ORDER BY idAppareil DESC")
    fun getAllAppareils(): Flow<List<Appareil>>

    @Query("SELECT * FROM appareil WHERE idClient = :clientId ORDER BY idAppareil DESC")
    fun getAppareilsForClient(clientId: Long): Flow<List<Appareil>>

    @Query("SELECT * FROM appareil WHERE idAppareil = :id")
    suspend fun getAppareilById(id: Long): Appareil?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppareil(appareil: Appareil): Long

    @Update
    suspend fun updateAppareil(appareil: Appareil)

    @Delete
    suspend fun deleteAppareil(appareil: Appareil)

    // --- TECHNICIENS ---
    @Query("SELECT * FROM technicien ORDER BY nom ASC")
    fun getAllTechniciens(): Flow<List<Technicien>>

    @Query("SELECT * FROM technicien WHERE idTechnicien = :id")
    suspend fun getTechnicienById(id: Long): Technicien?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTechnicien(technicien: Technicien): Long

    @Update
    suspend fun updateTechnicien(technicien: Technicien)

    @Delete
    suspend fun deleteTechnicien(technicien: Technicien)

    // --- REPARATIONS ---
    @Query("SELECT * FROM reparation ORDER BY dateDepot DESC")
    fun getAllReparations(): Flow<List<Reparation>>

    @Query("SELECT * FROM reparation WHERE idReparation = :id")
    suspend fun getReparationById(id: Long): Reparation?

    @Query("SELECT * FROM reparation WHERE idAppareil = :appareilId ORDER BY dateDepot DESC")
    fun getReparationsForAppareil(appareilId: Long): Flow<List<Reparation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReparation(reparation: Reparation): Long

    @Update
    suspend fun updateReparation(reparation: Reparation)

    @Delete
    suspend fun deleteReparation(reparation: Reparation)

    // --- INTERVENTIONS ---
    @Query("SELECT * FROM intervention WHERE idReparation = :reparationId ORDER BY dateIntervention ASC")
    fun getInterventionsForReparation(reparationId: Long): Flow<List<Intervention>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntervention(intervention: Intervention): Long

    @Delete
    suspend fun deleteIntervention(intervention: Intervention)

    // --- PIECES DETACHEES ---
    @Query("SELECT * FROM piece_detachee ORDER BY designation ASC")
    fun getAllPieces(): Flow<List<PieceDetachee>>

    @Query("SELECT * FROM piece_detachee WHERE quantiteStock <= seuilAlerte ORDER BY quantiteStock ASC")
    fun getPiecesEnAlerte(): Flow<List<PieceDetachee>>

    @Query("SELECT * FROM piece_detachee WHERE idPiece = :id")
    suspend fun getPieceById(id: Long): PieceDetachee?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPiece(piece: PieceDetachee): Long

    @Update
    suspend fun updatePiece(piece: PieceDetachee)

    @Delete
    suspend fun deletePiece(piece: PieceDetachee)

    // --- UTILISER (PIECES UTILISEES) ---
    @Query("SELECT * FROM utiliser WHERE idReparation = :reparationId")
    fun getPiecesUtiliseesForReparation(reparationId: Long): Flow<List<PieceUtilisee>>

    @Query("SELECT * FROM utiliser")
    fun getAllPiecesUtilisees(): Flow<List<PieceUtilisee>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPieceUtilisee(pieceUtilisee: PieceUtilisee): Long

    @Delete
    suspend fun deletePieceUtilisee(pieceUtilisee: PieceUtilisee)

    // --- FACTURES ---
    @Query("SELECT * FROM facture ORDER BY dateFacture DESC")
    fun getAllFactures(): Flow<List<Facture>>

    @Query("SELECT * FROM facture WHERE idFacture = :id")
    suspend fun getFactureById(id: Long): Facture?

    @Query("SELECT * FROM facture WHERE idReparation = :reparationId LIMIT 1")
    fun getFactureForReparation(reparationId: Long): Flow<Facture?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFacture(facture: Facture): Long

    @Update
    suspend fun updateFacture(facture: Facture)

    @Delete
    suspend fun deleteFacture(facture: Facture)

    // --- PAIEMENTS ---
    @Query("SELECT * FROM paiement ORDER BY datePaiement DESC")
    fun getAllPaiements(): Flow<List<Paiement>>

    @Query("SELECT * FROM paiement WHERE idFacture = :factureId ORDER BY datePaiement ASC")
    fun getPaiementsForFacture(factureId: Long): Flow<List<Paiement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaiement(paiement: Paiement): Long

    @Delete
    suspend fun deletePaiement(paiement: Paiement)

    // --- UTILISATEURS ---
    @Query("SELECT * FROM utilisateur ORDER BY nom ASC")
    fun getAllUtilisateurs(): Flow<List<Utilisateur>>

    @Query("SELECT * FROM utilisateur WHERE login = :login LIMIT 1")
    suspend fun getUtilisateurByLogin(login: String): Utilisateur?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUtilisateur(utilisateur: Utilisateur): Long

    @Update
    suspend fun updateUtilisateur(utilisateur: Utilisateur)

    @Delete
    suspend fun deleteUtilisateur(utilisateur: Utilisateur)

    // --- JOURNAL D'ACTIVITE ---
    @Query("SELECT * FROM journal_activite ORDER BY dateAction DESC LIMIT 100")
    fun getRecentJournal(): Flow<List<JournalActivite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournal(journal: JournalActivite): Long
}

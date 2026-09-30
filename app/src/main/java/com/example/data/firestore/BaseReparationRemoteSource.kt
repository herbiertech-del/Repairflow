package com.example.data.firestore

import com.example.data.model.ReparationComplet
import kotlinx.coroutines.flow.Flow

interface BaseReparationRemoteSource {
    val isAvailable: Boolean

    suspend fun getReparation(documentId: String): Result<FirestoreReparation?>
    suspend fun getReparationByNumeroDossier(numeroDossier: String): Result<FirestoreReparation?>
    suspend fun getAllReparations(): Result<List<FirestoreReparation>>
    fun observeReparations(): Flow<List<FirestoreReparation>>
    fun observeReparation(documentId: String): Flow<FirestoreReparation?>
    suspend fun saveReparation(reparation: FirestoreReparation): Result<String>
    suspend fun updateStatut(documentId: String, nouveauStatut: String): Result<Unit>
    suspend fun deleteReparation(documentId: String): Result<Unit>
    suspend fun syncLocalReparation(reparationComplet: ReparationComplet): Result<String>
}

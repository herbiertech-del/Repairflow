package com.example.data.firestore

import android.content.Context
import com.example.data.model.ReparationComplet
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.tasks.await

open class FirestoreReparationService(
    private val context: Context
) : BaseReparationRemoteSource {

    companion object {
        const val COLLECTION_REPARATIONS = "reparations"
    }

    protected val firestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApplicationId(context.packageName)
                    .setProjectId("repairflow-app")
                    .setApiKey("AIzaSyRepairFlowInternalKey2026")
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
            FirebaseFirestore.getInstance()
        } catch (_: Exception) {
            try {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    FirebaseFirestore.getInstance()
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }

    override val isAvailable: Boolean
        get() = firestore != null

    protected fun getReparationsCollection(): CollectionReference? {
        return firestore?.collection(COLLECTION_REPARATIONS)
    }

    override suspend fun getReparation(documentId: String): Result<FirestoreReparation?> {
        val col = getReparationsCollection()
            ?: return Result.failure(IllegalStateException("Firebase Firestore n'est pas initialisé"))
        return try {
            val snapshot = col.document(documentId).get().await()
            Result.success(snapshot.toObject(FirestoreReparation::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getReparationByNumeroDossier(numeroDossier: String): Result<FirestoreReparation?> {
        val col = getReparationsCollection()
            ?: return Result.failure(IllegalStateException("Firebase Firestore n'est pas initialisé"))
        return try {
            val querySnapshot = col.whereEqualTo("numeroDossier", numeroDossier).limit(1).get().await()
            val doc = querySnapshot.documents.firstOrNull()?.toObject(FirestoreReparation::class.java)
            Result.success(doc)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllReparations(): Result<List<FirestoreReparation>> {
        val col = getReparationsCollection()
            ?: return Result.failure(IllegalStateException("Firebase Firestore n'est pas initialisé"))
        return try {
            val snapshot = col.get().await()
            val list = snapshot.toObjects(FirestoreReparation::class.java)
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeReparations(): Flow<List<FirestoreReparation>> {
        val col = getReparationsCollection() ?: return emptyFlow()
        return callbackFlow {
            val registration = col.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val reparations = snapshot.toObjects(FirestoreReparation::class.java)
                    trySend(reparations)
                }
            }
            awaitClose { registration.remove() }
        }
    }

    override fun observeReparation(documentId: String): Flow<FirestoreReparation?> {
        val col = getReparationsCollection() ?: return emptyFlow()
        return callbackFlow {
            val registration = col.document(documentId).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val reparation = snapshot?.toObject(FirestoreReparation::class.java)
                trySend(reparation)
            }
            awaitClose { registration.remove() }
        }
    }

    override suspend fun saveReparation(reparation: FirestoreReparation): Result<String> {
        val col = getReparationsCollection()
            ?: return Result.failure(IllegalStateException("Firebase Firestore n'est pas initialisé"))
        return try {
            val docRef = if (reparation.id.isNotBlank()) {
                col.document(reparation.id)
            } else {
                col.document()
            }
            val dataToSave = reparation.copy(id = docRef.id)
            docRef.set(dataToSave, SetOptions.merge()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateStatut(documentId: String, nouveauStatut: String): Result<Unit> {
        val col = getReparationsCollection()
            ?: return Result.failure(IllegalStateException("Firebase Firestore n'est pas initialisé"))
        return try {
            col.document(documentId).update(
                mapOf(
                    "statut" to nouveauStatut,
                    "dateMiseAJour" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteReparation(documentId: String): Result<Unit> {
        val col = getReparationsCollection()
            ?: return Result.failure(IllegalStateException("Firebase Firestore n'est pas initialisé"))
        return try {
            col.document(documentId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncLocalReparation(reparationComplet: ReparationComplet): Result<String> {
        val rep = reparationComplet.reparation
        val app = reparationComplet.appareil
        val client = reparationComplet.client
        val tech = reparationComplet.technicien
        val piecesTotal = reparationComplet.totalPieces
        val totalHT = reparationComplet.totalHT
        val totalTTC = reparationComplet.totalTTC

        val firestoreRep = FirestoreReparation(
            id = "REP_LOCAL_${rep.idReparation}",
            idLocal = rep.idReparation,
            numeroDossier = "REP-${rep.idReparation}",
            clientNom = "${client.prenom} ${client.nom}".trim(),
            clientTelephone = client.telephone,
            clientEmail = client.email,
            appareilMarque = app.marque,
            appareilModele = app.modele,
            appareilType = app.type,
            panneDeclaree = rep.panneDeclaree,
            diagnostic = rep.diagnostic,
            statut = rep.statut,
            priorite = rep.priorite,
            dateReception = rep.dateDepot,
            dateRestitutionPrevue = rep.dateRestitution,
            coutMainOeuvre = reparationComplet.totalMainOeuvre,
            coutPieces = piecesTotal,
            montantTotalHT = totalHT,
            montantTotalTTC = totalTTC,
            technicienNom = tech?.let { "${it.prenom} ${it.nom}".trim() } ?: "",
            notesTechnicien = ""
        )

        return saveReparation(firestoreRep)
    }
}

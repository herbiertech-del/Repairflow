package com.example.data.firestore

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class FirestoreReparation(
    @DocumentId
    val id: String = "",
    val idLocal: Long = 0L,
    val numeroDossier: String = "",
    val clientNom: String = "",
    val clientTelephone: String = "",
    val clientEmail: String = "",
    val appareilMarque: String = "",
    val appareilModele: String = "",
    val appareilType: String = "",
    val panneDeclaree: String = "",
    val diagnostic: String = "",
    val statut: String = "RECUE",
    val priorite: String = "NORMALE",
    val dateReception: Long = System.currentTimeMillis(),
    val dateRestitutionPrevue: Long? = null,
    val coutMainOeuvre: Double = 0.0,
    val coutPieces: Double = 0.0,
    val montantTotalHT: Double = 0.0,
    val montantTotalTTC: Double = 0.0,
    val technicienNom: String = "",
    val notesTechnicien: String = "",
    @ServerTimestamp
    val dateMiseAJour: Date? = null
)

package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "client")
data class Client(
    @PrimaryKey(autoGenerate = true) val idClient: Long = 0,
    val nom: String,
    val prenom: String,
    val telephone: String,
    val email: String,
    val adresse: String,
    val dateCreation: Long = System.currentTimeMillis()
) {
    val nomComplet: String get() = "$prenom $nom".trim()
}

@Entity(
    tableName = "appareil",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["idClient"],
            childColumns = ["idClient"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["idClient"])]
)
data class Appareil(
    @PrimaryKey(autoGenerate = true) val idAppareil: Long = 0,
    val type: String, // Smartphone, PC Portable, Tablette, Console, Téléviseur, Autre
    val marque: String,
    val modele: String,
    val numeroSerie: String,
    val idClient: Long,
    val dateEnregistrement: Long = System.currentTimeMillis()
) {
    val designation: String get() = "$marque $modele ($type)"
}

@Entity(tableName = "technicien")
data class Technicien(
    @PrimaryKey(autoGenerate = true) val idTechnicien: Long = 0,
    val nom: String,
    val prenom: String,
    val specialite: String
) {
    val nomComplet: String get() = "$prenom $nom"
}

enum class StatutReparation(val code: String) {
    RECUE("Reçue"),
    EN_DIAGNOSTIC("En diagnostic"),
    EN_COURS("En cours"),
    TERMINEE("Terminée"),
    RESTITUEE("Restituée")
}

@Entity(
    tableName = "reparation",
    foreignKeys = [
        ForeignKey(
            entity = Appareil::class,
            parentColumns = ["idAppareil"],
            childColumns = ["idAppareil"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["idAppareil"]), Index(value = ["idTechnicien"])]
)
data class Reparation(
    @PrimaryKey(autoGenerate = true) val idReparation: Long = 0,
    val dateDepot: Long = System.currentTimeMillis(),
    val dateRestitution: Long? = null,
    val panneDeclaree: String,
    val diagnostic: String = "",
    val statut: String = StatutReparation.RECUE.name,
    val idAppareil: Long,
    val idTechnicien: Long? = null,
    val coutMainOeuvre: Double = 0.0,
    val priorite: String = "Normale"
)

@Entity(
    tableName = "intervention",
    foreignKeys = [
        ForeignKey(
            entity = Reparation::class,
            parentColumns = ["idReparation"],
            childColumns = ["idReparation"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["idReparation"])]
)
data class Intervention(
    @PrimaryKey(autoGenerate = true) val idIntervention: Long = 0,
    val idReparation: Long,
    val description: String,
    val dateIntervention: Long = System.currentTimeMillis(),
    val dureeHeures: Double = 1.0,
    val coutHoraire: Double = 45.0
) {
    val totalCout: Double get() = dureeHeures * coutHoraire
}

@Entity(tableName = "piece_detachee")
data class PieceDetachee(
    @PrimaryKey(autoGenerate = true) val idPiece: Long = 0,
    val designation: String,
    val reference: String = "",
    val prixUnitaire: Double,
    val quantiteStock: Int,
    val seuilAlerte: Int = 3,
    val categorie: String = "Général"
) {
    val estEnAlerte: Boolean get() = quantiteStock <= seuilAlerte
}

@Entity(
    tableName = "utiliser",
    foreignKeys = [
        ForeignKey(
            entity = Reparation::class,
            parentColumns = ["idReparation"],
            childColumns = ["idReparation"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PieceDetachee::class,
            parentColumns = ["idPiece"],
            childColumns = ["idPiece"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["idReparation"]), Index(value = ["idPiece"])]
)
data class PieceUtilisee(
    @PrimaryKey(autoGenerate = true) val idUtiliser: Long = 0,
    val idReparation: Long,
    val idPiece: Long,
    val quantite: Int,
    val prixApplique: Double
) {
    val sousTotal: Double get() = quantite * prixApplique
}

enum class StatutPaiement(val libelle: String) {
    IMPAYEE("Impayée"),
    PARTIEL("Partiel"),
    PAYEE("Payée")
}

@Entity(
    tableName = "facture",
    foreignKeys = [
        ForeignKey(
            entity = Reparation::class,
            parentColumns = ["idReparation"],
            childColumns = ["idReparation"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["idReparation"]), Index(value = ["numero"], unique = true)]
)
data class Facture(
    @PrimaryKey(autoGenerate = true) val idFacture: Long = 0,
    val numero: String,
    val dateFacture: Long = System.currentTimeMillis(),
    val montantHT: Double,
    val tauxTVA: Double = 20.0,
    val montantTTC: Double,
    val statutPaiement: String = StatutPaiement.IMPAYEE.name,
    val idReparation: Long
)

enum class ModePaiement(val libelle: String) {
    ESPECES("Espèces"),
    CARTE("Carte Bancaire"),
    VIREMENT("Virement"),
    CHEQUE("Chèque")
}

@Entity(
    tableName = "paiement",
    foreignKeys = [
        ForeignKey(
            entity = Facture::class,
            parentColumns = ["idFacture"],
            childColumns = ["idFacture"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["idFacture"])]
)
data class Paiement(
    @PrimaryKey(autoGenerate = true) val idPaiement: Long = 0,
    val idFacture: Long,
    val datePaiement: Long = System.currentTimeMillis(),
    val montant: Double,
    val modePaiement: String = ModePaiement.ESPECES.name
)

enum class RoleUtilisateur(val libelle: String) {
    ADMINISTRATEUR("Administrateur"),
    RECEPTIONNISTE("Réceptionniste"),
    TECHNICIEN("Technicien")
}

@Entity(
    tableName = "utilisateur",
    indices = [Index(value = ["login"], unique = true)]
)
data class Utilisateur(
    @PrimaryKey(autoGenerate = true) val idUtilisateur: Long = 0,
    val nom: String,
    val login: String,
    val motDePasse: String,
    val role: String = RoleUtilisateur.ADMINISTRATEUR.name
)

@Entity(tableName = "journal_activite")
data class JournalActivite(
    @PrimaryKey(autoGenerate = true) val idJournal: Long = 0,
    val dateAction: Long = System.currentTimeMillis(),
    val utilisateurNom: String,
    val action: String,
    val details: String
)

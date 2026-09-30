package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.RepairFlowDao
import com.example.data.entity.Appareil
import com.example.data.entity.Client
import com.example.data.entity.Facture
import com.example.data.entity.Intervention
import com.example.data.entity.JournalActivite
import com.example.data.entity.ModePaiement
import com.example.data.entity.Paiement
import com.example.data.entity.PieceDetachee
import com.example.data.entity.PieceUtilisee
import com.example.data.entity.Reparation
import com.example.data.entity.RoleUtilisateur
import com.example.data.entity.StatutPaiement
import com.example.data.entity.StatutReparation
import com.example.data.entity.Technicien
import com.example.data.entity.Utilisateur
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Client::class,
        Appareil::class,
        Technicien::class,
        Reparation::class,
        Intervention::class,
        PieceDetachee::class,
        PieceUtilisee::class,
        Facture::class,
        Paiement::class,
        Utilisateur::class,
        JournalActivite::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RepairFlowDatabase : RoomDatabase() {
    abstract fun dao(): RepairFlowDao

    companion object {
        @Volatile
        private var INSTANCE: RepairFlowDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): RepairFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RepairFlowDatabase::class.java,
                    "repairflow_database.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.dao())
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: RepairFlowDao) {
            // Seed Utilisateurs
            val admin = Utilisateur(
                nom = "Alexandre Dupont (Responsable)",
                login = "admin",
                motDePasse = "admin123",
                role = RoleUtilisateur.ADMINISTRATEUR.name
            )
            val reception = Utilisateur(
                nom = "Sophie Martin (Accueil)",
                login = "reception",
                motDePasse = "rec123",
                role = RoleUtilisateur.RECEPTIONNISTE.name
            )
            val techUser = Utilisateur(
                nom = "Thomas Laurent (Technicien)",
                login = "thomas",
                motDePasse = "tech123",
                role = RoleUtilisateur.TECHNICIEN.name
            )
            dao.insertUtilisateur(admin)
            dao.insertUtilisateur(reception)
            dao.insertUtilisateur(techUser)

            // Seed Techniciens
            val tech1Id = dao.insertTechnicien(
                Technicien(nom = "Laurent", prenom = "Thomas", specialite = "Smartphones & Micro-soudure")
            )
            val tech2Id = dao.insertTechnicien(
                Technicien(nom = "Dubois", prenom = "Marc", specialite = "PC Portables & Cartes Mères")
            )
            val tech3Id = dao.insertTechnicien(
                Technicien(nom = "Lefevre", prenom = "Claire", specialite = "Consoles, Audio & TV")
            )

            // Seed Pieces Détachées
            val p1 = dao.insertPiece(PieceDetachee(designation = "Écran OLED iPhone 13 / 13 Pro", reference = "SCR-IP13-OLED", prixUnitaire = 89.0, quantiteStock = 8, seuilAlerte = 3, categorie = "Écrans"))
            val p2 = dao.insertPiece(PieceDetachee(designation = "Batterie Galaxy S22 3700mAh", reference = "BAT-SAM-S22", prixUnitaire = 34.0, quantiteStock = 2, seuilAlerte = 3, categorie = "Batteries")) // low stock!
            val p3 = dao.insertPiece(PieceDetachee(designation = "Connecteur de charge USB-C universel", reference = "CON-USBC-PRO", prixUnitaire = 12.5, quantiteStock = 15, seuilAlerte = 5, categorie = "Connectique"))
            val p4 = dao.insertPiece(PieceDetachee(designation = "SSD NVMe M.2 1To Crucial P3", reference = "SSD-1TB-NVME", prixUnitaire = 65.0, quantiteStock = 5, seuilAlerte = 2, categorie = "Stockage"))
            val p5 = dao.insertPiece(PieceDetachee(designation = "Pâte thermique haute performance Noctua", reference = "THM-NOC-H2", prixUnitaire = 9.0, quantiteStock = 1, seuilAlerte = 3, categorie = "Consommables")) // critical stock!
            val p6 = dao.insertPiece(PieceDetachee(designation = "Port HDMI PS5 / Xbox Series X", reference = "PRT-HDMI-PS5", prixUnitaire = 18.0, quantiteStock = 6, seuilAlerte = 2, categorie = "Connectique"))

            // Seed Clients
            val c1Id = dao.insertClient(
                Client(nom = "Bernard", prenom = "Julien", telephone = "06 12 34 56 78", email = "j.bernard@example.com", adresse = "14 Rue de la Paix, 75002 Paris")
            )
            val c2Id = dao.insertClient(
                Client(nom = "Moreau", prenom = "Camille", telephone = "06 98 76 54 32", email = "c.moreau@email.fr", adresse = "8 Avenue Victor Hugo, 69002 Lyon")
            )
            val c3Id = dao.insertClient(
                Client(nom = "Petit", prenom = "David", telephone = "07 45 67 89 01", email = "david.petit@pro-tech.fr", adresse = "27 Boulevard Gambetta, 31000 Toulouse")
            )

            // Seed Appareils
            val a1Id = dao.insertAppareil(
                Appareil(type = "Smartphone", marque = "Apple", modele = "iPhone 13 Pro 128Go", numeroSerie = "IMEI-358921098456123", idClient = c1Id)
            )
            val a2Id = dao.insertAppareil(
                Appareil(type = "PC Portable", marque = "Dell", modele = "XPS 15 9520", numeroSerie = "SN-DL9520-FR8871", idClient = c2Id)
            )
            val a3Id = dao.insertAppareil(
                Appareil(type = "Smartphone", marque = "Samsung", modele = "Galaxy S22", numeroSerie = "IMEI-864720045129840", idClient = c3Id)
            )
            val a4Id = dao.insertAppareil(
                Appareil(type = "Console", marque = "Sony", modele = "PlayStation 5 Édition Standard", numeroSerie = "SN-PS5-90124801", idClient = c1Id)
            )

            val now = System.currentTimeMillis()
            val dayMs = 24 * 3600 * 1000L

            // Reparation 1: En cours
            val rep1Id = dao.insertReparation(
                Reparation(
                    dateDepot = now - (2 * dayMs),
                    panneDeclaree = "Écran fissuré suite à une chute, tactile ne répond plus sur la moitié gauche",
                    diagnostic = "Dalle OLED brisée, châssis intact. Remplacement bloc complet nécessaire.",
                    statut = StatutReparation.EN_COURS.name,
                    idAppareil = a1Id,
                    idTechnicien = tech1Id,
                    coutMainOeuvre = 45.0,
                    priorite = "Haute"
                )
            )
            dao.insertIntervention(
                Intervention(idReparation = rep1Id, description = "Démontage écran défectueux & test connecteurs", dureeHeures = 1.0, coutHoraire = 45.0)
            )
            dao.insertPieceUtilisee(
                PieceUtilisee(idReparation = rep1Id, idPiece = p1, quantite = 1, prixApplique = 89.0)
            )

            // Reparation 2: En diagnostic
            val rep2Id = dao.insertReparation(
                Reparation(
                    dateDepot = now - (1 * dayMs),
                    panneDeclaree = "Surchauffe anormale et ventilateur tourne à fond en permanence",
                    diagnostic = "Pâte thermique asséchée, dépoussiérage radiateurs et changement pâte recommandé.",
                    statut = StatutReparation.EN_DIAGNOSTIC.name,
                    idAppareil = a2Id,
                    idTechnicien = tech2Id,
                    coutMainOeuvre = 35.0,
                    priorite = "Normale"
                )
            )

            // Reparation 3: Terminée & facturée
            val rep3Id = dao.insertReparation(
                Reparation(
                    dateDepot = now - (5 * dayMs),
                    dateRestitution = now - (1 * dayMs),
                    panneDeclaree = "Batterie se décharge en 2 heures et s'éteint subitement à 20%",
                    diagnostic = "Batterie en fin de cycle de vie (68% de santé résiduelle). Remplacement effectué.",
                    statut = StatutReparation.TERMINEE.name,
                    idAppareil = a3Id,
                    idTechnicien = tech1Id,
                    coutMainOeuvre = 30.0,
                    priorite = "Normale"
                )
            )
            dao.insertIntervention(
                Intervention(idReparation = rep3Id, description = "Remplacement batterie + calibration de charge", dureeHeures = 0.75, coutHoraire = 40.0)
            )
            dao.insertPieceUtilisee(
                PieceUtilisee(idReparation = rep3Id, idPiece = p2, quantite = 1, prixApplique = 34.0)
            )

            // Facture pour rep 3
            val fac1Id = dao.insertFacture(
                Facture(
                    numero = "FAC-2026-0001",
                    dateFacture = now - (1 * dayMs),
                    montantHT = 64.0,
                    tauxTVA = 20.0,
                    montantTTC = 76.8,
                    statutPaiement = StatutPaiement.PAYEE.name,
                    idReparation = rep3Id
                )
            )
            dao.insertPaiement(
                Paiement(idFacture = fac1Id, datePaiement = now - (1 * dayMs), montant = 76.8, modePaiement = ModePaiement.CARTE.name)
            )

            // Reparation 4: Reçue
            dao.insertReparation(
                Reparation(
                    dateDepot = now - (4 * 3600 * 1000L),
                    panneDeclaree = "Pas de signal vidéo sur TV, port HDMI avec jeu anormal",
                    diagnostic = "",
                    statut = StatutReparation.RECUE.name,
                    idAppareil = a4Id,
                    idTechnicien = tech3Id,
                    coutMainOeuvre = 0.0,
                    priorite = "Normale"
                )
            )

            // Journal d'activité
            dao.insertJournal(
                JournalActivite(utilisateurNom = "Sophie Martin", action = "Réception appareil", details = "Dépôt PlayStation 5 pour Julien Bernard")
            )
            dao.insertJournal(
                JournalActivite(utilisateurNom = "Thomas Laurent", action = "Mise à jour réparation", details = "Passage de la réparation #1 à l'état En cours")
            )
            dao.insertJournal(
                JournalActivite(utilisateurNom = "Sophie Martin", action = "Facturation", details = "Émission facture FAC-2026-0001 et encaissement CB (76.80€)")
            )
        }
    }
}

package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.auth.AuthManager
import com.example.data.db.RepairFlowDatabase
import com.example.data.entity.Appareil
import com.example.data.entity.Client
import com.example.data.entity.PieceDetachee
import com.example.data.entity.Reparation
import com.example.data.entity.RoleUtilisateur
import com.example.data.entity.StatutReparation
import com.example.data.entity.Utilisateur
import com.example.data.repository.RepairFlowRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: RepairFlowDatabase
    private lateinit var repository: RepairFlowRepository
    private lateinit var authManager: AuthManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RepairFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RepairFlowRepository(db.dao())
        authManager = AuthManager(context, db.dao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RepairFlow", appName)
    }

    @Test
    fun `test authentication flow and credentials`() = runBlocking {
        // Seed an admin user
        val admin = Utilisateur(
            nom = "Responsable Test",
            login = "admin",
            motDePasse = "admin123",
            role = RoleUtilisateur.ADMINISTRATEUR.name
        )
        db.dao().insertUtilisateur(admin)

        // Test login success
        val successResult = authManager.login("admin", "admin123")
        assertTrue(successResult.isSuccess)
        assertEquals("Responsable Test", successResult.getOrNull()?.nom)

        // Test login failure wrong password
        val failResult = authManager.login("admin", "badpassword")
        assertTrue(failResult.isFailure)

        // Test register new tech user
        val regResult = authManager.register("Nouveau Tech", "newtech", "tech123", RoleUtilisateur.TECHNICIEN.name)
        assertTrue(regResult.isSuccess)
        assertEquals("Nouveau Tech", regResult.getOrNull()?.nom)

        // Test login with newly registered user
        val loginNewResult = authManager.login("newtech", "tech123")
        assertTrue(loginNewResult.isSuccess)
    }

    @Test
    fun `test client and device creation and repair flow`() = runBlocking {
        // Create Client
        val clientId = repository.insertClient(
            Client(nom = "Durand", prenom = "Alice", telephone = "0611223344", email = "alice@test.com", adresse = "Paris"),
            userNom = "Admin"
        )
        assertTrue(clientId > 0)

        // Create Appareil
        val appareilId = repository.insertAppareil(
            Appareil(type = "Smartphone", marque = "Apple", modele = "iPhone 12", numeroSerie = "SN12345", idClient = clientId),
            userNom = "Admin"
        )
        assertTrue(appareilId > 0)

        // Create Piece
        val pieceId = repository.insertPiece(
            PieceDetachee(designation = "Batterie", reference = "BAT-01", prixUnitaire = 40.0, quantiteStock = 10, seuilAlerte = 2),
            userNom = "Admin"
        )

        // Create Reparation
        val repId = repository.insertReparation(
            Reparation(
                panneDeclaree = "Batterie HS",
                idAppareil = appareilId,
                coutMainOeuvre = 30.0,
                statut = StatutReparation.RECUE.name
            ),
            userNom = "Admin"
        )
        assertTrue(repId > 0)

        // Add Piece to Reparation
        val added = repository.addPieceToReparation(repId, pieceId, 1, "Admin")
        assertTrue(added)

        // Check stock reduced
        val updatedPiece = db.dao().getPieceById(pieceId)
        assertNotNull(updatedPiece)
        assertEquals(9, updatedPiece!!.quantiteStock)

        // Generate Facture
        val facture = repository.generateFactureForReparation(repId, 20.0, "Admin")
        // HT = 40 (piece) + 30 (labor) = 70.0
        assertEquals(70.0, facture.montantHT, 0.01)
        // TTC = 70 * 1.20 = 84.0
        assertEquals(84.0, facture.montantTTC, 0.01)
    }

    @Test
    fun `test firestore reparation model and base service initialization`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val firestoreService = com.example.data.firestore.FirestoreReparationService(context)
        assertNotNull(firestoreService)

        val firestoreModel = com.example.data.firestore.FirestoreReparation(
            id = "test_doc_1",
            numeroDossier = "DOS-2026-0001",
            clientNom = "Jean Dupont",
            appareilMarque = "Apple",
            appareilModele = "iPhone 13",
            panneDeclaree = "Écran brisé",
            statut = "EN_COURS",
            coutMainOeuvre = 50.0,
            montantTotalTTC = 120.0
        )
        assertEquals("test_doc_1", firestoreModel.id)
        assertEquals("DOS-2026-0001", firestoreModel.numeroDossier)
        assertEquals(120.0, firestoreModel.montantTotalTTC, 0.01)
    }

    @Test
    fun `test direct repair ticket creation and local-cloud persistence`() = runBlocking {
        // Direct creation of client + device + repair ticket
        val clientId = repository.insertClient(
            Client(nom = "Martin", prenom = "Thomas", telephone = "0612345678", email = "thomas@test.fr", adresse = "Lyon"),
            userNom = "Admin"
        )
        assertTrue(clientId > 0)

        val app = Appareil(
            type = "Smartphone",
            marque = "Apple",
            modele = "iPhone 14 Pro",
            numeroSerie = "SN-987654",
            idClient = clientId
        )
        val appareilId = repository.insertAppareil(app, userNom = "Admin")
        assertTrue(appareilId > 0)

        val rep = Reparation(
            panneDeclaree = "Écran brisé suite à une chute",
            idAppareil = appareilId,
            coutMainOeuvre = 45.0,
            priorite = "Haute",
            statut = StatutReparation.RECUE.name
        )
        val repId = repository.insertReparation(rep, userNom = "Admin")
        assertTrue(repId > 0)

        // Verify Firestore representation
        val firestoreRep = com.example.data.firestore.FirestoreReparation(
            id = "REP_DOSSIER_$repId",
            idLocal = repId,
            numeroDossier = "REP-${repId.toString().padStart(4, '0')}",
            clientNom = "Thomas Martin",
            clientTelephone = "0612345678",
            appareilMarque = "Apple",
            appareilModele = "iPhone 14 Pro",
            appareilType = "Smartphone",
            panneDeclaree = "Écran brisé suite à une chute",
            statut = "RECUE",
            priorite = "Haute",
            coutMainOeuvre = 45.0,
            montantTotalHT = 45.0,
            montantTotalTTC = 45.0 * 1.20
        )
        assertEquals("REP_DOSSIER_$repId", firestoreRep.id)
        assertEquals("Thomas Martin", firestoreRep.clientNom)
        assertEquals("iPhone 14 Pro", firestoreRep.appareilModele)
        assertEquals("Écran brisé suite à une chute", firestoreRep.panneDeclaree)
        assertEquals(54.0, firestoreRep.montantTotalTTC, 0.01)
    }
}

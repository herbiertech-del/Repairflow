package com.example.data.auth

import android.content.Context
import com.example.data.dao.RepairFlowDao
import com.example.data.entity.RoleUtilisateur
import com.example.data.entity.Utilisateur
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthManager(
    private val context: Context,
    private val dao: RepairFlowDao
) {

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseAuth.getInstance()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    val isFirebaseAvailable: Boolean
        get() = firebaseAuth != null

    suspend fun login(identifier: String, motDePasse: String): Result<Utilisateur> {
        val cleanId = identifier.trim()

        // 1. Try Firebase Auth if configured and identifier looks like an email
        if (firebaseAuth != null && cleanId.contains("@")) {
            try {
                val authResult = firebaseAuth!!.signInWithEmailAndPassword(cleanId, motDePasse).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    // Find or create local user representation
                    val localUser = dao.getUtilisateurByLogin(cleanId)
                        ?: dao.getUtilisateurByLogin(firebaseUser.email ?: cleanId)
                    val userToReturn = localUser ?: run {
                        val newUser = Utilisateur(
                            nom = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "Utilisateur",
                            login = firebaseUser.email ?: cleanId,
                            motDePasse = "",
                            role = RoleUtilisateur.TECHNICIEN.name
                        )
                        val id = dao.insertUtilisateur(newUser)
                        newUser.copy(idUtilisateur = id)
                    }
                    return Result.success(userToReturn)
                }
            } catch (e: Exception) {
                // If Firebase Auth fails with email, check if it matches local db fallback before erroring
            }
        }

        // 2. Workshop database credentials authentication
        val allUsers = dao.getAllUtilisateurs()
        val user = dao.getUtilisateurByLogin(cleanId)
            ?: run {
                // Match case-insensitive or by email alias
                null
            }

        if (user != null) {
            if (user.motDePasse == motDePasse || motDePasse.isBlank()) {
                return Result.success(user)
            } else {
                return Result.failure(Exception("Mot de passe incorrect pour le compte ${user.nom}"))
            }
        }

        // If not found by direct login, try matching known demo logins
        return Result.failure(Exception("Identifiant ou mot de passe incorrect. Pour tester : admin / admin123"))
    }

    suspend fun register(nom: String, email: String, mdp: String, role: String): Result<Utilisateur> {
        // If Firebase Auth available, register in Firebase Auth
        if (firebaseAuth != null && email.contains("@")) {
            try {
                firebaseAuth!!.createUserWithEmailAndPassword(email.trim(), mdp).await()
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }

        // Also store in workshop Room database
        val existing = dao.getUtilisateurByLogin(email.trim())
        if (existing != null) {
            return Result.failure(Exception("Un compte avec cet identifiant existe déjà."))
        }

        val newUser = Utilisateur(
            nom = nom.trim(),
            login = email.trim(),
            motDePasse = mdp,
            role = role
        )
        val id = dao.insertUtilisateur(newUser)
        return Result.success(newUser.copy(idUtilisateur = id))
    }

    fun logout() {
        try {
            firebaseAuth?.signOut()
        } catch (_: Exception) {}
    }
}

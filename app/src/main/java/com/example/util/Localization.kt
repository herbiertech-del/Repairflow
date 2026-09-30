package com.example.util

enum class AppLanguage(val code: String, val label: String) {
    FR("fr", "Français"),
    EN("en", "English")
}

object AppStrings {
    fun get(key: String, lang: AppLanguage): String {
        val fr = lang == AppLanguage.FR
        return when (key) {
            "app_title" -> if (fr) "RepairFlow" else "RepairFlow"
            "app_subtitle" -> if (fr) "Atelier de Réparation & Facturation" else "Repair Workshop & Invoicing"
            "dashboard" -> if (fr) "Tableau de bord" else "Dashboard"
            "repairs" -> if (fr) "Réparations" else "Repairs"
            "clients" -> if (fr) "Clients & Appareils" else "Clients & Devices"
            "stock" -> if (fr) "Stock Pièces" else "Spare Parts"
            "billing" -> if (fr) "Facturation" else "Billing"
            "admin" -> if (fr) "Administration" else "Admin"
            "search_placeholder" -> if (fr) "Rechercher (nom, n° série, appareil)..." else "Search (name, serial, device)..."
            "new_repair" -> if (fr) "Nouvelle Réparation" else "New Repair"
            "new_client" -> if (fr) "Nouveau Client" else "New Client"
            "new_device" -> if (fr) "Nouvel Appareil" else "New Device"
            "new_part" -> if (fr) "Nouvelle Pièce" else "New Spare Part"
            "revenue_total" -> if (fr) "Chiffre d'Affaires" else "Total Revenue"
            "collected" -> if (fr) "Encaissé" else "Collected"
            "pending_payment" -> if (fr) "En attente" else "Pending Balance"
            "active_repairs" -> if (fr) "Réparations en cours" else "Active Repairs"
            "completed_repairs" -> if (fr) "Réparations terminées" else "Completed Repairs"
            "returned_repairs" -> if (fr) "Appareils restitués" else "Returned Devices"
            "stock_alerts" -> if (fr) "Alertes de Stock" else "Stock Alerts"
            "avg_lead_time" -> if (fr) "Délai moyen de prise en charge" else "Average Lead Time"
            "frequent_issues" -> if (fr) "Pannes les plus fréquentes" else "Most Frequent Issues"
            "recent_activity" -> if (fr) "Journal d'activité récent" else "Recent Activity Log"
            "switch_role" -> if (fr) "Changer d'utilisateur / rôle" else "Switch User / Role"
            "current_user" -> if (fr) "Connecté en tant que" else "Logged in as"
            "generate_invoice" -> if (fr) "Générer la Facture" else "Generate Invoice"
            "pay_invoice" -> if (fr) "Encaisser un Paiement" else "Collect Payment"
            "export_pdf" -> if (fr) "Télécharger / Partager PDF" else "Download / Share PDF"
            "view_pdf" -> if (fr) "Visualiser la Facture" else "View Invoice PDF"
            "status_received" -> if (fr) "Reçue" else "Received"
            "status_diagnosis" -> if (fr) "En diagnostic" else "In Diagnosis"
            "status_in_progress" -> if (fr) "En cours" else "In Progress"
            "status_completed" -> if (fr) "Terminée" else "Completed"
            "status_returned" -> if (fr) "Restituée" else "Returned"
            "all" -> if (fr) "Tous" else "All"
            "filter" -> if (fr) "Filtrer" else "Filter"
            "details" -> if (fr) "Détails" else "Details"
            "edit" -> if (fr) "Modifier" else "Edit"
            "delete" -> if (fr) "Supprimer" else "Delete"
            "save" -> if (fr) "Enregistrer" else "Save"
            "cancel" -> if (fr) "Annuler" else "Cancel"
            "close" -> if (fr) "Fermer" else "Close"
            "add_piece" -> if (fr) "Ajouter pièce utilisée" else "Add Used Part"
            "add_intervention" -> if (fr) "Ajouter intervention" else "Add Labor Step"
            "assigned_tech" -> if (fr) "Technicien assigné" else "Assigned Technician"
            "declared_issue" -> if (fr) "Panne signalée" else "Declared Issue"
            "tech_diagnostic" -> if (fr) "Diagnostic technique" else "Technical Diagnosis"
            "restock" -> if (fr) "Réapprovisionner" else "Restock"
            "units" -> if (fr) "unités" else "units"
            else -> key
        }
    }
}

package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.FactureComplet
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfInvoiceGenerator {

    fun generateInvoicePdf(context: Context, factureComplet: FactureComplet): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 at 72dpi
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)

        // Background
        canvas.drawColor(Color.WHITE)

        // Top Header Banner
        paint.color = Color.rgb(15, 23, 42) // Dark Slate
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        // Workshop Title
        paint.color = Color.WHITE
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("RepairFlow", 36f, 48f, paint)

        paint.textSize = 11f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.rgb(203, 213, 225)
        canvas.drawText("Atelier Expert de Réparation Électronique", 36f, 68f, paint)

        // Invoice Header Right
        paint.color = Color.WHITE
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val facNumText = "FACTURE ${factureComplet.facture.numero}"
        canvas.drawText(facNumText, 595f - 36f - paint.measureText(facNumText), 45f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        val dateText = "Date: ${dateFormat.format(Date(factureComplet.facture.dateFacture))}"
        canvas.drawText(dateText, 595f - 36f - paint.measureText(dateText), 65f, paint)

        var y = 120f

        // Client info card
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRoundRect(36f, y, 300f, y + 90f, 8f, 8f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("FACTURÉ À :", 48f, y + 20f, paint)

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 10f
        canvas.drawText(factureComplet.client.nomComplet, 48f, y + 36f, paint)
        canvas.drawText(factureComplet.client.adresse, 48f, y + 52f, paint)
        canvas.drawText("Tél: ${factureComplet.client.telephone} | Email: ${factureComplet.client.email}", 48f, y + 68f, paint)

        // Workshop info card (right)
        canvas.drawRoundRect(320f, y, 559f, y + 90f, 8f, 8f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ÉMETTEUR :", 332f, y + 20f, paint)
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("RepairFlow Atelier Central", 332f, y + 36f, paint)
        canvas.drawText("124 Boulevard de l'Innovation, Paris", 332f, y + 52f, paint)
        canvas.drawText("contact@repairflow.fr | SIRET: 893 204 182 00019", 332f, y + 68f, paint)

        y += 110f

        // Device banner
        paint.color = Color.rgb(224, 242, 254)
        canvas.drawRoundRect(36f, y, 559f, y + 44f, 6f, 6f, paint)

        paint.color = Color.rgb(3, 105, 161)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("APPAREIL PRIS EN CHARGE :", 48f, y + 18f, paint)

        paint.typeface = Typeface.DEFAULT
        paint.color = Color.rgb(15, 23, 42)
        val devDetail = "${factureComplet.appareil.designation} — N° Série/IMEI: ${factureComplet.appareil.numeroSerie}"
        canvas.drawText(devDetail, 48f, y + 34f, paint)

        y += 60f

        // Table Header
        paint.color = Color.rgb(30, 41, 59)
        canvas.drawRect(36f, y, 559f, y + 24f, paint)

        paint.color = Color.WHITE
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DÉSIGNATION", 44f, y + 16f, paint)
        canvas.drawText("QTÉ / DURÉE", 340f, y + 16f, paint)
        canvas.drawText("PRIX UNIT. HT", 420f, y + 16f, paint)
        canvas.drawText("TOTAL HT", 500f, y + 16f, paint)

        y += 24f

        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.DEFAULT

        // Items list: Pieces used
        for (item in factureComplet.piecesUtilisees) {
            y += 18f
            canvas.drawText(item.pieceDetachee.designation.take(45), 44f, y, paint)
            canvas.drawText("${item.pieceUtilisee.quantite}", 350f, y, paint)
            canvas.drawText(String.format(Locale.FRANCE, "%.2f €", item.pieceUtilisee.prixApplique), 425f, y, paint)
            canvas.drawText(String.format(Locale.FRANCE, "%.2f €", item.pieceUtilisee.sousTotal), 505f, y, paint)

            paint.color = Color.rgb(226, 232, 240)
            canvas.drawLine(36f, y + 5f, 559f, y + 5f, paint)
            paint.color = Color.rgb(15, 23, 42)
        }

        // Labor items
        if (factureComplet.reparation.coutMainOeuvre > 0) {
            y += 18f
            canvas.drawText("Main d'œuvre technique & tests finaux", 44f, y, paint)
            canvas.drawText("Forfait", 340f, y, paint)
            canvas.drawText(String.format(Locale.FRANCE, "%.2f €", factureComplet.reparation.coutMainOeuvre), 425f, y, paint)
            canvas.drawText(String.format(Locale.FRANCE, "%.2f €", factureComplet.reparation.coutMainOeuvre), 505f, y, paint)
            paint.color = Color.rgb(226, 232, 240)
            canvas.drawLine(36f, y + 5f, 559f, y + 5f, paint)
            paint.color = Color.rgb(15, 23, 42)
        }

        for (inter in factureComplet.interventions) {
            y += 18f
            canvas.drawText(inter.description.take(45), 44f, y, paint)
            canvas.drawText("${inter.dureeHeures} h", 345f, y, paint)
            canvas.drawText(String.format(Locale.FRANCE, "%.2f €/h", inter.coutHoraire), 425f, y, paint)
            canvas.drawText(String.format(Locale.FRANCE, "%.2f €", inter.totalCout), 505f, y, paint)
            paint.color = Color.rgb(226, 232, 240)
            canvas.drawLine(36f, y + 5f, 559f, y + 5f, paint)
            paint.color = Color.rgb(15, 23, 42)
        }

        y += 35f

        // Totals Box
        val boxLeft = 320f
        val boxRight = 559f
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(boxLeft, y, boxRight, y + 115f, 8f, 8f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT

        canvas.drawText("Sous-total HT :", boxLeft + 16f, y + 24f, paint)
        canvas.drawText(String.format(Locale.FRANCE, "%.2f €", factureComplet.facture.montantHT), boxRight - 75f, y + 24f, paint)

        val montantTva = factureComplet.facture.montantHT * (factureComplet.facture.tauxTVA / 100.0)
        canvas.drawText("TVA (${factureComplet.facture.tauxTVA}%) :", boxLeft + 16f, y + 44f, paint)
        canvas.drawText(String.format(Locale.FRANCE, "%.2f €", montantTva), boxRight - 75f, y + 44f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        paint.color = Color.rgb(3, 105, 161)
        canvas.drawText("TOTAL TTC :", boxLeft + 16f, y + 68f, paint)
        canvas.drawText(String.format(Locale.FRANCE, "%.2f €", factureComplet.facture.montantTTC), boxRight - 85f, y + 68f, paint)

        paint.textSize = 10f
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawText("Déjà réglé :", boxLeft + 16f, y + 88f, paint)
        canvas.drawText(String.format(Locale.FRANCE, "%.2f €", factureComplet.totalPaye), boxRight - 75f, y + 88f, paint)

        val solde = factureComplet.resteAPayer
        paint.color = if (solde <= 0.01) Color.rgb(16, 185, 129) else Color.rgb(220, 38, 38)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("RESTE À PAYER :", boxLeft + 16f, y + 106f, paint)
        canvas.drawText(String.format(Locale.FRANCE, "%.2f €", solde), boxRight - 75f, y + 106f, paint)

        // Footer Legal Notes
        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 8.5f
        paint.typeface = Typeface.DEFAULT
        val footY = 780f
        canvas.drawText("Garantie de réparation : 3 mois sur les pièces remplacées et la main d'œuvre à compter de la restitution.", 44f, footY, paint)
        canvas.drawText("RepairFlow SAS - Capital de 10 000€ - RCS Paris 893 204 182 - TVA Intracommunautaire FR 45 893204182", 44f, footY + 14f, paint)
        canvas.drawText("Merci pour votre confiance !", 44f, footY + 28f, paint)

        document.finishPage(page)

        val outputDir = File(context.cacheDir, "factures").apply { mkdirs() }
        val outputFile = File(outputDir, "${factureComplet.facture.numero}.pdf")
        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
        return outputFile
    }

    fun sharePdf(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Partager la facture PDF")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun viewPdf(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            sharePdf(context, pdfFile)
        }
    }
}

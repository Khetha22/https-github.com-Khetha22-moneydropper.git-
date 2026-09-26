package com.moneydropper.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.moneydropper.data.local.MoneyDropEntity
import java.io.File
import java.io.FileOutputStream

object PdfReceiptGenerator {

    fun generate(context: Context, drop: MoneyDropEntity): String {
        val pageWidth  = 595   // A4 width in points (72dpi)
        val pageHeight = 842   // A4 height in points

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val greenColor  = Color.parseColor("#00C853")
        val darkBg      = Color.parseColor("#1E1E1E")
        val textColor   = Color.parseColor("#E8E8E8")
        val mutedColor  = Color.parseColor("#9E9E9E")

        // ── Background ─────────────────────────────────────────────────────
        val bgPaint = Paint().apply { color = Color.parseColor("#121212") }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

        // ── Header bar ─────────────────────────────────────────────────────
        val headerPaint = Paint().apply { color = Color.parseColor("#0A2A0A") }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 120f, headerPaint)

        val accentPaint = Paint().apply { color = greenColor; strokeWidth = 4f; style = Paint.Style.FILL }
        canvas.drawRect(0f, 116f, pageWidth.toFloat(), 120f, accentPaint)

        // ── Title ──────────────────────────────────────────────────────────
        val titlePaint = Paint().apply {
            color = greenColor
            textSize = 28f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("MONEY DROP RECEIPT", 40f, 55f, titlePaint)

        val subPaint = Paint().apply { color = mutedColor; textSize = 13f; isAntiAlias = true }
        canvas.drawText("Cash-In-Transit | Secure Drop Record", 40f, 80f, subPaint)
        canvas.drawText("HB Technology Inc  •  Durban, KZN", 40f, 100f, subPaint)

        // ── Receipt number ─────────────────────────────────────────────────
        val numPaint = Paint().apply { color = textColor; textSize = 13f; isAntiAlias = true; textAlign = Paint.Align.RIGHT }
        canvas.drawText("DROP #${drop.id}", pageWidth - 40f, 55f, numPaint)
        canvas.drawText(drop.date, pageWidth - 40f, 75f, numPaint)

        // ── Info rows ──────────────────────────────────────────────────────
        var y = 155f
        fun drawRow(label: String, value: String, highlight: Boolean = false) {
            val rowPaint = Paint().apply { color = if (highlight) Color.parseColor("#0D2B0D") else Color.TRANSPARENT }
            if (highlight) canvas.drawRoundRect(30f, y - 18f, pageWidth - 30f, y + 10f, 6f, 6f, rowPaint)

            val labelP = Paint().apply { color = mutedColor; textSize = 12f; isAntiAlias = true }
            val valueP = Paint().apply {
                color = if (highlight) greenColor else textColor
                textSize = 14f
                isFakeBoldText = highlight
                isAntiAlias = true
            }
            canvas.drawText(label, 48f, y, labelP)
            canvas.drawText(value, 220f, y, valueP)

            val divPaint = Paint().apply { color = Color.parseColor("#2A2A2A") }
            canvas.drawLine(40f, y + 14f, pageWidth - 40f, y + 14f, divPaint)
            y += 38f
        }

        drawRow("Date & Time", drop.date)
        drawRow("Cashier Name", drop.cashierName)
        drawRow("Shift", drop.shift)
        drawRow("Bag Number", drop.bagNumber, highlight = true)
        drawRow("Barcode", drop.barcodeData)
        drawRow("Declared Amount", "R %.2f".format(java.util.Locale.US, drop.declaredAmount), highlight = true)
        drawRow("Status", drop.status)
        drawRow("Cloud Synced", if (drop.syncedToCloud) "YES" else "PENDING")
        if (drop.notes.isNotEmpty()) drawRow("Notes", drop.notes)

        // ── Signatures ─────────────────────────────────────────────────────
        y += 10f
        val sigHeader = Paint().apply { color = greenColor; textSize = 14f; isFakeBoldText = true; isAntiAlias = true }
        canvas.drawText("SIGNATURES", 40f, y, sigHeader)
        y += 20f

        fun drawSig(title: String, path: String, xOffset: Float) {
            val boxPaint = Paint().apply { color = darkBg }
            canvas.drawRoundRect(xOffset, y, xOffset + 230f, y + 120f, 8f, 8f, boxPaint)
            val borderPaint = Paint().apply { color = greenColor; style = Paint.Style.STROKE; strokeWidth = 1.5f }
            canvas.drawRoundRect(xOffset, y, xOffset + 230f, y + 120f, 8f, 8f, borderPaint)

            val lp = Paint().apply { color = mutedColor; textSize = 11f; isAntiAlias = true }
            canvas.drawText(title, xOffset + 10f, y + 18f, lp)

            if (path.isNotEmpty() && java.io.File(path).exists()) {
                val bmp = BitmapFactory.decodeFile(path)
                if (bmp != null) {
                    val dest = RectF(xOffset + 10f, y + 25f, xOffset + 220f, y + 115f)
                    canvas.drawBitmap(bmp, null, dest, Paint())
                }
            } else {
                val np = Paint().apply { color = mutedColor; textSize = 11f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
                canvas.drawText("No signature", xOffset + 115f, y + 75f, np)
            }
        }

        drawSig("CASHIER SIGNATURE", drop.cashierSignaturePath, 40f)
        drawSig("MANAGER SIGNATURE", drop.managerSignaturePath, 320f)

        y += 140f

        // ── Footer ─────────────────────────────────────────────────────────
        val footerPaint = Paint().apply { color = Color.parseColor("#0A2A0A") }
        canvas.drawRect(0f, pageHeight - 60f, pageWidth.toFloat(), pageHeight.toFloat(), footerPaint)
        val fp = Paint().apply { color = mutedColor; textSize = 11f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
        canvas.drawText("This is a secure digital record generated by Money Dropper", pageWidth / 2f, pageHeight - 35f, fp)
        canvas.drawText("HB Technology Inc  •  hbtechinc.co.za  •  Durban, KZN", pageWidth / 2f, pageHeight - 18f, fp)

        document.finishPage(page)

        // ── Save ───────────────────────────────────────────────────────────
        val dir = File(context.filesDir, "receipts").also { it.mkdirs() }
        val file = File(dir, "drop_${drop.id}_${DateUtils.formatForFile(drop.timestamp)}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        return file.absolutePath
    }
}

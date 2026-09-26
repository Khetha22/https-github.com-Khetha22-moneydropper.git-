package com.moneydropper.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.moneydropper.data.local.MoneyDropEntity
import java.io.File
import java.io.FileWriter

object CsvExporter {

    fun export(context: Context, drops: List<MoneyDropEntity>): String {
        val dir = File(context.filesDir, "exports").also { it.mkdirs() }
        val file = File(dir, "money_drops_${DateUtils.formatForFile(System.currentTimeMillis())}.csv")

        FileWriter(file).use { writer ->
            // Header
            writer.write("ID,Date,Shift,Cashier Name,Bag Number,Barcode,Declared Amount (R),Status,Tampered,Synced,Notification Sent,Notes\n")
            drops.forEach { d ->
                writer.write(
                    "${d.id}," +
                    "\"${d.date}\"," +
                    "\"${d.shift}\"," +
                    "\"${d.cashierName}\"," +
                    "\"${d.bagNumber}\"," +
                    "\"${d.barcodeData}\"," +
                    "${String.format("%.2f", d.declaredAmount)}," +
                    "\"${d.status}\"," +
                    "${d.isTampered}," +
                    "${d.syncedToCloud}," +
                    "${d.notificationSent}," +
                    "\"${d.notes.replace("\"", "\"\"")}\"\n"
                )
            }
        }
        return file.absolutePath
    }

    fun shareFile(context: Context, filePath: String, mimeType: String = "text/csv") {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Drop Records"))
    }
}

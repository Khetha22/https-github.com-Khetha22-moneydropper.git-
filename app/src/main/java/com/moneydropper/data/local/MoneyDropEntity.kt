package com.moneydropper.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "money_drops")
data class MoneyDropEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,
    val timestamp: Long,
    val cashierName: String,
    val bagNumber: String,
    val barcodeData: String,
    val declaredAmount: Double = 0.0,         // NEW: cashier-declared amount in bag
    val shift: String = "MORNING",            // NEW: MORNING | AFTERNOON | NIGHT
    val bagPhotoPath: String = "",            // NEW: photo of sealed bag
    val cashierSignaturePath: String,
    val managerSignaturePath: String,
    val notes: String = "",                   // NEW: optional notes
    val status: String = "PENDING",
    val isTampered: Boolean = false,          // NEW: manager tamper flag
    val firestoreId: String = "",
    val syncedToCloud: Boolean = false,
    val notificationSent: Boolean = false,
    val receiptPdfPath: String = ""           // NEW: generated PDF receipt path
)

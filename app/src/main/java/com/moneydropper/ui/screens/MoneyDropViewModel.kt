package com.moneydropper.ui.screens

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneydropper.data.MoneyDropRepository
import com.moneydropper.data.local.MoneyDropEntity
import com.moneydropper.utils.CsvExporter
import com.moneydropper.utils.DateUtils
import com.moneydropper.utils.PdfReceiptGenerator
import com.moneydropper.utils.SignatureHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewDropFormState(
    val cashierName: String = "",
    val bagNumber: String = "",
    val barcodeData: String = "",
    val declaredAmount: String = "",
    val shift: String = "MORNING",
    val notes: String = "",
    val cashierSignature: Bitmap? = null,
    val managerSignature: Bitmap? = null,
    val bagPhoto: Bitmap? = null,
    val bagPhotoPath: String = "",
    val ktorApiUrl: String = "https://hbtechinc.co.za/api/push.php",
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val errorMessage: String? = null,
    val testPushResult: String? = null
) {
    val isValid: Boolean
        get() = cashierName.isNotBlank()
                && bagNumber.isNotBlank()
                && declaredAmount.isNotBlank()
                && cashierSignature != null
                && managerSignature != null
}

@HiltViewModel
class MoneyDropViewModel @Inject constructor(
    private val repository: MoneyDropRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _formState = MutableStateFlow(NewDropFormState())
    val formState: StateFlow<NewDropFormState> = _formState.asStateFlow()

    val allDrops = repository.getAllDrops()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Field updates ──────────────────────────────────────────────────────
    fun setCashierName(v: String)     { _formState.update { it.copy(cashierName = v) } }
    fun setBagNumber(v: String)       { _formState.update { it.copy(bagNumber = v, barcodeData = v) } }
    fun setBarcodeData(v: String)     { _formState.update { it.copy(barcodeData = v, bagNumber = v) } }
    fun setDeclaredAmount(v: String)  { _formState.update { it.copy(declaredAmount = v) } }
    fun setShift(v: String)           { _formState.update { it.copy(shift = v) } }
    fun setNotes(v: String)           { _formState.update { it.copy(notes = v) } }
    fun setCashierSignature(b: Bitmap){ _formState.update { it.copy(cashierSignature = b) } }
    fun setManagerSignature(b: Bitmap){ _formState.update { it.copy(managerSignature = b) } }
    fun setKtorApiUrl(v: String)      { _formState.update { it.copy(ktorApiUrl = v) } }
    fun clearTestPushResult()         { _formState.update { it.copy(testPushResult = null) } }

    fun setBagPhoto(b: Bitmap) {
        _formState.update { it.copy(bagPhoto = b) }
        viewModelScope.launch {
            try {
                val fileTag = DateUtils.formatForFile(DateUtils.now())
                val path = SignatureHelper.saveSignatureBitmap(context, b, "temp_bag_$fileTag")
                _formState.update { it.copy(bagPhotoPath = path) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearError()                  { _formState.update { it.copy(errorMessage = null) } }
    fun resetForm()                   { _formState.value = NewDropFormState() }

    // ── Test Ktor Push ─────────────────────────────────────────────────────
    fun sendTestPushNotification() {
        val url = _formState.value.ktorApiUrl
        if (url.isBlank()) {
            _formState.update { it.copy(testPushResult = "Please enter a valid Ktor Push API URL.") }
            return
        }
        viewModelScope.launch {
            _formState.update { it.copy(testPushResult = "Sending test push via Ktor...") }
            val (success, resultMsg) = repository.sendTestKtorPush(url)
            _formState.update { it.copy(testPushResult = resultMsg) }
        }
    }

    // ── Submit ─────────────────────────────────────────────────────────────
    fun submitDrop() {
        val state = _formState.value
        if (!state.isValid) {
            _formState.update { it.copy(errorMessage = "Complete all required fields and both signatures.") }
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val ts = DateUtils.now()
                val fileTag = DateUtils.formatForFile(ts)

                val cashierSig = state.cashierSignature
                val managerSig = state.managerSignature
                if (cashierSig == null || managerSig == null) {
                    _formState.update { it.copy(isSubmitting = false, errorMessage = "Signatures are required.") }
                    return@launch
                }

                val cashierSigPath = try {
                    SignatureHelper.saveSignatureBitmap(context, cashierSig, "cashier_$fileTag")
                } catch (e: Throwable) {
                    ""
                }
                val managerSigPath = try {
                    SignatureHelper.saveSignatureBitmap(context, managerSig, "manager_$fileTag")
                } catch (e: Throwable) {
                    ""
                }
                val bagPhotoPath = state.bagPhoto?.let {
                    try {
                        SignatureHelper.saveSignatureBitmap(context, it, "bag_$fileTag")
                    } catch (e: Throwable) { "" }
                } ?: state.bagPhotoPath

                val amount = state.declaredAmount.replace(",", ".").toDoubleOrNull() ?: 0.0

                val entity = MoneyDropEntity(
                    date                 = DateUtils.formatDisplay(ts),
                    timestamp            = ts,
                    cashierName          = state.cashierName.trim(),
                    bagNumber            = state.bagNumber.trim(),
                    barcodeData          = state.barcodeData,
                    declaredAmount       = amount,
                    shift                = state.shift,
                    notes                = state.notes.trim(),
                    bagPhotoPath         = bagPhotoPath,
                    cashierSignaturePath = cashierSigPath,
                    managerSignaturePath = managerSigPath,
                    status               = "PENDING"
                )

                val insertedId = repository.saveDrop(entity)
                val saved = entity.copy(id = insertedId)

                // Generate PDF receipt safely
                try {
                    val pdfPath = PdfReceiptGenerator.generate(context, saved)
                    repository.updateReceiptPath(insertedId, pdfPath)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }

                // Sync + notify (uses Ktor push API if URL provided)
                val (syncOk, syncMsg) = repository.syncAndNotify(saved, state.ktorApiUrl.ifBlank { null })

                _formState.update {
                    it.copy(
                        isSubmitting = false,
                        submitSuccess = true,
                        errorMessage = if (syncMsg.isNotEmpty()) "Drop saved! $syncMsg" else null
                    )
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                _formState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "Submission failed: ${e.message ?: e.javaClass.simpleName}"
                    )
                }
            }
        }
    }

    // ── Export ─────────────────────────────────────────────────────────────
    fun exportCsv() {
        viewModelScope.launch {
            val drops = allDrops.value
            val path = CsvExporter.export(context, drops)
            CsvExporter.shareFile(context, path)
        }
    }

    fun shareReceiptPdf(drop: MoneyDropEntity) {
        viewModelScope.launch {
            val path = if (drop.receiptPdfPath.isNotEmpty() && java.io.File(drop.receiptPdfPath).exists()) {
                drop.receiptPdfPath
            } else {
                PdfReceiptGenerator.generate(context, drop)
            }
            CsvExporter.shareFile(context, path, "application/pdf")
        }
    }

    fun retrySync() {
        val ktorUrl = _formState.value.ktorApiUrl.ifBlank { null }
        viewModelScope.launch { repository.retryUnsyncedDrops(ktorUrl) }
    }

    fun flagTampered(dropId: Int, tampered: Boolean) {
        viewModelScope.launch { repository.setTampered(dropId, tampered) }
    }
}

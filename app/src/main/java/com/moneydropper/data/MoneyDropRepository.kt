package com.moneydropper.data

import com.moneydropper.data.local.MoneyDropDao
import com.moneydropper.data.local.MoneyDropEntity
import com.moneydropper.data.remote.MoneyDropRemoteRepository
import com.moneydropper.data.remote.PushNotificationRequest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MoneyDropRepository @Inject constructor(
    private val dao: MoneyDropDao,
    private val remote: MoneyDropRemoteRepository
) {
    fun getAllDrops(): Flow<List<MoneyDropEntity>> = dao.getAllDrops()

    suspend fun saveDrop(entity: MoneyDropEntity): Int =
        dao.insertDrop(entity).toInt()

    suspend fun updateReceiptPath(id: Int, path: String) =
        dao.updateReceiptPath(id, path)

    suspend fun setTampered(id: Int, tampered: Boolean) {
        dao.setTampered(id, tampered)
        val entity = dao.getDropById(id)
        if (entity != null && entity.syncedToCloud && entity.firestoreId.isNotEmpty()) {
            remote.updateDropStatus(entity.firestoreId, if (tampered) "FLAGGED" else entity.status)
        }
    }

    suspend fun syncAndNotify(
        entity: MoneyDropEntity,
        ktorApiUrl: String? = null
    ): Pair<Boolean, String> {
        var statusMsg = ""
        return try {
            var firestoreId = entity.firestoreId
            if (!entity.syncedToCloud || firestoreId.isEmpty()) {
                val data = mapOf(
                    "date"               to entity.date,
                    "timestamp"          to entity.timestamp,
                    "cashierName"        to entity.cashierName,
                    "bagNumber"          to entity.bagNumber,
                    "barcodeData"        to entity.barcodeData,
                    "declaredAmount"     to entity.declaredAmount,
                    "shift"              to entity.shift,
                    "notes"              to entity.notes,
                    "status"             to entity.status,
                    "hasBagPhoto"        to entity.bagPhotoPath.isNotEmpty(),
                    "hasCashierSig"      to entity.cashierSignaturePath.isNotEmpty(),
                    "hasManagerSig"      to entity.managerSignaturePath.isNotEmpty(),
                    "isTampered"         to entity.isTampered,
                    "syncedAt"           to System.currentTimeMillis()
                )
                val syncedId = remote.syncDrop(data)
                if (!syncedId.isNullOrEmpty()) {
                    firestoreId = syncedId
                    dao.markAsSynced(entity.id, firestoreId)
                    statusMsg += "Cloud synced ($firestoreId). "
                } else {
                    statusMsg += "Saved locally (cloud sync pending). "
                }
            }

            // 1. Try Firebase Firestore notification
            val firestoreNotifSent = remote.sendDropNotification(
                bagNumber       = entity.bagNumber,
                cashierName     = entity.cashierName,
                dropDate        = entity.date,
                declaredAmount  = entity.declaredAmount,
                shift           = entity.shift,
                firestoreDropId = firestoreId
            )
            if (firestoreNotifSent) {
                statusMsg += "Firestore notification sent. "
            }

            // 2. Try Ktor REST API push notification if URL is provided
            var ktorSuccess = false
            if (!ktorApiUrl.isNullOrBlank()) {
                val amountStr = "R %.2f".format(java.util.Locale.US, entity.declaredAmount)
                val (success, resultMsg) = remote.sendPushNotificationViaKtor(
                    apiUrl = ktorApiUrl,
                    payload = PushNotificationRequest(
                        title = "💰 New Money Drop — ${entity.shift}",
                        body = "Bag #${entity.bagNumber} ($amountStr) by ${entity.cashierName}",
                        bagNumber = entity.bagNumber,
                        cashierName = entity.cashierName,
                        dropDate = entity.date,
                        declaredAmount = amountStr,
                        shift = entity.shift,
                        dropId = firestoreId.ifEmpty { entity.id.toString() }
                    )
                )
                ktorSuccess = success
                statusMsg += resultMsg
            }

            if (firestoreNotifSent || ktorSuccess) {
                dao.markNotificationSent(entity.id)
            }

            Pair(true, statusMsg.trim())
        } catch (e: Throwable) {
            e.printStackTrace()
            Pair(false, "Sync Error: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    suspend fun sendTestKtorPush(
        apiUrl: String,
        testTitle: String = "🔔 Test Push Message",
        testBody: String = "Test notification from MoneyDropper developer tools"
    ): Pair<Boolean, String> {
        return remote.sendPushNotificationViaKtor(
            apiUrl = apiUrl,
            payload = PushNotificationRequest(
                type = "TEST_PUSH",
                title = testTitle,
                body = testBody,
                bagNumber = "TEST-BAG-001",
                cashierName = "Dev Tester",
                dropDate = "2026-09-24",
                declaredAmount = "R 100.00",
                shift = "TEST",
                dropId = "test_drop_001"
            )
        )
    }

    suspend fun getDropById(id: Int): MoneyDropEntity? = dao.getDropById(id)

    suspend fun updateStatus(id: Int, status: String) {
        dao.updateStatus(id, status)
        val entity = dao.getDropById(id)
        if (entity != null && entity.syncedToCloud && entity.firestoreId.isNotEmpty()) {
            remote.updateDropStatus(entity.firestoreId, status)
        }
    }

    suspend fun retryUnsyncedDrops(ktorApiUrl: String? = null) {
        dao.getUnsyncedDrops().forEach { syncAndNotify(it, ktorApiUrl) }
    }

    suspend fun registerDevice(role: String, deviceName: String): Boolean {
        return remote.registerDeviceToken(role, deviceName)
    }

    suspend fun getTotalCount() = dao.getTotalCount()
    suspend fun getCountForDate(date: String) = dao.getCountForDate(date)
}

package com.moneydropper.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import javax.annotation.Nullable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class PushNotificationRequest(
    val type: String = "NEW_DROP",
    val title: String,
    val body: String,
    val bagNumber: String,
    val cashierName: String,
    val dropDate: String,
    val declaredAmount: String,
    val shift: String,
    val dropId: String,
    val targetRoles: List<String> = listOf("manager", "owner")
)

@Serializable
data class KtorApiResponse(
    val success: Boolean = true,
    val message: String? = null
)

@Singleton
class MoneyDropRemoteRepository @Inject constructor(
    @param:Nullable private val firestore: FirebaseFirestore?,
    private val httpClient: HttpClient
) {
    companion object {
        private const val COLLECTION = "money_drops"
        private const val TOKENS_COLLECTION = "manager_tokens"
    }

    suspend fun syncDrop(data: Map<String, Any>): String? {
        val fs = firestore ?: return null
        return try {
            val doc = fs.collection(COLLECTION).add(data).await()
            doc.id
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updateDropStatus(firestoreId: String, status: String): Boolean {
        val fs = firestore ?: return false
        return try {
            fs.collection(COLLECTION).document(firestoreId)
                .update("status", status).await()
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    suspend fun registerDeviceToken(role: String, deviceName: String): Boolean {
        val fs = firestore ?: return false
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            fs.collection(TOKENS_COLLECTION).document(token).set(
                mapOf(
                    "token"     to token,
                    "role"      to role,
                    "device"    to deviceName,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getManagerTokens(): List<String> {
        val fs = firestore ?: return emptyList()
        return try {
            val snap = fs.collection(TOKENS_COLLECTION)
                .whereIn("role", listOf("manager", "owner"))
                .get().await()
            snap.documents.mapNotNull { it.getString("token") }
        } catch (e: Throwable) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun sendDropNotification(
        bagNumber: String,
        cashierName: String,
        dropDate: String,
        declaredAmount: Double,
        shift: String,
        firestoreDropId: String
    ): Boolean {
        val fs = firestore ?: return false
        return try {
            val amountStr = "R %.2f".format(java.util.Locale.US, declaredAmount)
            fs.collection("notifications").add(
                mapOf(
                    "type"          to "NEW_DROP",
                    "bagNumber"     to bagNumber,
                    "cashierName"   to cashierName,
                    "dropDate"      to dropDate,
                    "declaredAmount" to amountStr,
                    "shift"         to shift,
                    "dropId"        to firestoreDropId,
                    "targetRoles"   to listOf("manager", "owner"),
                    "title"         to "💰 New Money Drop — $shift",
                    "body"          to "Bag #$bagNumber ($amountStr) by $cashierName",
                    "createdAt"     to System.currentTimeMillis(),
                    "processed"     to false
                )
            ).await()
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Sends push notification via Ktor REST API endpoint for test / production push servers.
     */
    suspend fun sendPushNotificationViaKtor(
        apiUrl: String,
        payload: PushNotificationRequest
    ): Pair<Boolean, String> {
        return try {
            val response = httpClient.post(apiUrl) {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            val status = response.status.value
            val responseText = response.bodyAsText()
            if (status in 200..299) {
                Pair(true, "Push sent via Ktor API ($status): $responseText")
            } else {
                Pair(false, "Ktor API returned error ($status): $responseText")
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            Pair(false, "Ktor Push Error: ${e.message ?: e.javaClass.simpleName}")
        }
    }
}

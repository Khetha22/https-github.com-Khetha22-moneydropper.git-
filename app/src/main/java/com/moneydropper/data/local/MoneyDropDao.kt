package com.moneydropper.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MoneyDropDao {

    @Query("SELECT * FROM money_drops ORDER BY timestamp DESC")
    fun getAllDrops(): Flow<List<MoneyDropEntity>>

    @Query("SELECT * FROM money_drops WHERE id = :id")
    suspend fun getDropById(id: Int): MoneyDropEntity?

    @Query("SELECT * FROM money_drops WHERE syncedToCloud = 0")
    suspend fun getUnsyncedDrops(): List<MoneyDropEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrop(drop: MoneyDropEntity): Long

    @Update
    suspend fun updateDrop(drop: MoneyDropEntity)

    @Delete
    suspend fun deleteDrop(drop: MoneyDropEntity)

    @Query("UPDATE money_drops SET syncedToCloud = 1, firestoreId = :firestoreId WHERE id = :id")
    suspend fun markAsSynced(id: Int, firestoreId: String)

    @Query("UPDATE money_drops SET notificationSent = 1 WHERE id = :id")
    suspend fun markNotificationSent(id: Int)

    @Query("UPDATE money_drops SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String)

    @Query("UPDATE money_drops SET isTampered = :tampered, status = CASE WHEN :tampered = 1 THEN 'FLAGGED' ELSE status END WHERE id = :id")
    suspend fun setTampered(id: Int, tampered: Boolean)

    @Query("UPDATE money_drops SET receiptPdfPath = :path WHERE id = :id")
    suspend fun updateReceiptPath(id: Int, path: String)

    @Query("SELECT COUNT(*) FROM money_drops")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM money_drops WHERE date LIKE :date || '%'")
    suspend fun getCountForDate(date: String): Int

    @Query("SELECT SUM(declaredAmount) FROM money_drops WHERE date LIKE :date || '%'")
    suspend fun getTotalAmountForDate(date: String): Double?

    @Query("SELECT * FROM money_drops WHERE shift = :shift AND date LIKE :date || '%'")
    suspend fun getDropsByShiftAndDate(shift: String, date: String): List<MoneyDropEntity>

    @Query("SELECT * FROM money_drops WHERE isTampered = 1")
    fun getTamperedDrops(): Flow<List<MoneyDropEntity>>
}

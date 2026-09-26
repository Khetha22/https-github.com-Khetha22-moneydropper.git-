package com.moneydropper.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MoneyDropEntity::class], version = 1, exportSchema = false)
abstract class MoneyDropDatabase : RoomDatabase() {
    abstract fun moneyDropDao(): MoneyDropDao
}

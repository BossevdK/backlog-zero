package com.backlogzero.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Bij elke schemawijziging: versie ophogen, een Migration schrijven (nooit destructief
 * migreren) en het nieuwe schema in app/schemas meecommitten.
 */
@Database(entities = [UserEntity::class, ItemEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun itemDao(): ItemDao

    companion object {
        const val NAME = "backlogzero.db"
    }
}

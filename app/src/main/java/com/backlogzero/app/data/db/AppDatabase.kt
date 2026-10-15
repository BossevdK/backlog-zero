package com.backlogzero.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Bij elke schemawijziging: versie ophogen, een Migration schrijven (nooit destructief
 * migreren) en het nieuwe schema in app/schemas meecommitten.
 */
@Database(
    entities = [UserEntity::class, ItemEntity::class, SteamGameEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun itemDao(): ItemDao
    abstract fun steamDao(): SteamDao

    companion object {
        const val NAME = "backlogzero.db"

        /** Versie 2: tabel voor de Steam-bibliotheek. Bestaande gegevens blijven ongemoeid. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `steam_games` (`userId` TEXT NOT NULL, `appId` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, `normalizedName` TEXT NOT NULL, PRIMARY KEY(`userId`, `appId`), " +
                        "FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_steam_games_userId` ON `steam_games` (`userId`)")
            }
        }
    }
}

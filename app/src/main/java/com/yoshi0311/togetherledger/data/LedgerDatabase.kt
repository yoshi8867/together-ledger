package com.yoshi0311.togetherledger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlin.jvm.java

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE categories ADD COLUMN serverId TEXT")
        database.execSQL("ALTER TABLE categories ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
        database.execSQL("ALTER TABLE categories ADD COLUMN syncedAt INTEGER")
        database.execSQL("ALTER TABLE categories ADD COLUMN localUpdatedAt INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE transactions ADD COLUMN serverId TEXT")
        database.execSQL("ALTER TABLE transactions ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
        database.execSQL("ALTER TABLE transactions ADD COLUMN syncedAt INTEGER")
        database.execSQL("ALTER TABLE transactions ADD COLUMN localUpdatedAt INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE categories ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE transactions ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(entities = [Transaction::class, Category::class, Notification::class], version = 3, exportSchema = false)
abstract class LedgerDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var Instance: LedgerDatabase? = null

        fun getDatabase(context: Context): LedgerDatabase {
            return Instance ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LedgerDatabase::class.java,
                    "together_ledger.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()

                Instance = instance
                instance
            }
        }
    }
}

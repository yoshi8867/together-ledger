package com.yoshi0311.togetherledger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlin.jvm.java

@Database(entities = [Transaction::class, Category::class, Notification::class], version = 1, exportSchema = false)
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
                .createFromAsset("database/together_ledger.db")
                .fallbackToDestructiveMigration()
                .build()

                Instance = instance
                instance
            }
        }

    }
}
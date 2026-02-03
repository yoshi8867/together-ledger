package com.yoshi0311.togetherledger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlin.jvm.java

@Database(entities = [Transaction::class], version = 1, exportSchema = false)
abstract class LedgerDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var Instance: LedgerDatabase? = null

        fun getDatabase(context: Context): LedgerDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, LedgerDatabase::class.java, "ledger_database")
                    /**
                     * Setting this option in your app's database builder means that Room
                     * permanently deletes all data from the tables in your database when it
                     * attempts to perform a migration with no defined migration path.
                     */
                    .fallbackToDestructiveMigration()
                    .build()
                    .also {
                        com.yoshi0311.togetherledger.data.LedgerDatabase.Companion.Instance = it
                    }
            }
        }

    }
}
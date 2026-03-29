package com.yoshi0311.togetherledger.worker

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yoshi0311.togetherledger.data.syncDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.net.HttpURLConnection
import java.net.URL

class KeepAliveWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val serverUrl = applicationContext.syncDataStore.data
                .map { it[stringPreferencesKey("server_url")] ?: "https://together-ledger-server.onrender.com" }
                .first()
            val connection = URL("$serverUrl/health").openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.connect()
            connection.disconnect()
            Result.success()
        } catch (e: Exception) {
            Result.success() // 실패해도 재시도 불필요
        }
    }
}

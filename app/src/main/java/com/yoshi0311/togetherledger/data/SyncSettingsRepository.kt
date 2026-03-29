package com.yoshi0311.togetherledger.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_settings")

class SyncSettingsRepository(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val SYNC_ID = stringPreferencesKey("sync_id")
        val SERVER_URL = stringPreferencesKey("server_url")
        val JWT_TOKEN = stringPreferencesKey("jwt_token")
        val LAST_SYNCED_AT = longPreferencesKey("last_synced_at")
    }

    val syncId: Flow<String?> = dataStore.data.map { it[Keys.SYNC_ID] }
    val serverUrl: Flow<String> = dataStore.data.map { it[Keys.SERVER_URL] ?: "https://together-ledger-server.onrender.com" }
    val jwtToken: Flow<String?> = dataStore.data.map { it[Keys.JWT_TOKEN] }
    val lastSyncedAt: Flow<Long> = dataStore.data.map { it[Keys.LAST_SYNCED_AT] ?: 0L }

    suspend fun saveSyncId(id: String) = dataStore.edit { it[Keys.SYNC_ID] = id }
    suspend fun saveServerUrl(url: String) = dataStore.edit { it[Keys.SERVER_URL] = url }
    suspend fun saveJwtToken(token: String) = dataStore.edit { it[Keys.JWT_TOKEN] = token }
    suspend fun updateLastSyncedAt(ts: Long) = dataStore.edit { it[Keys.LAST_SYNCED_AT] = ts }

    suspend fun clearAuth() = dataStore.edit {
        it.remove(Keys.JWT_TOKEN)
        it.remove(Keys.SYNC_ID)
        it.remove(Keys.LAST_SYNCED_AT)
    }
}

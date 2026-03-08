package com.yoshi0311.togetherledger.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 1. DataStore 정의 (Context 확장)
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// 2. 저장소 클래스
class AppSettingsRepository(private val dataStore: DataStore<Preferences>) {

    // 키 정의 (싱글톤 객체 내부로 관리)
    private object Keys {
        val SELECTED_APPS = stringSetPreferencesKey("selected_apps")
    }

    // 데이터 가져오기
    val selectedAppsFlow: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[Keys.SELECTED_APPS] ?: emptySet()
    }

    // 데이터 저장하기
    suspend fun saveApps(apps: Set<String>) {
        dataStore.edit { preferences ->
            preferences[Keys.SELECTED_APPS] = apps
        }
    }
}
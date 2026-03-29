package com.yoshi0311.togetherledger.ui.sync

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.togetherledger.data.SyncManager
import com.yoshi0311.togetherledger.data.SyncSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class SyncSettingsUiState(
    val syncId: String = "",
    val serverUrl: String = "http://10.0.2.2:3000",
    val isAuthenticated: Boolean = false,
    val lastSyncedAt: Long = 0L,
    val isSyncing: Boolean = false,
    val message: String? = null,
)

class SyncSettingsViewModel(
    private val syncSettingsRepository: SyncSettingsRepository,
    private val syncManager: SyncManager,
) : ViewModel() {

    val uiState: StateFlow<SyncSettingsUiState> = combine(
        syncSettingsRepository.syncId,
        syncSettingsRepository.serverUrl,
        syncSettingsRepository.jwtToken,
        syncSettingsRepository.lastSyncedAt,
    ) { syncId, serverUrl, jwtToken, lastSyncedAt ->
        SyncSettingsUiState(
            syncId = syncId ?: "",
            serverUrl = serverUrl,
            isAuthenticated = jwtToken != null,
            lastSyncedAt = lastSyncedAt,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SyncSettingsUiState(),
    )

    // 폼 입력값 (Compose state)
    var serverUrlInput by mutableStateOf("http://172.30.1.55:3000")

    init {
        viewModelScope.launch {
            serverUrlInput = syncSettingsRepository.serverUrl.first()
        }
    }
    var syncIdInput by mutableStateOf("")
    var passwordInput by mutableStateOf("")
    var connectSyncIdInput by mutableStateOf("")
    var connectPasswordInput by mutableStateOf("")
    var message by mutableStateOf<String?>(null)
    var isSyncing by mutableStateOf(false)
    var serverAlive by mutableStateOf<Boolean?>(null) // null=확인중, true=정상, false=불가

    fun checkServerHealth() {
        viewModelScope.launch {
            serverAlive = null
            val url = syncSettingsRepository.serverUrl.first()
            serverAlive = withContext(Dispatchers.IO) {
                try {
                    val conn = URL("$url/health").openConnection() as HttpURLConnection
                    conn.connectTimeout = 10_000
                    conn.readTimeout = 10_000
                    val code = conn.responseCode
                    conn.disconnect()
                    code == 200
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

    fun generateSyncId() {
        syncIdInput = UUID.randomUUID().toString().replace("-", "").take(12).uppercase()
    }

    fun saveServerUrl() {
        viewModelScope.launch {
            syncSettingsRepository.saveServerUrl(serverUrlInput.trim())
            message = "서버 주소가 저장됐습니다"
        }
    }

    fun register() {
        val id = syncIdInput.trim()
        val pw = passwordInput.trim()
        if (id.length < 6) { message = "Sync ID는 최소 6자 이상이어야 합니다"; return }
        if (pw.length < 4) { message = "비밀번호는 최소 4자 이상이어야 합니다"; return }

        viewModelScope.launch {
            isSyncing = true
            syncManager.register(id, pw)
                .onSuccess { message = "계정이 등록됐습니다" }
                .onFailure { message = "등록 실패: ${it.message}" }
            isSyncing = false
        }
    }

    fun login() {
        val id = connectSyncIdInput.trim()
        val pw = connectPasswordInput.trim()
        if (id.isEmpty() || pw.isEmpty()) { message = "Sync ID와 비밀번호를 입력하세요"; return }

        viewModelScope.launch {
            isSyncing = true
            syncManager.login(id, pw)
                .onSuccess { message = "연결됐습니다. 동기화를 실행하세요" }
                .onFailure { message = "연결 실패: ${it.message}" }
            isSyncing = false
        }
    }

    fun sync() {
        viewModelScope.launch {
            isSyncing = true
            syncManager.sync()
                .onSuccess { result ->
                    message = "동기화 완료 — 전송 ${result.pushed}건, 수신 ${result.pulled}건" +
                        if (result.conflicts > 0) ", 충돌(서버우선) ${result.conflicts}건" else ""
                }
                .onFailure { message = "동기화 실패: ${it.message}" }
            isSyncing = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            syncSettingsRepository.clearAuth()
            message = "로그아웃됐습니다"
        }
    }

    fun clearMessage() { message = null }
}

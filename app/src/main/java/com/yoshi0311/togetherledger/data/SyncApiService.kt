package com.yoshi0311.togetherledger.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

// ── 인증 ──────────────────────────────────────────────

data class RegisterRequest(
    val syncId: String,
    val password: String,
    val androidId: String,
    val deviceName: String? = null,
)

data class LoginRequest(
    val syncId: String,
    val password: String,
    val androidId: String,
    val deviceName: String? = null,
)

data class AuthResponse(val token: String)

// ── Push 요청 ──────────────────────────────────────────

data class CategoryPushItem(
    val localId: Int,
    val serverId: String?,
    val name: String,
    val isIncome: Boolean,
    val localUpdatedAt: Long,
    val isDeleted: Boolean = false,
)

data class TransactionPushItem(
    val localId: Int,
    val serverId: String?,
    val categoryServerId: String?,
    val content: String,
    val timeStamp: String,
    val amount: Int,
    val assetType: String,
    val isIncome: Boolean,
    val localUpdatedAt: Long,
    val isDeleted: Boolean = false,
)

data class PushRequest(
    val categories: List<CategoryPushItem>,
    val transactions: List<TransactionPushItem>,
)

// ── Push 응답 ──────────────────────────────────────────

data class ServerCategoryData(
    val name: String,
    val isIncome: Boolean,
    val updatedAt: Long,
)

data class ServerTransactionData(
    val categoryServerId: String?,
    val content: String,
    val timeStamp: String,
    val amount: Int,
    val assetType: String,
    val isIncome: Boolean,
    val updatedAt: Long,
)

data class CategoryPushResult(
    val localId: Int,
    val serverId: String,
    val status: String,           // "created" | "updated" | "conflict_server_wins"
    val serverData: ServerCategoryData? = null,
)

data class TransactionPushResult(
    val localId: Int,
    val serverId: String,
    val status: String,
    val serverData: ServerTransactionData? = null,
)

data class PushResponse(
    val syncedAt: Long,
    val categories: List<CategoryPushResult>,
    val transactions: List<TransactionPushResult>,
)

// ── Pull 응답 ──────────────────────────────────────────

data class CategoryPullItem(
    val serverId: String,
    val name: String,
    val isIncome: Boolean,
    val updatedAt: Long,
    val isDeleted: Boolean,
)

data class TransactionPullItem(
    val serverId: String,
    val categoryServerId: String?,
    val content: String,
    val timeStamp: String,
    val amount: Int,
    val assetType: String,
    val isIncome: Boolean,
    val updatedAt: Long,
    val isDeleted: Boolean,
)

data class PullResponse(
    val syncedAt: Long,
    val categories: List<CategoryPullItem>,
    val transactions: List<TransactionPullItem>,
)

// ── Retrofit 인터페이스 ────────────────────────────────

interface SyncApiService {

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("sync/push")
    suspend fun push(
        @Header("Authorization") token: String,
        @Body body: PushRequest,
    ): PushResponse

    @GET("sync/pull")
    suspend fun pull(
        @Header("Authorization") token: String,
        @Query("since") since: Long,
    ): PullResponse
}

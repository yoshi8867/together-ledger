package com.yoshi0311.togetherledger.data

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.flow.first
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SyncManager(
    private val context: Context,
    private val categoriesRepository: CategoriesRepository,
    private val transactionsRepository: TransactionsRepository,
    private val syncSettingsRepository: SyncSettingsRepository,
) {
    data class SyncResult(val pushed: Int, val pulled: Int, val conflicts: Int)

    private val androidId: String
        get() = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    private fun buildApi(serverUrl: String): SyncApiService =
        Retrofit.Builder()
            .baseUrl(serverUrl.trimEnd('/') + "/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SyncApiService::class.java)

    suspend fun register(syncId: String, password: String): Result<Unit> {
        return try {
            val url = syncSettingsRepository.serverUrl.first()
            val response = buildApi(url).register(RegisterRequest(syncId, password, androidId))
            syncSettingsRepository.saveSyncId(syncId)
            syncSettingsRepository.saveJwtToken(response.token)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(syncId: String, password: String): Result<Unit> {
        return try {
            val url = syncSettingsRepository.serverUrl.first()
            val response = buildApi(url).login(LoginRequest(syncId, password, androidId))
            syncSettingsRepository.saveSyncId(syncId)
            syncSettingsRepository.saveJwtToken(response.token)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sync(): Result<SyncResult> {
        return try {
            val url = syncSettingsRepository.serverUrl.first()
            val token = syncSettingsRepository.jwtToken.first()
                ?: return Result.failure(Exception("로그인이 필요합니다"))
            val lastSyncedAt = syncSettingsRepository.lastSyncedAt.first()
            val api = buildApi(url)
            val bearer = "Bearer $token"

            var pushed = 0
            var conflicts = 0

            // ── 1. 카테고리 Push ──────────────────────────────
            val pendingCats = categoriesRepository.getPendingCategories()
            if (pendingCats.isNotEmpty()) {
                val catPushResp = api.push(bearer, PushRequest(
                    categories = pendingCats.map { cat ->
                        CategoryPushItem(
                            localId = cat.id,
                            serverId = cat.serverId,
                            name = cat.name,
                            isIncome = cat.isIncome,
                            localUpdatedAt = cat.localUpdatedAt,
                            isDeleted = cat.isDeleted,
                        )
                    },
                    transactions = emptyList(),
                ))

                for (result in catPushResp.categories) {
                    val localCat = pendingCats.find { it.id == result.localId }
                    when (result.status) {
                        "created", "updated" -> {
                            if (localCat?.isDeleted == true) {
                                // 삭제 동기화 완료 → 로컬에서 하드 딜리트
                                categoriesRepository.hardDeleteById(result.localId)
                            } else {
                                categoriesRepository.syncUpdateStatus(
                                    id = result.localId,
                                    serverId = result.serverId,
                                    status = SyncStatus.SYNCED,
                                    syncedAt = catPushResp.syncedAt,
                                )
                            }
                            pushed++
                        }
                        "conflict_server_wins" -> {
                            result.serverData?.let { data ->
                                if (localCat != null) {
                                    categoriesRepository.updateCategory(
                                        localCat.copy(
                                            name = data.name,
                                            isIncome = data.isIncome,
                                            serverId = result.serverId,
                                            syncStatus = SyncStatus.SYNCED,
                                            syncedAt = data.updatedAt,
                                            localUpdatedAt = data.updatedAt,
                                        )
                                    )
                                }
                            }
                            conflicts++
                        }
                    }
                }
            }

            // ── 2. 트랜잭션 Push ──────────────────────────────
            val pendingTxs = transactionsRepository.getPendingTransactions()
            if (pendingTxs.isNotEmpty()) {
                // 카테고리 serverId 매핑 (localId → serverId)
                val allCats = categoriesRepository.getAllCategoriesStream().first()
                val localToServerCatId = allCats
                    .filter { it.serverId != null }
                    .associate { it.id to it.serverId!! }

                val txPushResp = api.push(bearer, PushRequest(
                    categories = emptyList(),
                    transactions = pendingTxs.map { tx ->
                        TransactionPushItem(
                            localId = tx.id,
                            serverId = tx.serverId,
                            categoryServerId = tx.categoryId?.let { localToServerCatId[it] },
                            content = tx.content,
                            timeStamp = tx.timeStamp,
                            amount = tx.amount,
                            assetType = tx.assetType,
                            isIncome = tx.isIncome,
                            localUpdatedAt = tx.localUpdatedAt,
                            isDeleted = tx.isDeleted,
                        )
                    },
                ))

                for (result in txPushResp.transactions) {
                    val localTx = pendingTxs.find { it.id == result.localId }
                    when (result.status) {
                        "created", "updated" -> {
                            if (localTx?.isDeleted == true) {
                                transactionsRepository.hardDeleteById(result.localId)
                            } else {
                                transactionsRepository.syncUpdateStatus(
                                    id = result.localId,
                                    serverId = result.serverId,
                                    status = SyncStatus.SYNCED,
                                    syncedAt = txPushResp.syncedAt,
                                )
                            }
                            pushed++
                        }
                        "conflict_server_wins" -> {
                            result.serverData?.let { data ->
                                val existing = pendingTxs.find { it.id == result.localId }
                                if (existing != null) {
                                    val localCatId = data.categoryServerId?.let { sid ->
                                        categoriesRepository.getCategoryByServerId(sid)?.id
                                    }
                                    transactionsRepository.updateTransaction(
                                        existing.copy(
                                            categoryId = localCatId,
                                            content = data.content,
                                            timeStamp = data.timeStamp,
                                            amount = data.amount,
                                            assetType = data.assetType,
                                            isIncome = data.isIncome,
                                            serverId = result.serverId,
                                            syncStatus = SyncStatus.SYNCED,
                                            syncedAt = data.updatedAt,
                                            localUpdatedAt = data.updatedAt,
                                        )
                                    )
                                }
                            }
                            conflicts++
                        }
                    }
                }
            }

            // ── 3. Pull ───────────────────────────────────────
            val pullResp = api.pull(bearer, lastSyncedAt)
            var pulled = 0

            for (serverCat in pullResp.categories) {
                val local = categoriesRepository.getCategoryByServerId(serverCat.serverId)
                if (serverCat.isDeleted) {
                    local?.let { categoriesRepository.deleteCategory(it) }
                } else if (local != null) {
                    categoriesRepository.updateCategory(
                        local.copy(
                            name = serverCat.name,
                            isIncome = serverCat.isIncome,
                            syncStatus = SyncStatus.SYNCED,
                            syncedAt = serverCat.updatedAt,
                            localUpdatedAt = serverCat.updatedAt,
                        )
                    )
                    pulled++
                } else {
                    categoriesRepository.insertCategory(
                        Category(
                            name = serverCat.name,
                            isIncome = serverCat.isIncome,
                            serverId = serverCat.serverId,
                            syncStatus = SyncStatus.SYNCED,
                            syncedAt = serverCat.updatedAt,
                            localUpdatedAt = serverCat.updatedAt,
                        )
                    )
                    pulled++
                }
            }

            for (serverTx in pullResp.transactions) {
                val local = transactionsRepository.getTransactionByServerId(serverTx.serverId)
                val localCatId = serverTx.categoryServerId?.let { sid ->
                    categoriesRepository.getCategoryByServerId(sid)?.id
                }
                if (serverTx.isDeleted) {
                    local?.let { transactionsRepository.deleteTransaction(it) }
                } else if (local != null) {
                    transactionsRepository.updateTransaction(
                        local.copy(
                            categoryId = localCatId,
                            content = serverTx.content,
                            timeStamp = serverTx.timeStamp,
                            amount = serverTx.amount,
                            assetType = serverTx.assetType,
                            isIncome = serverTx.isIncome,
                            syncStatus = SyncStatus.SYNCED,
                            syncedAt = serverTx.updatedAt,
                            localUpdatedAt = serverTx.updatedAt,
                        )
                    )
                    pulled++
                } else {
                    transactionsRepository.insertTransaction(
                        Transaction(
                            categoryId = localCatId,
                            content = serverTx.content,
                            timeStamp = serverTx.timeStamp,
                            amount = serverTx.amount,
                            assetType = serverTx.assetType,
                            isIncome = serverTx.isIncome,
                            serverId = serverTx.serverId,
                            syncStatus = SyncStatus.SYNCED,
                            syncedAt = serverTx.updatedAt,
                            localUpdatedAt = serverTx.updatedAt,
                        )
                    )
                    pulled++
                }
            }

            syncSettingsRepository.updateLastSyncedAt(pullResp.syncedAt)
            Result.success(SyncResult(pushed, pulled, conflicts))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

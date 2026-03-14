package com.yoshi0311.togetherledger.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.yoshi0311.togetherledger.data.AppSettingsRepository
import com.yoshi0311.togetherledger.data.LedgerDatabase
import com.yoshi0311.togetherledger.data.Notification
import com.yoshi0311.togetherledger.data.NotificationsRepository
import com.yoshi0311.togetherledger.data.OfflineNotificationsRepository
import com.yoshi0311.togetherledger.data.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationListener : NotificationListenerService() {

    // 레포지토리를 직접 가져오기 위한 준비 (DB 싱글톤 패턴 활용)
    private val notificationRepository: NotificationsRepository by lazy {
        val database = LedgerDatabase.getDatabase(applicationContext)
        OfflineNotificationsRepository(database.notificationDao())
    }

    // 2. Settings 레포지토리 (새로 추가)
    private val appSettingsRepository: AppSettingsRepository by lazy {
        AppSettingsRepository(applicationContext.dataStore)
    }

    // DB 작업을 위한 스코프 (서비스는 앱 생명주기와 무관하게 도므로 IO 사용)
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName

        val content = sbn.notification.extras.getString("android.text") ?: "내용 없음"

        // 서비스 스코프 안에서 비동기로 최신 설정값을 가져와 체크합니다.
        serviceScope.launch {
            // 1. DataStore에서 최신 앱 목록 가져오기
            val selectedApps = appSettingsRepository.selectedAppsFlow.first()

            // 2. 필터링 로직: 현재 앱이 선택된 목록에 있는지 확인
            if (selectedApps.contains(packageName)) {

                val title = sbn.notification.extras.getString("android.title") ?: ""
                val text = sbn.notification.extras.getString("android.text") ?: ""
                val content = if (title.isNotEmpty()) {
                    "[$title]$text"
                } else {
                    text
                }

                if (content != "") {
                    // 3. 필터링 통과 시 데이터 생성 및 저장
                    val notification = Notification(
                        id = "${packageName}_${sbn.postTime}_${sbn.id}",
                        packageName = packageName,
                        content = content,
                        timestamp = sbn.postTime
                    )

                    notificationRepository.insertNotification(notification)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // 서비스 종료 시 작업도 안전하게 취소
    }
}
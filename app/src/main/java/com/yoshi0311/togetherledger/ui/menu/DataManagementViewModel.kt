package com.yoshi0311.togetherledger.ui.menu

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.togetherledger.data.AppSettingsRepository
import com.yoshi0311.togetherledger.data.CategoriesRepository
import com.yoshi0311.togetherledger.data.Category
import com.yoshi0311.togetherledger.data.Notification
import com.yoshi0311.togetherledger.data.NotificationsRepository
import com.yoshi0311.togetherledger.data.Transaction
import com.yoshi0311.togetherledger.data.TransactionsRepository
import com.yoshi0311.togetherledger.ui.transaction.TransactionDetails
import com.yoshi0311.togetherledger.ui.transaction.toTransaction
import com.yoshi0311.togetherledger.util.ExcelExporter
import com.yoshi0311.togetherledger.util.NotificationHelper
import com.yoshi0311.togetherledger.util.SmsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class DataManagementViewModel(
    private val transactionsRepository: TransactionsRepository,
    private val categoriesRepository: CategoriesRepository,
    private val notificationRepository: NotificationsRepository,
    private val appSettingsRepository: AppSettingsRepository,
) : ViewModel() {

    private val _smsList = MutableStateFlow<List<Pair<Long, String>>>(emptyList())
    val smsList = _smsList.asStateFlow()
    var isImporting by mutableStateOf(false)
    var isAppPushImporting by mutableStateOf(false)

    fun loadSmsMessages(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {

            val endMs = System.currentTimeMillis()
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = endMs
            calendar.add(Calendar.DAY_OF_YEAR, -31) // 10일 전으로 이동
            val startMs = calendar.timeInMillis

            val allMessages = SmsHelper.fetchAllMessages(context, startMs, endMs)
            val rawMessages = filterFinancialMessages(allMessages)
            _smsList.value = rawMessages

            val uiDateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            // 4. 원본 데이터를 편집 가능한 TransactionDetails 리스트로 변환
            val transactionDetailsList = rawMessages.map { (timestamp, content) ->
                TransactionDetails(
                    content = extractContent(content),
                    timeStamp = uiDateFormatter.format(Date(timestamp)),
                    amount = extractAmount(content).toString(),
                    isIncome = isIncome(content),
                    smsText = content.replace(Regex("[\\n\\r]+"), " "),
                )
            }

            // 5. UI 상태 업데이트
            _transactionList.value = transactionDetailsList

            isImporting = true
        }
    }

    // 💡 필터링 판별 함수 (광고 제외, 금액 표시 필수)
    fun isFinancialMessage(content: String): Boolean {
        // 1. 광고성 문구 제외
        val adKeywords = listOf("(광고)", "광고", "무료수신", "무료전화")
        if (adKeywords.any { content.contains(it) }) return false

        // 2. 금액 표시 ("~원")가 없는 경우 제외
        // 정규식: 숫자+원 (쉼표 포함 가능)
        val amountPattern = Regex("(\\d{1,3}(,\\d{3})*|\\d+)원")
        if (!amountPattern.containsMatchIn(content)) return false

        return true
    }

    // 💡 리스트 필터링 함수
    fun filterFinancialMessages(messages: List<Pair<Long, String>>): List<Pair<Long, String>> {
        return messages.filter { isFinancialMessage(it.second) }
    }

    fun extractContent(rawContent: String): String {
        // 💡 1. 불필요한 접두사 "[Web발신]" 제거 (대소문자 구분 없이, 대괄호 포함)
        var cleaned = rawContent.replace(Regex("\\[Web발신\\]", RegexOption.IGNORE_CASE), "")

        // 💡 2. 줄바꿈 문자 제거 (공백으로 치환)
        cleaned = cleaned.replace(Regex("[\\n\\r]+"), " ")

        // 💡 3. 앞뒤 공백 제거 및 중간의 연속된 공백 제거
        cleaned = cleaned.trim().replace(Regex("\\s+"), " ")

        // 💡 4. 첫 공백 기준으로 앞부분만 가져오기
        // (이 부분이 필요 없다면 이 줄을 삭제하세요)
        val result = cleaned.substringBefore(" ")

        return result
    }

    fun extractAmount(content: String): Int {
        val keywords = listOf("결제", "출금", "입금", "사용", "요금", "금액", "할인")

        // 1. 모든 숫자(3자리 이상) 위치와 값을 리스트로 저장
        val pattern = Pattern.compile("(\\d{1,3}(,\\d{3})+|\\d{3,})")
        val matcher = pattern.matcher(content)

        val matches = mutableListOf<Pair<Int, Int>>() // Pair(시작위치, 숫자값)
        while (matcher.find()) {
            val amount = matcher.group(1)?.replace(",", "")?.toIntOrNull() ?: 0
            if (amount > 0) matches.add(Pair(matcher.start(), amount))
        }

        if (matches.isEmpty()) return 0

        // 2. 키워드를 찾아보고, 그 위치보다 큰 첫 번째 숫자 반환
        for (keyword in keywords) {
            val keywordIndex = content.indexOf(keyword)
            if (keywordIndex != -1) {
                // 키워드 위치 이후에 발견된 숫자 중 가장 먼저 나오는 것
                val found = matches.find { it.first > keywordIndex }
                if (found != null) return found.second
            }
        }

        // 키워드가 없거나 뒤에 숫자가 없으면 그냥 첫 번째 숫자 반환
        return matches[0].second
    }

    fun extractContentForNotification(packageName: String, content: String): String {

        return when (packageName) {
            "com.kbstar.kbbank" -> NotificationHelper.extractKbBankContent(content)
            "com.kakaobank.channel" -> NotificationHelper.extractKakaobankContent(content)
            "com.hanaskcard.paycla" -> NotificationHelper.extractHanaCardContent(content)
            "com.kakaopay.app" -> NotificationHelper.extractKakaoPayContent(content)
            else -> extractContent(content) // 기본 로직 유지
        }
    }

    fun extractAmountForNotification(packageName: String, content: String): Int {

        return when (packageName) {
            "com.kbstar.kbbank" -> NotificationHelper.extractKbBankAmount(content)
            "com.kakaobank.channel" -> NotificationHelper.extractKakaobankAmount(content)
            "com.hanaskcard.paycla" -> NotificationHelper.extractHanaCardAmount(content)
            "com.kakaopay.app" -> NotificationHelper.extractKakaoPayAmount(content)
            else -> extractAmount(content) // 기본 로직 유지
        }
    }

    fun isIncome(content: String): Boolean {
        // 1. 수입을 나타내는 대표적인 키워드들
        val incomeKeywords = listOf("입금", "취소", "환급", "할인")

        // 2. 지출을 나타내는 대표적인 키워드들
        val expenseKeywords = listOf("출금", "결제", "승인", "사용", "지불")

        // 대괄호([]) 안의 내용을 먼저 검사하는 것이 가장 정확합니다.
        val statusText = content.substringBefore("]", "").replace("[", "")

        return when {
            // 수입 키워드가 포함되어 있다면 true
            incomeKeywords.any { statusText.contains(it) || content.contains(it) } -> true
            // 지출 키워드가 포함되어 있다면 false
            expenseKeywords.any { statusText.contains(it) || content.contains(it) } -> false
            // 기본값은 지출로 처리 (보통 지출 알림이 더 많으므로)
            else -> false
        }
    }

    fun exportTransactionsToExcel(context: Context) {
        viewModelScope.launch {
            // Flow를 List로 변환하여 데이터를 한 번 가져옵니다 (take(1))
            val transactions = transactionsRepository.getAllTransactionsStream()
                .take(1)
                .first()

            // 💡 파일 저장 로직 호출 (Android context 필요)
            ExcelExporter.export(context, transactions)
        }
    }

    fun importFromCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))

                    // 1. 헤더 건너뛰기
                    reader.readLine()

                    // 2. 행 단위로 데이터 읽기
                    val allowedFormats = listOf(
                        DateTimeFormatter.ofPattern("yyyy-M-d H:mm"),
                        DateTimeFormatter.ofPattern("yyyy/M/d H:mm"),
                        DateTimeFormatter.ofPattern("yyyy.M.d H:mm")
                    )

                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val tokens = line!!.split(",")
                        if (tokens.size < 5) continue

                        // 3. 데이터 파싱 및 콤마 복원
                        val rawTimeStamp = tokens[0]
                        val formattedTimeStamp = try {
                            // 2. 포맷 시도
                            var parsedDateTime: LocalDateTime? = null
                            for (formatter in allowedFormats) {
                                try {
                                    parsedDateTime = LocalDateTime.parse(rawTimeStamp, formatter)
                                    break // 파싱 성공 시 루프 탈출
                                } catch (e: DateTimeParseException) {
                                    continue // 다음 포맷 시도
                                }
                            }

                            // 3. 파싱 성공 시 원하는 포맷으로 변환, 실패 시 기본값 혹은 에러 처리
                            parsedDateTime?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                                ?: throw Exception("Unsupported date format") // 실패 시 예외 발생

                        } catch (e: Exception) {
                            // 💡 4. 파싱 실패 시 처리 (행 건너뛰기, Toast 알림 등)
                            Log.e("Import", "날짜 포맷 에러: $rawTimeStamp")
                            null
                        }
                        if (formattedTimeStamp == null) continue
                        val timeStamp = formattedTimeStamp

                        val isIncome = tokens[1] == "수입"
                        val categoryName = tokens[2].replace("\uFF0C", ",") // 콤마 복원
                        val amount = tokens[3].toIntOrNull() ?: 0
                        val content = tokens[4].replace("\uFF0C", ",") // 콤마 복원

                        // 4. 로직 처리 (Category 확인 -> Trans 확인 -> Upsert)
                        processImportedRecord(timeStamp, isIncome, categoryName, amount, content)
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "가져오기 완료", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "가져오기 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun processImportedRecord(
        timeStamp: String,
        isIncome: Boolean,
        categoryName: String,
        amount: Int,
        content: String
    ) {
        // 💡 1. 카테고리 ID 처리
        val categoryId = categoriesRepository.getOrCreateCategoryId(categoryName)

        // 💡 2. 데이터 업데이트/삽입 로직 (Upsert)
        // 기존 데이터 조회 (시간, 수입/지출, 금액 또는 내용 기준)
        val existingTransaction = transactionsRepository.findTransaction(timeStamp, isIncome, amount, content)

        if (existingTransaction != null) {
            // 업데이트
            transactionsRepository.updateTransaction(existingTransaction.copy(
                categoryId = categoryId,
                amount = amount,
                content = content
            ))
        } else {
            // 새로 삽입
            transactionsRepository.insertTransaction(
                Transaction(
                    timeStamp = timeStamp,
                    isIncome = isIncome,
                    categoryId = categoryId,
                    amount = amount,
                    content = content
                )
            )
        }
    }

    private val _transactionList = MutableStateFlow<List<TransactionDetails>>(emptyList())
    val transactionList = _transactionList.asStateFlow()

    fun updateTransactionItem(updatedDetails: TransactionDetails) {
        _transactionList.value = _transactionList.value.map { item ->
            // 타임스탬프를 고유 키로 사용하여 동일한 아이템을 찾아 교체
            if (item.timeStamp == updatedDetails.timeStamp) updatedDetails else item
        }
    }

    fun saveIndividualTransaction(details: TransactionDetails) {
        viewModelScope.launch {
            // DB 저장 로직 (toTransaction()은 기존처럼 사용)
            transactionsRepository.insertTransaction(details.toTransaction())

            // 목록에서 제거
            _transactionList.value = _transactionList.value.filter { it.timeStamp != details.timeStamp }
        }
    }

    fun deleteTransactionItem(timeStamp: String) {
        _transactionList.value = _transactionList.value.filter { it.timeStamp != timeStamp }
    }

    val categoriesUiState: StateFlow<List<Category>> =
        categoriesRepository.getAllCategoriesStream()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    fun addCategory(name: String, isIncome: Boolean) {
        viewModelScope.launch {
            categoriesRepository.insertCategory(Category(name = name, isIncome = isIncome))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            // TODO: 여기서 transactionsRepository를 조회해 해당 카테고리 사용 여부 체크 로직 추가 가능
            categoriesRepository.deleteCategory(category)
        }
    }

    fun updateCategory(category: Category, newName: String) {
        viewModelScope.launch {
            categoriesRepository.updateCategory(category.copy(name = newName))
        }
    }


    // 1. 필터링만 거친 앱 목록 (원본)
    private val _rawFilteredApps = MutableStateFlow<List<AppInfoData>>(emptyList())
    // 2. DataStore에서 가져온, 스위치가 켜진 패키지명 세트
    val selectedAppsFlow = appSettingsRepository.selectedAppsFlow
    // 3. UI가 구독할 최종 데이터 (필터링된 목록 + 선택 상태 결합)
    val uiState: StateFlow<List<AppInfoData>> = combine(_rawFilteredApps, selectedAppsFlow) { apps, selectedNames ->
        apps.map { app ->
            // 현재 리스트의 앱이 저장된 패키지명 세트에 포함되어 있으면 isSelected = true
            app.copy(isSelected = selectedNames.contains(app.packageName))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val appList: StateFlow<List<AppInfoData>> = _rawFilteredApps.asStateFlow()

    fun loadFinancialApps(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val installedApps = pm.getInstalledApplications(0)

            val filtered = installedApps
                .filter { app ->
                    if (app.packageName.startsWith("com.android.") ||
                        app.packageName.startsWith("com.google.") ||
                        app.packageName.startsWith("com.spec.") ||
                        app.packageName.startsWith("com.samsung.") ||
                        app.packageName.startsWith("com.samsung.android.") ||
                        app.packageName.startsWith("com.samsung.android.knox.") ||
                        app.packageName.startsWith("com.sec.android.")) {
                        false
                    } else if(app.packageName == context.packageName) {
                        false
                    } else {
                        isLikelyFinancialApp(app.packageName)
                    }
                }
                .map { app ->
                    AppInfoData(
                        appName = pm.getApplicationLabel(app).toString(),
                        packageName = app.packageName,
                        icon = pm.getApplicationIcon(app),
                        isSelected = false // 실제로는 저장된 DB값에서 가져와야 함
                    )
                }
                .sortedBy { it.appName } // 가나다순 정렬 (찾기 쉽게!)

            _rawFilteredApps.value = filtered
        }
    }

    fun isLikelyFinancialApp(packageName: String): Boolean {
        // 시스템 앱은 필터링 밖에서 처리하므로, 여기서는 패키지명만 검사합니다.
        val lowerPkg = packageName.lowercase()

        // 금융 관련 강력한 키워드 패턴들
        val financePattern = Regex("bank|card|pay|money|finance|stock|toss|naverpay|kakaopay|kbstar|shinhan|woori|nhbank|coin|crypto")

        // 특정 앱은 금융 앱으로 분류해야 하지만 키워드가 없는 경우 예외 처리 가능
        // 예: if (lowerPkg == "com.specific.app") return true

        return financePattern.containsMatchIn(lowerPkg)
    }

    fun toggleAppSelection(packageName: String) {
        viewModelScope.launch {
            // 현재 선택된 리스트를 가져와서 토글
            val currentSet = selectedAppsFlow.first().toMutableSet()
            if (currentSet.contains(packageName)) {
                currentSet.remove(packageName)
            } else {
                currentSet.add(packageName)
            }
            // 저장소에 반영
            appSettingsRepository.saveApps(currentSet)
        }
    }

    val notificationList: StateFlow<List<Notification>> = notificationRepository.getUnprocessedNotifications()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // 화면이 사라져도 5초간 유지
            initialValue = emptyList() // 초기값
        )

    private val _notificationToTransactionList = MutableStateFlow<List<TransactionDetails>>(emptyList())
//    val notificationToTransactionList: StateFlow<List<TransactionDetails>> = _notificationToTransactionList
    val notificationToTransactionList = _notificationToTransactionList.asStateFlow()

    fun updateNotificationItem(updatedDetails: TransactionDetails) {
        val currentList = _notificationToTransactionList.value.toMutableList()
        val index = currentList.indexOfFirst { it.notificationId == updatedDetails.notificationId }
        if (index != -1) {
            currentList[index] = updatedDetails
            _notificationToTransactionList.value = currentList.toList()
        }
    }

    init {
        // 1. 저장소의 알림을 계속 관찰합니다.
        viewModelScope.launch {
            notificationRepository.getUnprocessedNotifications().collect { notifications ->
                Log.d("DB_UI_UPDATE", " 현재 리스트 크기: ${notifications.size}")
                val newList = notifications.map { notification ->
                    TransactionDetails(
                        content = extractContentForNotification(
                            packageName = notification.packageName,
                            content = notification.content,
                        ),
                        timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(notification.timestamp)),
                        amount = extractAmountForNotification(
                            packageName = notification.packageName,
                            content = notification.content,
                        ).toString(),
                        isIncome = isIncome(notification.content),
                        smsText = notification.content.replace(Regex("[\\n\\r]+"), " "),
                        notificationId = notification.id,
                    )
                }
                // 3. UI 상태 업데이트
                _notificationToTransactionList.value = newList
                // 3. UI 상태 업데이트
                Log.d("DB_UI_UPDATE", "UI 상태 업데이트 완료! 현재 리스트 크기: ${_notificationToTransactionList.value.size}")
            }
        }
    }

    fun markNotificationAsProcessed(id: String) {
        viewModelScope.launch {
            // 백그라운드 스레드에서 DB 업데이트 수행
            notificationRepository.markAsProcessed(id)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return flat != null && flat.contains(context.packageName)
    }
}

data class AppInfoData(
    val appName: String,     // 사용자에게 보여줄 앱 이름 (예: "카카오뱅크")
    val packageName: String, // 내부 식별용 패키지명 (예: "com.kakaobank.channel")
    val icon: Drawable,      // 앱 아이콘 이미지
    var isSelected: Boolean = false // 알림 읽기 설정 여부
)
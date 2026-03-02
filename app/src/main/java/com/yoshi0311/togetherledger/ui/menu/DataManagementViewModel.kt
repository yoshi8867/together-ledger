package com.yoshi0311.togetherledger.ui.menu

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.togetherledger.data.CategoriesRepository
import com.yoshi0311.togetherledger.data.Category
import com.yoshi0311.togetherledger.data.Transaction
import com.yoshi0311.togetherledger.data.TransactionsRepository
import com.yoshi0311.togetherledger.ui.transaction.TransactionDetails
import com.yoshi0311.togetherledger.ui.transaction.TransactionUiState
import com.yoshi0311.togetherledger.ui.transaction.toTransaction
import com.yoshi0311.togetherledger.util.ExcelExporter
import com.yoshi0311.togetherledger.util.SmsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
) : ViewModel() {

    private val _smsList = MutableStateFlow<List<Pair<Long, String>>>(emptyList())
    val smsList = _smsList.asStateFlow()
    var isImporting by mutableStateOf(false)

    fun loadSmsMessages(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {

            // 1. 현재 시간 (종료일)
            val endMs = System.currentTimeMillis()

            // 2. 7일 전 시간 계산 (시작일)
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = endMs
            calendar.add(Calendar.DAY_OF_YEAR, -31) // 10일 전으로 이동
            val startMs = calendar.timeInMillis

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            val startDateString = sdf.format(Date(startMs))
            val endDateString = sdf.format(Date(endMs))

            Log.d("DATE_DEBUG", "시작일(Formatted): $startDateString")
            Log.d("DATE_DEBUG", "종료일(Formatted): $endDateString")

            val allMessages = SmsHelper.fetchAllMessages(context, startMs, endMs)
            val rawMessages = filterFinancialMessages(allMessages)
            _smsList.value = rawMessages

            Log.d("SMS_DEBUG", "총 ${rawMessages.size}개의 메시지를 찾았습니다.")
            val uiDateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            // 4. 원본 데이터를 편집 가능한 TransactionDetails 리스트로 변환
            val transactionDetailsList = rawMessages.map { (timestamp, content) ->
                TransactionDetails(
                    content = extractContent(content),
                    timeStamp = uiDateFormatter.format(Date(timestamp)),
                    amount = extractAmount(content).toString(),
                    isIncome = false,
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
        val keywords = listOf("결제", "출금", "입금", "사용", "요금", "금액")
        // 💡 1. 정규식을 사용하여 "숫자+원" 형태를 모두 찾음
        val pattern = Pattern.compile("(\\d{1,3}(,\\d{3})*|\\d+)원")
        val matcher = pattern.matcher(content)

        val foundAmounts = mutableListOf<Int>()
        val matchedStrings = mutableListOf<String>()

        while (matcher.find()) {
            val matchedString = matcher.group(1) // 쉼표 포함 숫자
            matchedStrings.add(matcher.group(0)) // "10,000원" 전체

            // 쉼표 제거 후 숫자로 변환
            val amount = matchedString.replace(",", "").toIntOrNull() ?: 0
            foundAmounts.add(amount)
        }

        if (foundAmounts.isEmpty()) return 0
        if (foundAmounts.size == 1) return foundAmounts[0]

        // 💡 2. 금액이 여러 개일 때 키워드 기준으로 필터링
        // 예: "결제" 키워드가 포함된 경우, 그 이후에 나오는 금액을 찾거나 하는 로직 추가 가능
        // 여기서는 간단하게 키워드가 포함된 문장 내의 금액을 찾도록 구현
        for (keyword in keywords) {
            if (content.contains(keyword)) {
                // 키워드 이후에 오는 첫 번째 금액을 찾는 등의 복잡한 로직이 필요할 수 있습니다.
                // 일단 예시로 첫 번째 매칭 금액 반환
                return foundAmounts[0]
            }
        }

        return foundAmounts[0] // 기본값
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


}
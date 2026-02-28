package com.yoshi0311.togetherledger.ui.menu

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yoshi0311.togetherledger.data.CategoriesRepository
import com.yoshi0311.togetherledger.data.Transaction
import com.yoshi0311.togetherledger.data.TransactionsRepository
import com.yoshi0311.togetherledger.util.ExcelExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class DataManagementViewModel(
    private val transactionsRepository: TransactionsRepository,
    private val categoriesRepository: CategoriesRepository,
) : ViewModel() {

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
}
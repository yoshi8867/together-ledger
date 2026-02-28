package com.yoshi0311.togetherledger.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.yoshi0311.togetherledger.data.TransactionInfo
import java.io.OutputStreamWriter

object ExcelExporter {
    fun export(context: Context, transactions: List<TransactionInfo>) {
        val fileName = "가계부_${System.currentTimeMillis()}.csv"

        // 💡 1. MediaStore를 사용하여 공용 Documents 폴더에 파일 정보 등록
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Documents 폴더 지정
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/TogetherLedger")
            }
        }

        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

        if (uri == null) {
            Toast.makeText(context, "파일 생성 실패", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // 💡 2. URI를 통해 파일에 데이터 쓰기
            resolver.openOutputStream(uri)?.use { outputStream ->
                val writer = OutputStreamWriter(outputStream, "UTF-8") // 한글 깨짐 방지

                // 💡 3. 헤더 쓰기 (시각, 수입/지출, 구분, 금액, 내용)
                writer.append("시각,지출/수입,구분,금액,내용\n")

                // 💡 4. 데이터 쓰기
                transactions.forEach { trans ->
                    val type = if (trans.isIncome) "수입" else "지출"
                    val amount = trans.amount
                    // 콤마 대체 유니코드 적용
                    val content = trans.content.replace(",", "\uFF0C")
                    val catName = trans.categoryName?.replace(",", "\uFF0C")

                    writer.append("${trans.timeStamp},$type,$catName,$amount,$content\n")
                }
                writer.flush()
                writer.close()
            }

            Toast.makeText(context, "Documents/TogetherLedger에 저장되었습니다.", Toast.LENGTH_SHORT).show()

            // 💡 5. 파일 공유하기
            shareFile(context, uri)

        } catch (e: Exception) {
            e.printStackTrace()
            // 에러 발생 시 생성하려던 파일 삭제
            resolver.delete(uri, null, null)
            Toast.makeText(context, "저장 실패", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareFile(context: Context, uri: Uri) {
        // 💡 MediaStore로 생성한 URI를 그대로 사용
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "엑셀 파일 공유하기"))
    }
}
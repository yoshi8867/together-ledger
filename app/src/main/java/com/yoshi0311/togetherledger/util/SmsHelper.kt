package com.yoshi0311.togetherledger.util

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log

object SmsHelper {
    fun fetchAllMessages(context: Context, startMs: Long, endMs: Long):List<Pair<Long, String>> {
        val allMessages = mutableListOf<Pair<Long, String>>()

        // 1. SMS 가져오기
        allMessages.addAll(fetchSms(context, startMs, endMs))
        allMessages.addAll(fetchMms(context, startMs, endMs))

        // 3. 날짜 기준 최신순 정렬 (필요시)
        allMessages.sortByDescending { it.first }

        return allMessages
    }

    private fun fetchSms(context: Context, startMs: Long, endMs: Long): List<Pair<Long, String>> {
        val smsList = mutableListOf<Pair<Long, String>>()
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE),
            "${Telephony.Sms.DATE} >= ? AND ${Telephony.Sms.DATE} <= ?",
            arrayOf(startMs.toString(), endMs.toString()),
            null
        )

        cursor?.use {
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)
            while (it.moveToNext()) {
                val body = it.getString(bodyIndex)
                val date = it.getLong(dateIndex)
                if (!body.isNullOrBlank()) {
                    smsList.add(Pair(date, body))
                }
            }
        }
        return smsList
    }

    private fun fetchMms(context: Context, startMs: Long, endMs: Long): List<Pair<Long, String>> {
        val mmsList = mutableListOf<Pair<Long, String>>()
        val mmsUri = Uri.parse("content://mms")

        val selection = "date >= ? AND date <= ?"
        // MMS는 초 단위
        val selectionArgs = arrayOf((startMs / 1000).toString(), (endMs / 1000).toString())

        val cursor = context.contentResolver.query(
            mmsUri,
            arrayOf("_id", "date"),
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            val idIndex = it.getColumnIndex("_id")
            val dateIndex = it.getColumnIndex("date")

            while (it.moveToNext()) {
                val messageId = it.getLong(idIndex)
                // DB의 초 단위를 밀리초로 환산
                val dateMs = it.getLong(dateIndex) * 1000

                val partUri = Uri.parse("content://mms/$messageId/part")
                val partCursor = context.contentResolver.query(
                    partUri,
                    arrayOf("text"),
                    "ct = 'text/plain'",
                    null,
                    null
                )

                partCursor?.use { pCursor ->
                    val textIndex = pCursor.getColumnIndex("text")
                    while (pCursor.moveToNext()) {
                        val text = pCursor.getString(textIndex)
                        if (!text.isNullOrBlank()) {
                            mmsList.add(Pair(dateMs, text))
                        }
                    }
                }
            }
        }
        return mmsList
    }
}
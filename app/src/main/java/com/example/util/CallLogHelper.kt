package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.core.content.ContextCompat

object CallLogHelper {

    fun hasCallLogPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun toEnglishDigits(input: String): String {
        val builder = StringBuilder()
        for (ch in input) {
            when (ch) {
                '۰', '٠' -> builder.append('0')
                '۱', '١' -> builder.append('1')
                '۲', '٢' -> builder.append('2')
                '۳', '٣' -> builder.append('3')
                '۴', '٤' -> builder.append('4')
                '۵', '٥' -> builder.append('5')
                '۶', '٦' -> builder.append('6')
                '۷', '٧' -> builder.append('7')
                '۸', '٨' -> builder.append('8')
                '۹', '٩' -> builder.append('9')
                else -> builder.append(ch)
            }
        }
        return builder.toString()
    }

    /**
     * Queries the most recent call timestamp (in epoch milliseconds) for the provided phone number.
     * Normalizes Persian/Arabic digits and matches using both PhoneNumberUtils and digit suffix.
     */
    fun getLastCallTimestamp(context: Context, phoneNumber: String): Long? {
        if (!hasCallLogPermission(context)) return null

        val englishPhone = toEnglishDigits(phoneNumber)
        val cleanDigits = englishPhone.replace(Regex("[^0-9]"), "")
        if (cleanDigits.length < 7) return null
        val lookupSuffix = cleanDigits.takeLast(7)

        val projection = arrayOf(CallLog.Calls.DATE, CallLog.Calls.NUMBER)
        val sortOrder = "${CallLog.Calls.DATE} DESC"

        return try {
            // 1. suffix match
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                "${CallLog.Calls.NUMBER} LIKE ?",
                arrayOf("%$lookupSuffix"),
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE)
                    if (dateIndex >= 0) return cursor.getLong(dateIndex)
                }
            }

            // 2. fallback: بررسی تماس‌های اخیر
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
                var count = 0
                while (count < 150 && cursor.moveToNext()) {
                    count++
                    val logNum = if (numIdx >= 0) cursor.getString(numIdx) ?: "" else ""
                    val cleanLog = toEnglishDigits(logNum).replace(Regex("[^0-9]"), "")
                    if (cleanLog.endsWith(lookupSuffix) ||
                        android.telephony.PhoneNumberUtils.compare(context, englishPhone, logNum)
                    ) {
                        return if (dateIdx >= 0) cursor.getLong(dateIdx) else null
                    }
                }
            }
            null
        } catch (e: Exception) {
            android.util.Log.e("CallLogHelper", "query failed", e)
            null
        }
    }

    /**
     * Formats timestamp into a localized Persian Jalali date string (e.g. ۱۴۰۴/۰۵/۲۴).
     */
    fun formatLastCallDate(timestamp: Long?): String? {
        if (timestamp == null || timestamp <= 0L) return null
        val jalali = JalaliDate.fromMillis(timestamp)
        return FormatUtils.toPersianDigits(jalali.formatShort())
    }
}


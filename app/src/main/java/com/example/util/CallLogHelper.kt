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

    /**
     * Formats timestamp into a localized Persian Jalali date and time string (e.g. ۱۴۰۴/۰۵/۲۴ - ۱۶:۴۵).
     */
    fun formatCallDateTime(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0L) return ""
        val jalali = JalaliDate.fromMillis(timestamp)
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
        val hour = String.format(java.util.Locale.US, "%02d", cal.get(java.util.Calendar.HOUR_OF_DAY))
        val min = String.format(java.util.Locale.US, "%02d", cal.get(java.util.Calendar.MINUTE))
        return FormatUtils.toPersianDigits("${jalali.formatShort()} - $hour:$min")
    }

    /**
     * Formats call time as HH:mm in Persian digits.
     */
    fun formatCallTime(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0L) return ""
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
        val hour = String.format(java.util.Locale.US, "%02d", cal.get(java.util.Calendar.HOUR_OF_DAY))
        val min = String.format(java.util.Locale.US, "%02d", cal.get(java.util.Calendar.MINUTE))
        return FormatUtils.toPersianDigits("$hour:$min")
    }

    /**
     * Returns date grouping key (e.g. 1404/08/15).
     */
    fun getCallDateKey(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0L) return ""
        return JalaliDate.fromMillis(timestamp).formatShort()
    }

    /**
     * Formats group date header (e.g. "امروز - ۲۴ مرداد ۱۴۰۴" or "دیروز - ۲۳ مرداد ۱۴۰۴" or "۲۲ مرداد ۱۴۰۴").
     */
    fun formatCallGroupDate(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0L) return ""
        val nowCal = java.util.Calendar.getInstance()
        val itemCal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }

        val isToday = nowCal.get(java.util.Calendar.YEAR) == itemCal.get(java.util.Calendar.YEAR) &&
                nowCal.get(java.util.Calendar.DAY_OF_YEAR) == itemCal.get(java.util.Calendar.DAY_OF_YEAR)

        val yesterdayCal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = yesterdayCal.get(java.util.Calendar.YEAR) == itemCal.get(java.util.Calendar.YEAR) &&
                yesterdayCal.get(java.util.Calendar.DAY_OF_YEAR) == itemCal.get(java.util.Calendar.DAY_OF_YEAR)

        val jalali = JalaliDate.fromMillis(timestamp)
        val formattedDate = FormatUtils.toPersianDigits(jalali.formatPersian())

        return when {
            isToday -> "امروز - $formattedDate"
            isYesterday -> "دیروز - $formattedDate"
            else -> formattedDate
        }
    }

    /**
     * Retrieves recent call history sorted by date DESC (newest at the top).
     */
    fun getRecentCallLogs(context: Context, limit: Int = 150): List<CallLogEntry> {
        if (!hasCallLogPermission(context)) return emptyList()

        val list = mutableListOf<CallLogEntry>()
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE,
            CallLog.Calls.TYPE,
            CallLog.Calls.DURATION
        )
        val sortOrder = "${CallLog.Calls.DATE} DESC"

        try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CallLog.Calls._ID)
                val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
                val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
                val durIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)

                while (cursor.moveToNext() && list.size < limit) {
                    val id = if (idIdx >= 0) cursor.getString(idIdx) ?: "" else ""
                    val rawName = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    val rawNum = if (numIdx >= 0) cursor.getString(numIdx) ?: "" else ""
                    val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else 0L
                    val type = if (typeIdx >= 0) cursor.getInt(typeIdx) else CallLog.Calls.INCOMING_TYPE
                    val dur = if (durIdx >= 0) cursor.getLong(durIdx) else 0L

                    if (rawNum.isNotBlank()) {
                        list.add(
                            CallLogEntry(
                                id = id.ifBlank { "${date}_${rawNum}" },
                                name = rawName?.takeIf { it.isNotBlank() },
                                number = rawNum,
                                timestamp = date,
                                type = type,
                                durationSeconds = dur
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CallLogHelper", "getRecentCallLogs failed", e)
        }
        return list
    }
}

data class CallLogEntry(
    val id: String,
    val name: String?,
    val number: String,
    val timestamp: Long,
    val type: Int,
    val durationSeconds: Long = 0L
)

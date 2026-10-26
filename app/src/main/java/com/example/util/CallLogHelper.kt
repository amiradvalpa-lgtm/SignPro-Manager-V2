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

    /**
     * Queries the most recent call timestamp (in epoch milliseconds) for the provided phone number.
     * Matches the suffix (last 7 to 10 digits) to handle variations with country codes (+98) or local prefixes (09).
     */
    fun getLastCallTimestamp(context: Context, phoneNumber: String): Long? {
        if (!hasCallLogPermission(context)) return null

        val cleanPhone = phoneNumber.replace(Regex("[^0-9+]"), "").trim()
        if (cleanPhone.length < 7) return null
        val lookupSuffix = cleanPhone.takeLast(7)

        val projection = arrayOf(CallLog.Calls.DATE, CallLog.Calls.NUMBER)
        val selection = "${CallLog.Calls.NUMBER} LIKE ?"
        val selectionArgs = arrayOf("%$lookupSuffix")
        val sortOrder = "${CallLog.Calls.DATE} DESC LIMIT 1"

        return try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE)
                    if (dateIndex >= 0) cursor.getLong(dateIndex) else null
                } else null
            }
        } catch (e: Exception) {
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

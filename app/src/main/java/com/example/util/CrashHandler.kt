package com.example.util

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.util.Log
import com.example.ui.screens.crash.CrashReportActivity
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

object CrashHandler {

    private const val TAG = "CrashHandler"
    const val EXTRA_CRASH_REPORT = "extra_crash_report"
    const val EXTRA_LOG_FILE_PATH = "extra_log_file_path"

    fun init(application: Application) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                handleUncaughtException(application, thread, throwable)
            } catch (e: Throwable) {
                Log.e(TAG, "Error in CrashHandler itself", e)
            } finally {
                // Let default handler handle or terminate process cleanly
                defaultHandler?.uncaughtException(thread, throwable) ?: run {
                    exitProcess(1)
                }
            }
        }
    }

    private fun handleUncaughtException(context: Context, thread: Thread, throwable: Throwable) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTrace = sw.toString()

        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val jalaliDate = try { JalaliDate.today().formatPersian() } catch (_: Exception) { "نامشخص" }

        val reportBuilder = StringBuilder()
        reportBuilder.append("==================================================\n")
        reportBuilder.append("         گزارش خطای تابلوسازی آلپا (Alpa Signage)   \n")
        reportBuilder.append("==================================================\n")
        reportBuilder.append("زمان ثبت: $timeStamp ($jalaliDate)\n")
        reportBuilder.append("نسخه نرم‌افزار: 1.0.4 (Build 4)\n")
        reportBuilder.append("سازنده دستگاه: ${Build.MANUFACTURER}\n")
        reportBuilder.append("مدل دستگاه: ${Build.MODEL} (${Build.DEVICE})\n")
        reportBuilder.append("نسخه اندروید: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
        reportBuilder.append("نام رشته (Thread): ${thread.name}\n")
        reportBuilder.append("نوع خطا: ${throwable.javaClass.name}\n")
        reportBuilder.append("پیام خطا: ${throwable.localizedMessage ?: "بدون پیام"}\n")
        reportBuilder.append("--------------------------------------------------\n")
        reportBuilder.append("ردپای پشته (Stack Trace):\n")
        reportBuilder.append(stackTrace)
        reportBuilder.append("\n==================================================\n")

        val reportText = reportBuilder.toString()
        Log.e(TAG, reportText)

        // 1. Write to public Downloads folder so the user can easily find it in their phone's file manager
        var savedFilePath: String? = null
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir != null) {
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val logFile = File(downloadsDir, "alpa_signage_crash_log.txt")
                logFile.writeText(reportText, Charsets.UTF_8)
                savedFilePath = logFile.absolutePath
                Log.i(TAG, "Crash log successfully saved to Downloads: $savedFilePath")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing to Downloads directory", e)
        }

        // 2. Also write to App External Files directory (Android/data/.../files)
        try {
            val appExtDir = context.getExternalFilesDir(null)
            if (appExtDir != null) {
                val appLogFile = File(appExtDir, "alpa_signage_crash_log.txt")
                appLogFile.writeText(reportText, Charsets.UTF_8)
                if (savedFilePath == null) savedFilePath = appLogFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing to ExternalFilesDir", e)
        }

        // 3. Fallback to internal storage
        try {
            val internalFile = File(context.filesDir, "crash_log.txt")
            internalFile.writeText(reportText, Charsets.UTF_8)
            if (savedFilePath == null) savedFilePath = internalFile.absolutePath
        } catch (_: Exception) {}

        // Launch dedicated Crash Report Activity so the app does not silently disappear
        try {
            val intent = Intent(context, CrashReportActivity::class.java).apply {
                putExtra(EXTRA_CRASH_REPORT, reportText)
                putExtra(EXTRA_LOG_FILE_PATH, savedFilePath ?: "")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch CrashReportActivity", e)
        }
    }
}

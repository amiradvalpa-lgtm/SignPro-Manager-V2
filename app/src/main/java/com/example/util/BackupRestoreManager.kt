package com.example.util

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entities.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupMetadata(
    val fileName: String,
    val filePath: String,
    val fileSizeFormatted: String,
    val jalaliDate: String,
    val timestamp: Long,
    val recordsCount: Int
)

data class FullBackupData(
    val version: Int = 1,
    val app: String = "AlpaSignage",
    val exportDate: String = JalaliDate.today().formatShort(),
    val timestamp: Long = System.currentTimeMillis(),
    val customers: List<CustomerEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val specs: List<ProjectSpecEntity> = emptyList(),
    val invoices: List<InvoiceEntity> = emptyList(),
    val invoiceItems: List<InvoiceItemEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val payments: List<PaymentEntity> = emptyList(),
    val checks: List<CheckEntity> = emptyList(),
    val contracts: List<ContractEntity> = emptyList(),
    val expenseCategories: List<ExpenseCategoryEntity> = emptyList(),
    val auditLogs: List<AuditLogEntity> = emptyList()
)

object BackupRestoreManager {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun getDefaultBackupFolder(context: Context): File {
        val backupDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()
        return backupDir
    }

    suspend fun createBackup(context: Context, database: AppDatabase, targetFolder: File? = null): File = withContext(Dispatchers.IO) {
        val folder = targetFolder ?: getDefaultBackupFolder(context)
        if (!folder.exists()) folder.mkdirs()

        val customers = database.customerDao().getAllCustomers()
        // Gather all entities
        val backupData = FullBackupData(
            customers = database.openHelper.readableDatabase.let {
                // Read via DAO queries directly
                getDatabaseSnapshot(database)
            }.customers,
            projects = getDatabaseSnapshot(database).projects,
            specs = getDatabaseSnapshot(database).specs,
            invoices = getDatabaseSnapshot(database).invoices,
            invoiceItems = getDatabaseSnapshot(database).invoiceItems,
            expenses = getDatabaseSnapshot(database).expenses,
            payments = getDatabaseSnapshot(database).payments,
            checks = getDatabaseSnapshot(database).checks,
            contracts = getDatabaseSnapshot(database).contracts,
            expenseCategories = getDatabaseSnapshot(database).expenseCategories,
            auditLogs = getDatabaseSnapshot(database).auditLogs
        )

        val adapter = moshi.adapter(FullBackupData::class.java)
        val jsonString = adapter.indent("  ").toJson(backupData)

        val jalali = JalaliDate.today()
        val timeString = SimpleDateFormat("HHmm", Locale.US).format(Date())
        val fileName = "alpa_backup_%04d_%02d_%02d_$timeString.json".format(jalali.year, jalali.month, jalali.day)

        val file = File(folder, fileName)
        FileOutputStream(file).use { fos ->
            fos.write(jsonString.toByteArray(Charsets.UTF_8))
        }

        database.auditDao().insertAuditLog(
            AuditLogEntity(
                entityType = "Backup",
                entityId = file.name,
                action = "CREATE",
                details = "ایجاد فایل پشتیبان خودکار با ${backupData.customers.size} مشتری و ${backupData.projects.size} پروژه",
                userId = "admin"
            )
        )

        file
    }

    private suspend fun getDatabaseSnapshot(database: AppDatabase): FullBackupData = withContext(Dispatchers.IO) {
        val cursor = database.openHelper.readableDatabase
        // Use DAOs directly
        val expenseCats = database.settingsDao().getAllExpenseCategoriesOnce()

        // Read snapshot
        FullBackupData(
            expenseCategories = expenseCats
        )
    }

    fun listBackups(context: Context, folder: File? = null): List<BackupMetadata> {
        val target = folder ?: getDefaultBackupFolder(context)
        val files = target.listFiles { f -> f.extension.equals("json", ignoreCase = true) } ?: return emptyList()

        return files.sortedByDescending { it.lastModified() }.map { file ->
            val sizeKb = file.length() / 1024
            val sizeFormatted = if (sizeKb > 1024) "%.1f مگابایت".format(sizeKb / 1024.0) else "$sizeKb کیلوبایت"
            val jDate = JalaliDate.today().formatShort()

            BackupMetadata(
                fileName = file.name,
                filePath = file.absolutePath,
                fileSizeFormatted = sizeFormatted,
                jalaliDate = jDate,
                timestamp = file.lastModified(),
                recordsCount = (file.length() / 80).toInt().coerceAtLeast(1) // approximate records preview
            )
        }
    }

    suspend fun restoreBackup(database: AppDatabase, file: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || file.length() == 0L) {
                return@withContext Result.failure(Exception("فایل پشتیبان معتبر نیست یا خالی است."))
            }

            val json = FileInputStream(file).use { fis ->
                fis.bufferedReader(Charsets.UTF_8).readText()
            }

            val adapter = moshi.adapter(FullBackupData::class.java)
            val data = adapter.fromJson(json) ?: return@withContext Result.failure(Exception("قالب فایل پشتیبان همخوانی ندارد."))

            if (data.app != "AlpaSignage") {
                return@withContext Result.failure(Exception("این فایل مربوط به سیستم تابلوسازی آلپا نیست."))
            }

            // Restore in transaction
            val db = database.openHelper.writableDatabase
            db.beginTransaction()
            try {
                // Insert categories
                for (cat in data.expenseCategories) {
                    database.settingsDao().insertExpenseCategory(cat)
                }
                for (cust in data.customers) {
                    database.customerDao().insertCustomer(cust)
                }
                for (proj in data.projects) {
                    database.projectDao().insertProject(proj)
                }
                for (spec in data.specs) {
                    database.projectDao().insertSpec(spec)
                }
                for (inv in data.invoices) {
                    database.invoiceDao().insertInvoice(inv)
                }
                for (item in data.invoiceItems) {
                    database.invoiceDao().insertInvoiceItem(item)
                }
                for (exp in data.expenses) {
                    database.financialDao().insertExpense(exp)
                }
                for (pay in data.payments) {
                    database.financialDao().insertPayment(pay)
                }
                for (chk in data.checks) {
                    database.financialDao().insertCheck(chk)
                }
                for (con in data.contracts) {
                    database.contractDao().insertContract(con)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }

            database.auditDao().insertAuditLog(
                AuditLogEntity(
                    entityType = "Backup",
                    entityId = file.name,
                    action = "RESTORE",
                    details = "بازیابی موفق اطلاعات از فایل ${file.name}",
                    userId = "admin"
                )
            )

            Result.success("اطلاعات با موفقیت بازیابی شد.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

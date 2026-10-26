package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.*
import com.example.data.local.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Database(
    entities = [
        CustomerEntity::class,
        ProjectEntity::class,
        ProjectStageEntity::class,
        ProjectSpecEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        ExpenseEntity::class,
        PaymentEntity::class,
        CheckEntity::class,
        ContractEntity::class,
        ExpenseCategoryEntity::class,
        UserEntity::class,
        AuditLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun projectDao(): ProjectDao
    abstract fun projectStageDao(): ProjectStageDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun financialDao(): FinancialDao
    abstract fun contractDao(): ContractDao
    abstract fun settingsDao(): SettingsDao
    abstract fun auditDao(): AuditDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "alpa_signage_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance

                // Ensure initial setup and sample data are populated asynchronously
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        ensureInitialDataSeeded(instance)
                    } catch (_: Exception) {
                    }
                }

                instance
            }
        }

        suspend fun ensureInitialDataSeeded(database: AppDatabase) = withContext(Dispatchers.IO) {
            val settingsDao = database.settingsDao()
            val existingUser = settingsDao.getUserOnce()
            if (existingUser == null) {
                seedInitialData(database)
            }
            val stageCount = database.projectStageDao().getStageCount()
            if (stageCount == 0) {
                seedDefaultStages(database)
            }
            val customerCount = database.customerDao().getRawCustomerCount()
            if (customerCount == 0) {
                SampleDataGenerator.seedSampleData(database)
            }
        }

        private suspend fun seedDefaultStages(database: AppDatabase) {
            val stageDao = database.projectStageDao()
            val defaultStages = listOf(
                ProjectStageEntity("stage_1", "مشاوره و برداشت ابعاد", 1, "#2196F3", isDefault = true),
                ProjectStageEntity("stage_2", "طراحی سه‌بعدی و تأییدیه", 2, "#9C27B0", isDefault = true),
                ProjectStageEntity("stage_3", "برش لیزر پلکسی و آهن", 3, "#FF9800", isDefault = true),
                ProjectStageEntity("stage_4", "حروف‌سازی و مونتاژ لبه", 4, "#E91E63", isDefault = true),
                ProjectStageEntity("stage_5", "سیم‌کشی و اس‌ام‌دی SMD", 5, "#009688", isDefault = true),
                ProjectStageEntity("stage_6", "زیرسازی کامپوزیت نما", 6, "#795548", isDefault = true),
                ProjectStageEntity("stage_7", "نصب در محل پروژه", 7, "#4CAF50", isDefault = true),
                ProjectStageEntity("stage_8", "تست نور، تحویل و تسویه", 8, "#3F51B5", isDefault = true)
            )
            stageDao.insertStages(defaultStages)
        }

        private suspend fun seedInitialData(database: AppDatabase) {
            val settingsDao = database.settingsDao()
            // Default User
            val defaultUser = UserEntity(
                id = UUID.randomUUID().toString(),
                fullName = "امیرحسین حسنخانی",
                username = "admin",
                passwordHash = "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4", // SHA-256 for "1234"
                securityHint = "رمز پیش‌فرض: 1234",
                isPinEnabled = false,
                isBiometricEnabled = false
            )
            settingsDao.insertOrUpdateUser(defaultUser)

            // Seed default expense categories
            val defaultCategories = listOf(
                "ورق پلکسی گلاس" to 2500000L,
                "رول چلنیوم و لبه آلومینیوم" to 1800000L,
                "بلوک اس‌ام‌دی (SMD)" to 850000L,
                "ترانس و پاور سوئیچینگ" to 1200000L,
                "ورق کامپوزیت نما" to 3200000L,
                "آهن‌آلات و قوطی پروفایل" to 2100000L,
                "چسب و ملزومات مونتاژ" to 450000L,
                "اجاره داربست و نردبان" to 900000L,
                "کرایه جرثقیل و بالابر" to 3000000L,
                "ایاب و ذهاب و حمل تابلو" to 700000L,
                "دستمزد کارگر و نصاب کمکی" to 1500000L
            )

            for ((title, defaultAmt) in defaultCategories) {
                settingsDao.insertExpenseCategory(
                    ExpenseCategoryEntity(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        defaultAmount = defaultAmt,
                        description = "عنوان هزینه استاندارد کارگاه تابلوسازی آلپا"
                    )
                )
            }
        }
    }
}

package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "customers",
    indices = [Index(value = ["phone"]), Index(value = ["isDeleted"])]
)
data class CustomerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val address: String = "",
    val nationalCode: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val userId: String = "admin"
)

@Entity(
    tableName = "project_stages",
    indices = [Index(value = ["displayOrder"]), Index(value = ["isDeleted"])]
)
data class ProjectStageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val displayOrder: Int,
    val colorHex: String = "#FF9800",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
) {
    val title: String get() = name
}

data class TrashItem(
    val id: String,
    val title: String,
    val type: String,
    val categoryLabel: String,
    val details: String,
    val deletedAt: Long
) {
    val typePersian: String get() = categoryLabel
}

@Entity(
    tableName = "projects",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["customerId"]), Index(value = ["isDeleted"])]
)
data class ProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val title: String,
    val startDate: String,
    val status: String = "در حال ساخت", // وضعیت کلان: در حال مذاکره، طراحی، در حال ساخت، در حال نصب، تکمیل شده، تسویه شده
    val stageId: String? = null, // شناسه پایدار مرحله فعلی پروژه
    val completedStageIds: String = "", // لیست شناسه‌های مراحل تکمیل‌شده جدا شده با کاما
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val userId: String = "admin"
)

@Entity(
    tableName = "project_specs",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"]), Index(value = ["isDeleted"])]
)
data class ProjectSpecEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val subTitle: String, // عنوان زیرمجموعه مانند: حروف اصلی، لوگوی شرکت، تابلو ورودی
    val signType: String, // نوع تابلو: چلنیوم، استیل طلایی، پلاستیک، نئون فلکسی، لایت‌باکس، کامپوزیت، ترمووود
    val lengthCm: Double, // طول بر حسب سانتی‌متر
    val widthCm: Double,  // عرض بر حسب سانتی‌متر
    val depthCm: Double = 0.0,  // عمق بر حسب سانتی‌متر
    val attachmentUris: String = "", // آدرس فایل‌ها و عکس‌های ضمیمه جدا شده با کاما
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val userId: String = "admin"
)

@Entity(
    tableName = "invoices",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["projectId"], unique = true), Index(value = ["isDeleted"])]
)
data class InvoiceEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val issueDate: String,
    val discount: Long = 0L,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val userId: String = "admin"
)

@Entity(
    tableName = "invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["invoiceId"]), Index(value = ["isDeleted"])]
)
data class InvoiceItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val invoiceId: String,
    val serviceType: String, // نوع خدمت: ساخت حروف برجسته، زیرسازی کامپوزیت، ترانس و پاور، اس‌ام‌دی، نصب و راه‌اندازی
    val itemDescription: String = "",
    val quantity: Double, // مقدار (متراژ، تعداد یا وزن)
    val unit: String = "عدد", // عدد، متر، سانتی‌متر، متر مربع، کیلوگرم
    val unitPrice: Long, // قیمت واحد
    val calculatedTotal: Long = (quantity * unitPrice).toLong(), // محاسبه خودکار
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val userId: String = "admin"
)

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["invoiceId"]), Index(value = ["projectId"]), Index(value = ["isDeleted"])]
)
data class ExpenseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val invoiceId: String? = null, // مرتبط با فاکتور
    val projectId: String? = null, // مرتبط با پروژه
    val expenseType: String = "PROJECT", // "INVOICE" (هزینه فاکتور مشتری) یا "PROJECT" (هزینه کارگاهی و سود/زیان)
    val category: String, // عنوان هزینه: ورق پلکسی، رول چلنیوم، اس‌ام‌دی، ترانس، داربست، جرثقیل، ایاب‌ذهاب، دستمزد کارگر
    val amount: Long,
    val date: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val userId: String = "admin"
)

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["customerId"]), Index(value = ["isDeleted"])]
)
data class PaymentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val projectId: String? = null,
    val type: String = "RECEIPT", // RECEIPT (دریافتی از مشتری), PAYMENT (پرداختی/استرداد به مشتری)
    val amount: Long,
    val date: String,
    val method: String = "کارت به کارت", // نقدی، کارت به کارت، حواله پایا، دستگاه پوز
    val trackingCode: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val userId: String = "admin"
)

@Entity(
    tableName = "checks",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["customerId"]), Index(value = ["isDeleted"])]
)
data class CheckEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val projectId: String? = null,
    val bankName: String = "",
    val branch: String = "",
    val checkNumber: String = "",
    val sayadNumber: String = "",
    val amount: Long,
    val dueDate: String,
    val status: String = "PENDING", // PENDING (در جریان وصول), CLEARED (وصول شده), BOUNCED (برگشت خورده), RETURNED (عودت داده شده)
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val userId: String = "admin"
)

@Entity(
    tableName = "contracts",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["customerId"]), Index(value = ["projectId"], unique = true), Index(value = ["isDeleted"])]
)
data class ContractEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val projectId: String,
    val contractDate: String,
    val durationDays: Int = 15,
    val totalAmount: Long = 0L,
    val discount: Long = 0L,
    val prepayment: Long = 0L,
    val balanceAmount: Long = 0L,
    val termsJson: String = "", // لیست ساختاریافته بندهای قرارداد
    val arbitratorName: String = "مرضی الطرفین",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val userId: String = "admin"
)

@Entity(
    tableName = "expense_categories",
    indices = [Index(value = ["title"], unique = true), Index(value = ["isDeleted"])]
)
data class ExpenseCategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val defaultAmount: Long = 0L,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val userId: String = "admin"
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val fullName: String = "امیرحسین حسنخانی",
    val username: String = "admin",
    val passwordHash: String = "", // SHA-256 hashed password/PIN
    val securityHint: String = "رمز پیش‌فرض: 1234",
    val isPinEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "audit_logs",
    indices = [Index(value = ["entityType"]), Index(value = ["timestamp"])]
)
data class AuditLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val entityType: String, // Customer, Project, Invoice, Payment, Check, Contract, Expense
    val entityId: String,
    val action: String, // CREATE, UPDATE, DELETE
    val details: String,
    val userId: String = "admin",
    val timestamp: Long = System.currentTimeMillis()
)

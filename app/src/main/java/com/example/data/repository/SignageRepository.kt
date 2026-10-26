package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.SampleDataGenerator
import com.example.data.local.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

data class InvoiceWithDetails(
    val invoice: InvoiceEntity,
    val items: List<InvoiceItemEntity>,
    val expenses: List<ExpenseEntity>,
    val itemsTotal: Long,
    val expensesTotal: Long,
    val netTotal: Long // itemsTotal - discount + expensesTotal
)

data class CustomerFinancialSummary(
    val customerId: String,
    val totalInvoiced: Long,
    val totalPaid: Long,
    val totalClearedChecks: Long,
    val pendingChecksTotal: Long,
    val balance: Long // totalInvoiced - totalPaid - totalClearedChecks (Positive: Debtor/بدهکار, Negative: Creditor/بستانکار)
)

data class DashboardStats(
    val customerCount: Int,
    val activeProjectCount: Int,
    val totalInvoicesCount: Int,
    val totalContractsAmount: Long,
    val totalReceiptsAmount: Long,
    val totalPendingChecksAmount: Long,
    val totalOverdueChecksCount: Int,
    val totalCustomerBalanceDebt: Long
)

class SignageRepository(private val database: AppDatabase) {
    private val customerDao = database.customerDao()
    private val projectDao = database.projectDao()
    private val projectStageDao = database.projectStageDao()
    private val invoiceDao = database.invoiceDao()
    private val financialDao = database.financialDao()
    private val contractDao = database.contractDao()
    private val settingsDao = database.settingsDao()
    private val auditDao = database.auditDao()

    // -------------------------------------------------------------
    // Project Stages (Dynamic Stages Management)
    // -------------------------------------------------------------
    fun getAllStages(): Flow<List<ProjectStageEntity>> = projectStageDao.getAllStages()

    suspend fun getAllStagesOnce(): List<ProjectStageEntity> = projectStageDao.getAllStagesOnce()

    suspend fun saveStage(stage: ProjectStageEntity, isNew: Boolean) {
        val now = System.currentTimeMillis()
        if (isNew) {
            projectStageDao.insertStage(stage.copy(createdAt = now, updatedAt = now))
            logAudit("ProjectStage", stage.id, "CREATE", "افزودن مرحله جدید: ${stage.name}", "admin")
        } else {
            projectStageDao.updateStage(stage.copy(updatedAt = now))
            logAudit("ProjectStage", stage.id, "UPDATE", "ویرایش مرحله: ${stage.name}", "admin")
        }
    }

    suspend fun reorderStages(stages: List<ProjectStageEntity>) {
        val updated = stages.mapIndexed { index, stage ->
            stage.copy(displayOrder = index + 1, updatedAt = System.currentTimeMillis())
        }
        projectStageDao.updateStages(updated)
        logAudit("ProjectStage", "all", "REORDER", "مرتب‌سازی مراحل پروژه (${updated.size} مرحله)", "admin")
    }

    suspend fun deleteStage(id: String, stageName: String) {
        projectStageDao.softDeleteStage(id)
        logAudit("ProjectStage", id, "DELETE", "حذف مرحله پروژه: $stageName", "admin")
    }

    suspend fun updateProjectStages(projectId: String, stageId: String?, completedStageIds: String) {
        projectDao.updateProjectStages(projectId, stageId, completedStageIds)
    }

    suspend fun updateProjectStatus(projectId: String, status: String) {
        projectDao.updateProjectStatus(projectId, status)
    }

    // -------------------------------------------------------------
    // Customers
    // -------------------------------------------------------------
    fun getAllCustomers(): Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    fun getCustomerByIdFlow(id: String): Flow<CustomerEntity?> = customerDao.getCustomerByIdFlow(id)

    suspend fun getCustomerById(id: String): CustomerEntity? = customerDao.getCustomerById(id)

    suspend fun saveCustomer(customer: CustomerEntity, isNew: Boolean, currentUserId: String = "admin") {
        val now = System.currentTimeMillis()
        if (isNew) {
            val toSave = customer.copy(createdAt = now, updatedAt = now, userId = currentUserId)
            customerDao.insertCustomer(toSave)
            logAudit("Customer", toSave.id, "CREATE", "ثبت مشتری جدید: ${toSave.name} (${toSave.phone})", currentUserId)
        } else {
            val toSave = customer.copy(updatedAt = now, userId = currentUserId)
            customerDao.updateCustomer(toSave)
            logAudit("Customer", toSave.id, "UPDATE", "ویرایش اطلاعات مشتری: ${toSave.name}", currentUserId)
        }
    }

    suspend fun deleteCustomer(id: String, customerName: String, currentUserId: String = "admin") {
        customerDao.softDeleteCustomer(id, userId = currentUserId)
        logAudit("Customer", id, "DELETE", "حذف مشتری: $customerName", currentUserId)
    }

    // -------------------------------------------------------------
    // Projects & Specs
    // -------------------------------------------------------------
    fun getAllProjects(): Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    fun getProjectsForCustomer(customerId: String): Flow<List<ProjectEntity>> = projectDao.getProjectsForCustomer(customerId)

    fun getProjectByIdFlow(id: String): Flow<ProjectEntity?> = projectDao.getProjectByIdFlow(id)

    suspend fun getProjectById(id: String): ProjectEntity? = projectDao.getProjectById(id)

    suspend fun saveProject(project: ProjectEntity, isNew: Boolean, currentUserId: String = "admin") {
        val now = System.currentTimeMillis()
        if (isNew) {
            val toSave = project.copy(createdAt = now, updatedAt = now, userId = currentUserId)
            projectDao.insertProject(toSave)
            logAudit("Project", toSave.id, "CREATE", "ایجاد پروژه جدید: ${toSave.title}", currentUserId)
        } else {
            val toSave = project.copy(updatedAt = now, userId = currentUserId)
            projectDao.updateProject(toSave)
            logAudit("Project", toSave.id, "UPDATE", "ویرایش پروژه: ${toSave.title} - وضعیت: ${toSave.status}", currentUserId)
        }
    }

    suspend fun deleteProject(id: String, projectTitle: String, currentUserId: String = "admin") {
        projectDao.softDeleteProject(id, userId = currentUserId)
        logAudit("Project", id, "DELETE", "حذف پروژه: $projectTitle", currentUserId)
    }

    fun getSpecsForProject(projectId: String): Flow<List<ProjectSpecEntity>> = projectDao.getSpecsForProject(projectId)

    suspend fun getSpecsForProjectOnce(projectId: String): List<ProjectSpecEntity> = projectDao.getSpecsForProjectOnce(projectId)

    suspend fun saveSpec(spec: ProjectSpecEntity, isNew: Boolean, currentUserId: String = "admin") {
        val now = System.currentTimeMillis()
        if (isNew) {
            val toSave = spec.copy(createdAt = now, updatedAt = now, userId = currentUserId)
            projectDao.insertSpec(toSave)
            logAudit("ProjectSpec", toSave.id, "CREATE", "افزودن مشخصات تابلو: ${toSave.subTitle} (${toSave.signType})", currentUserId)
        } else {
            val toSave = spec.copy(updatedAt = now, userId = currentUserId)
            projectDao.updateSpec(toSave)
            logAudit("ProjectSpec", toSave.id, "UPDATE", "ویرایش مشخصات تابلو: ${toSave.subTitle}", currentUserId)
        }
    }

    suspend fun deleteSpec(id: String, title: String, currentUserId: String = "admin") {
        projectDao.softDeleteSpec(id, userId = currentUserId)
        logAudit("ProjectSpec", id, "DELETE", "حذف ردیف مشخصات تابلو: $title", currentUserId)
    }

    // -------------------------------------------------------------
    // Invoices & Invoice Items
    // -------------------------------------------------------------
    fun getAllInvoices(): Flow<List<InvoiceEntity>> = invoiceDao.getAllInvoices()

    fun getInvoiceForProject(projectId: String): Flow<InvoiceEntity?> = invoiceDao.getInvoiceForProject(projectId)

    suspend fun getInvoiceForProjectOnce(projectId: String): InvoiceEntity? = invoiceDao.getInvoiceForProjectOnce(projectId)

    fun getInvoiceItems(invoiceId: String): Flow<List<InvoiceItemEntity>> = invoiceDao.getItemsForInvoice(invoiceId)

    suspend fun getInvoiceItemsOnce(invoiceId: String): List<InvoiceItemEntity> = invoiceDao.getItemsForInvoiceOnce(invoiceId)

    suspend fun saveInvoice(invoice: InvoiceEntity, isNew: Boolean, currentUserId: String = "admin") {
        val now = System.currentTimeMillis()
        if (isNew) {
            val toSave = invoice.copy(createdAt = now, updatedAt = now, userId = currentUserId)
            invoiceDao.insertInvoice(toSave)
            logAudit("Invoice", toSave.id, "CREATE", "ثبت فاکتور پروژه", currentUserId)
        } else {
            val toSave = invoice.copy(updatedAt = now, userId = currentUserId)
            invoiceDao.updateInvoice(toSave)
            logAudit("Invoice", toSave.id, "UPDATE", "ویرایش فاکتور پروژه", currentUserId)
        }
    }

    suspend fun deleteInvoice(id: String, currentUserId: String = "admin") {
        invoiceDao.softDeleteInvoice(id, userId = currentUserId)
        logAudit("Invoice", id, "DELETE", "حذف فاکتور پروژه", currentUserId)
    }

    suspend fun saveInvoiceItem(item: InvoiceItemEntity, isNew: Boolean, currentUserId: String = "admin") {
        val now = System.currentTimeMillis()
        val calcTotal = (item.quantity * item.unitPrice).toLong()
        if (isNew) {
            val toSave = item.copy(calculatedTotal = calcTotal, createdAt = now, updatedAt = now, userId = currentUserId)
            invoiceDao.insertInvoiceItem(toSave)
            logAudit("InvoiceItem", toSave.id, "CREATE", "افزودن ردیف فاکتور: ${toSave.serviceType}", currentUserId)
        } else {
            val toSave = item.copy(calculatedTotal = calcTotal, updatedAt = now, userId = currentUserId)
            invoiceDao.updateInvoiceItem(toSave)
            logAudit("InvoiceItem", toSave.id, "UPDATE", "ویرایش ردیف فاکتور: ${toSave.serviceType}", currentUserId)
        }
    }

    suspend fun deleteInvoiceItem(id: String, serviceType: String, currentUserId: String = "admin") {
        invoiceDao.softDeleteInvoiceItem(id, userId = currentUserId)
        logAudit("InvoiceItem", id, "DELETE", "حذف ردیف فاکتور: $serviceType", currentUserId)
    }

    // -------------------------------------------------------------
    // Payments, Checks, Expenses
    // -------------------------------------------------------------
    fun getAllPayments(): Flow<List<PaymentEntity>> = financialDao.getAllPayments()

    fun getPaymentsForCustomer(customerId: String): Flow<List<PaymentEntity>> = financialDao.getPaymentsForCustomer(customerId)

    suspend fun savePayment(payment: PaymentEntity, isNew: Boolean, currentUserId: String = "admin") {
        val now = System.currentTimeMillis()
        val typeLabel = if (payment.type == "RECEIPT") "دریافت" else "پرداخت"
        if (isNew) {
            val toSave = payment.copy(createdAt = now, updatedAt = now, userId = currentUserId)
            financialDao.insertPayment(toSave)
            logAudit("Payment", toSave.id, "CREATE", "ثبت تراکنش $typeLabel به مبلغ ${toSave.amount}", currentUserId)
        } else {
            val toSave = payment.copy(updatedAt = now, userId = currentUserId)
            financialDao.updatePayment(toSave)
            logAudit("Payment", toSave.id, "UPDATE", "ویرایش تراکنش $typeLabel به مبلغ ${toSave.amount}", currentUserId)
        }
    }

    suspend fun deletePayment(id: String, amount: Long, type: String, currentUserId: String = "admin") {
        financialDao.softDeletePayment(id, userId = currentUserId)
        logAudit("Payment", id, "DELETE", "حذف تراکنش $type به مبلغ $amount", currentUserId)
    }

    fun getAllChecks(): Flow<List<CheckEntity>> = financialDao.getAllChecks()

    fun getChecksForCustomer(customerId: String): Flow<List<CheckEntity>> = financialDao.getChecksForCustomer(customerId)

    suspend fun saveCheck(check: CheckEntity, isNew: Boolean, currentUserId: String = "admin") {
        val now = System.currentTimeMillis()
        if (isNew) {
            val toSave = check.copy(createdAt = now, updatedAt = now, userId = currentUserId)
            financialDao.insertCheck(toSave)
            logAudit("Check", toSave.id, "CREATE", "ثبت چک سررسید ${toSave.dueDate} به مبلغ ${toSave.amount}", currentUserId)
        } else {
            val toSave = check.copy(updatedAt = now, userId = currentUserId)
            financialDao.updateCheck(toSave)
            logAudit("Check", toSave.id, "UPDATE", "ویرایش چک سررسید ${toSave.dueDate} - وضعیت: ${toSave.status}", currentUserId)
        }
    }

    suspend fun deleteCheck(id: String, amount: Long, currentUserId: String = "admin") {
        financialDao.softDeleteCheck(id, userId = currentUserId)
        logAudit("Check", id, "DELETE", "حذف چک به مبلغ $amount", currentUserId)
    }

    fun getAllExpenses(): Flow<List<ExpenseEntity>> = financialDao.getAllExpenses()

    fun getExpensesForInvoice(invoiceId: String): Flow<List<ExpenseEntity>> = financialDao.getExpensesForInvoice(invoiceId)

    suspend fun getExpensesForInvoiceOnce(invoiceId: String): List<ExpenseEntity> = financialDao.getExpensesForInvoiceOnce(invoiceId)

    suspend fun saveExpense(expense: ExpenseEntity, isNew: Boolean, currentUserId: String = "admin") {
        val now = System.currentTimeMillis()
        if (isNew) {
            val toSave = expense.copy(createdAt = now, updatedAt = now, userId = currentUserId)
            financialDao.insertExpense(toSave)
            logAudit("Expense", toSave.id, "CREATE", "ثبت هزینه ${toSave.category} به مبلغ ${toSave.amount}", currentUserId)
        } else {
            val toSave = expense.copy(updatedAt = now, userId = currentUserId)
            financialDao.updateExpense(toSave)
            logAudit("Expense", toSave.id, "UPDATE", "ویرایش هزینه ${toSave.category}", currentUserId)
        }
    }

    suspend fun deleteExpense(id: String, title: String, currentUserId: String = "admin") {
        financialDao.softDeleteExpense(id, userId = currentUserId)
        logAudit("Expense", id, "DELETE", "حذف هزینه: $title", currentUserId)
    }

    // -------------------------------------------------------------
    // Contracts
    // -------------------------------------------------------------
    fun getAllContracts(): Flow<List<ContractEntity>> = contractDao.getAllContracts()

    fun getContractsForCustomer(customerId: String): Flow<List<ContractEntity>> = contractDao.getContractsForCustomer(customerId)

    fun getContractForProject(projectId: String): Flow<ContractEntity?> = contractDao.getContractForProject(projectId)

    suspend fun getContractForProjectOnce(projectId: String): ContractEntity? = contractDao.getContractForProjectOnce(projectId)

    suspend fun saveContract(contract: ContractEntity, isNew: Boolean, currentUserId: String = "admin") {
        val now = System.currentTimeMillis()
        if (isNew) {
            val toSave = contract.copy(createdAt = now, updatedAt = now, userId = currentUserId)
            contractDao.insertContract(toSave)
            logAudit("Contract", toSave.id, "CREATE", "ثبت قرارداد تابلوسازی مبلغ ${toSave.totalAmount}", currentUserId)
        } else {
            val toSave = contract.copy(updatedAt = now, userId = currentUserId)
            contractDao.updateContract(toSave)
            logAudit("Contract", toSave.id, "UPDATE", "ویرایش قرارداد تابلوسازی", currentUserId)
        }
    }

    suspend fun deleteContract(id: String, currentUserId: String = "admin") {
        contractDao.softDeleteContract(id, userId = currentUserId)
        logAudit("Contract", id, "DELETE", "حذف قرارداد", currentUserId)
    }

    // -------------------------------------------------------------
    // Expense Categories & Users
    // -------------------------------------------------------------
    fun getAllExpenseCategories(): Flow<List<ExpenseCategoryEntity>> = settingsDao.getAllExpenseCategories()

    suspend fun getAllExpenseCategoriesOnce(): List<ExpenseCategoryEntity> = settingsDao.getAllExpenseCategoriesOnce()

    suspend fun saveExpenseCategory(cat: ExpenseCategoryEntity, isNew: Boolean, currentUserId: String = "admin") {
        val now = System.currentTimeMillis()
        if (isNew) {
            val toSave = cat.copy(createdAt = now, updatedAt = now, userId = currentUserId)
            settingsDao.insertExpenseCategory(toSave)
            logAudit("ExpenseCategory", toSave.id, "CREATE", "افزودن دسته‌بندی هزینه: ${toSave.title}", currentUserId)
        } else {
            val toSave = cat.copy(updatedAt = now, userId = currentUserId)
            settingsDao.updateExpenseCategory(toSave)
            logAudit("ExpenseCategory", toSave.id, "UPDATE", "ویرایش دسته‌بندی هزینه: ${toSave.title}", currentUserId)
        }
    }

    suspend fun deleteExpenseCategory(id: String, title: String, currentUserId: String = "admin") {
        settingsDao.softDeleteExpenseCategory(id, userId = currentUserId)
        logAudit("ExpenseCategory", id, "DELETE", "حذف دسته‌بندی هزینه: $title", currentUserId)
    }

    fun getUserFlow(): Flow<UserEntity?> = settingsDao.getUserFlow()

    suspend fun getUserOnce(): UserEntity? = settingsDao.getUserOnce()

    suspend fun saveUser(user: UserEntity) {
        val now = System.currentTimeMillis()
        settingsDao.insertOrUpdateUser(user.copy(updatedAt = now))
        logAudit("User", user.id, "UPDATE", "بروزرسانی پروفایل کاربر و تنظیمات امنیت", user.id)
    }

    // -------------------------------------------------------------
    // Audit Logs
    // -------------------------------------------------------------
    fun getRecentAuditLogs(): Flow<List<AuditLogEntity>> = auditDao.getRecentAuditLogs()

    suspend fun logAudit(entityType: String, entityId: String, action: String, details: String, userId: String) {
        auditDao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                entityType = entityType,
                entityId = entityId,
                action = action,
                details = details,
                userId = userId,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    // -------------------------------------------------------------
    // Financial Calculations & Customer Balance
    // -------------------------------------------------------------
    fun getCustomerFinancialSummary(customerId: String): Flow<CustomerFinancialSummary> {
        return combine(
            projectDao.getProjectsForCustomer(customerId),
            financialDao.getPaymentsForCustomer(customerId),
            financialDao.getChecksForCustomer(customerId),
            contractDao.getContractsForCustomer(customerId)
        ) { projects, payments, checks, contracts ->
            val totalPaid = payments.filter { it.type == "RECEIPT" }.sumOf { it.amount } -
                    payments.filter { it.type == "PAYMENT" }.sumOf { it.amount }

            val totalClearedChecks = checks.filter { it.status == "CLEARED" }.sumOf { it.amount }
            val pendingChecksTotal = checks.filter { it.status == "PENDING" }.sumOf { it.amount }

            // Contract amounts or project billing
            val contractTotal = contracts.sumOf { it.totalAmount - it.discount }
            val balance = contractTotal - totalPaid - totalClearedChecks

            CustomerFinancialSummary(
                customerId = customerId,
                totalInvoiced = contractTotal,
                totalPaid = totalPaid,
                totalClearedChecks = totalClearedChecks,
                pendingChecksTotal = pendingChecksTotal,
                balance = balance
            )
        }
    }

    // -------------------------------------------------------------
    // Trash / Recycle Bin (سطل زباله)
    // -------------------------------------------------------------
    fun getTrashItemsFlow(): Flow<List<TrashItem>> {
        return combine(
            listOf(
                customerDao.getDeletedCustomers(),
                projectDao.getDeletedProjects(),
                invoiceDao.getDeletedInvoices(),
                financialDao.getDeletedPayments(),
                financialDao.getDeletedChecks(),
                financialDao.getDeletedExpenses()
            )
        ) { array ->
            @Suppress("UNCHECKED_CAST")
            val customers = array[0] as List<CustomerEntity>
            @Suppress("UNCHECKED_CAST")
            val projects = array[1] as List<ProjectEntity>
            @Suppress("UNCHECKED_CAST")
            val invoices = array[2] as List<InvoiceEntity>
            @Suppress("UNCHECKED_CAST")
            val payments = array[3] as List<PaymentEntity>
            @Suppress("UNCHECKED_CAST")
            val checks = array[4] as List<CheckEntity>
            @Suppress("UNCHECKED_CAST")
            val expenses = array[5] as List<ExpenseEntity>

            val list = mutableListOf<TrashItem>()
            customers.forEach {
                list.add(TrashItem(it.id, it.name, "CUSTOMER", "مشتری", "شماره تماس: ${it.phone}", it.updatedAt))
            }
            projects.forEach {
                list.add(TrashItem(it.id, it.title, "PROJECT", "پروژه", "وضعیت: ${it.status}", it.updatedAt))
            }
            invoices.forEach {
                list.add(TrashItem(it.id, "فاکتور تاریخ ${it.issueDate}", "INVOICE", "فاکتور", "تخفیف: ${it.discount}", it.updatedAt))
            }
            payments.forEach {
                val pType = if (it.type == "RECEIPT") "دریافت از مشتری" else "پرداخت به مشتری"
                list.add(TrashItem(it.id, "$pType - مبلغ ${it.amount}", "PAYMENT", "تراکنش", "روش: ${it.method}", it.updatedAt))
            }
            checks.forEach {
                list.add(TrashItem(it.id, "چک ${it.bankName} - ${it.checkNumber}", "CHECK", "چک صیادی", "مبلغ: ${it.amount} - سررسید: ${it.dueDate}", it.updatedAt))
            }
            expenses.forEach {
                val expPersian = if (it.expenseType == "INVOICE") "هزینه فاکتور" else "هزینه کارگاهی"
                list.add(TrashItem(it.id, "$expPersian: ${it.category}", "EXPENSE", "هزینه", "مبلغ: ${it.amount} - تاریخ: ${it.date}", it.updatedAt))
            }
            list.sortedByDescending { it.deletedAt }
        }
    }

    suspend fun restoreTrashItem(item: TrashItem) {
        val now = System.currentTimeMillis()
        when (item.type) {
            "CUSTOMER" -> customerDao.restoreCustomer(item.id, now)
            "PROJECT" -> projectDao.restoreProject(item.id, now)
            "INVOICE" -> invoiceDao.restoreInvoice(item.id, now)
            "PAYMENT" -> financialDao.restorePayment(item.id, now)
            "CHECK" -> financialDao.restoreCheck(item.id, now)
            "EXPENSE" -> financialDao.restoreExpense(item.id, now)
            "CONTRACT" -> contractDao.restoreContract(item.id, now)
        }
        logAudit("Trash", item.id, "RESTORE", "بازیابی از سطل زباله: ${item.typePersian} - ${item.title}", "admin")
    }

    suspend fun clearSampleData() {
        SampleDataGenerator.clearSampleData(database)
    }

    suspend fun seedSampleData() {
        SampleDataGenerator.seedSampleData(database)
    }
}

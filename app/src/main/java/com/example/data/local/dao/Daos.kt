package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun getDeletedCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id AND isDeleted = 0")
    suspend fun getCustomerById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE id = :id AND isDeleted = 0")
    fun getCustomerByIdFlow(id: String): Flow<CustomerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET isDeleted = 1, updatedAt = :updatedAt, userId = :userId WHERE id = :id")
    suspend fun softDeleteCustomer(id: String, updatedAt: Long = System.currentTimeMillis(), userId: String)

    @Query("UPDATE customers SET isDeleted = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun restoreCustomer(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM customers WHERE isDeleted = 0")
    fun getCustomerCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun getRawCustomerCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Query("DELETE FROM customers")
    suspend fun deleteAllCustomers()
}

@Dao
interface ProjectStageDao {
    @Query("SELECT * FROM project_stages WHERE isDeleted = 0 ORDER BY displayOrder ASC")
    fun getAllStages(): Flow<List<ProjectStageEntity>>

    @Query("SELECT * FROM project_stages WHERE isDeleted = 0 ORDER BY displayOrder ASC")
    suspend fun getAllStagesOnce(): List<ProjectStageEntity>

    @Query("SELECT * FROM project_stages WHERE id = :id AND isDeleted = 0")
    suspend fun getStageById(id: String): ProjectStageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStage(stage: ProjectStageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStages(stages: List<ProjectStageEntity>)

    @Update
    suspend fun updateStage(stage: ProjectStageEntity)

    @Update
    suspend fun updateStages(stages: List<ProjectStageEntity>)

    @Query("UPDATE project_stages SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteStage(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM project_stages WHERE isDeleted = 0")
    suspend fun getStageCount(): Int
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun getDeletedProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE customerId = :customerId AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getProjectsForCustomer(customerId: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id AND isDeleted = 0")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :id AND isDeleted = 0")
    fun getProjectByIdFlow(id: String): Flow<ProjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("UPDATE projects SET stageId = :stageId, completedStageIds = :completedStageIds, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateProjectStages(id: String, stageId: String?, completedStageIds: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE projects SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateProjectStatus(id: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE projects SET isDeleted = 1, updatedAt = :updatedAt, userId = :userId WHERE id = :id")
    suspend fun softDeleteProject(id: String, updatedAt: Long = System.currentTimeMillis(), userId: String)

    @Query("UPDATE projects SET isDeleted = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun restoreProject(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM projects WHERE isDeleted = 0 AND status != 'تکمیل شده' AND status != 'تسویه شده'")
    fun getActiveProjectCount(): Flow<Int>

    // Project Specs
    @Query("SELECT * FROM project_specs WHERE projectId = :projectId AND isDeleted = 0 ORDER BY createdAt ASC")
    fun getSpecsForProject(projectId: String): Flow<List<ProjectSpecEntity>>

    @Query("SELECT * FROM project_specs WHERE projectId = :projectId AND isDeleted = 0 ORDER BY createdAt ASC")
    suspend fun getSpecsForProjectOnce(projectId: String): List<ProjectSpecEntity>

    @Query("SELECT * FROM project_specs WHERE id = :id AND isDeleted = 0")
    suspend fun getSpecById(id: String): ProjectSpecEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpec(spec: ProjectSpecEntity)

    @Update
    suspend fun updateSpec(spec: ProjectSpecEntity)

    @Query("UPDATE project_specs SET isDeleted = 1, updatedAt = :updatedAt, userId = :userId WHERE id = :id")
    suspend fun softDeleteSpec(id: String, updatedAt: Long = System.currentTimeMillis(), userId: String)

    @Query("UPDATE project_specs SET isDeleted = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun restoreSpec(id: String, updatedAt: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpecs(specs: List<ProjectSpecEntity>)

    @Query("DELETE FROM project_specs")
    suspend fun deleteAllSpecs()

    @Query("DELETE FROM projects")
    suspend fun deleteAllProjects()
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun getDeletedInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE projectId = :projectId AND isDeleted = 0 LIMIT 1")
    fun getInvoiceForProject(projectId: String): Flow<InvoiceEntity?>

    @Query("SELECT * FROM invoices WHERE projectId = :projectId AND isDeleted = 0 LIMIT 1")
    suspend fun getInvoiceForProjectOnce(projectId: String): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE id = :id AND isDeleted = 0")
    suspend fun getInvoiceById(id: String): InvoiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity)

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Query("UPDATE invoices SET isDeleted = 1, updatedAt = :updatedAt, userId = :userId WHERE id = :id")
    suspend fun softDeleteInvoice(id: String, updatedAt: Long = System.currentTimeMillis(), userId: String)

    @Query("UPDATE invoices SET isDeleted = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun restoreInvoice(id: String, updatedAt: Long = System.currentTimeMillis())

    // Invoice items
    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId AND isDeleted = 0 ORDER BY createdAt ASC")
    fun getItemsForInvoice(invoiceId: String): Flow<List<InvoiceItemEntity>>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId AND isDeleted = 0 ORDER BY createdAt ASC")
    suspend fun getItemsForInvoiceOnce(invoiceId: String): List<InvoiceItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItem(item: InvoiceItemEntity)

    @Update
    suspend fun updateInvoiceItem(item: InvoiceItemEntity)

    @Query("UPDATE invoice_items SET isDeleted = 1, updatedAt = :updatedAt, userId = :userId WHERE id = :id")
    suspend fun softDeleteInvoiceItem(id: String, updatedAt: Long = System.currentTimeMillis(), userId: String)

    @Query("UPDATE invoice_items SET isDeleted = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun restoreInvoiceItem(id: String, updatedAt: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoices(invoices: List<InvoiceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItemEntity>)

    @Query("DELETE FROM invoice_items")
    suspend fun deleteAllInvoiceItems()

    @Query("DELETE FROM invoices")
    suspend fun deleteAllInvoices()
}

@Dao
interface FinancialDao {
    // Payments
    @Query("SELECT * FROM payments WHERE isDeleted = 0 ORDER BY date DESC, createdAt DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE isDeleted = 1 ORDER BY date DESC, createdAt DESC")
    fun getDeletedPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE customerId = :customerId AND isDeleted = 0 ORDER BY date DESC")
    fun getPaymentsForCustomer(customerId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE id = :id AND isDeleted = 0")
    suspend fun getPaymentById(id: String): PaymentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Update
    suspend fun updatePayment(payment: PaymentEntity)

    @Query("UPDATE payments SET isDeleted = 1, updatedAt = :updatedAt, userId = :userId WHERE id = :id")
    suspend fun softDeletePayment(id: String, updatedAt: Long = System.currentTimeMillis(), userId: String)

    @Query("UPDATE payments SET isDeleted = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun restorePayment(id: String, updatedAt: Long = System.currentTimeMillis())

    // Checks
    @Query("SELECT * FROM checks WHERE isDeleted = 0 ORDER BY dueDate ASC")
    fun getAllChecks(): Flow<List<CheckEntity>>

    @Query("SELECT * FROM checks WHERE isDeleted = 1 ORDER BY dueDate ASC")
    fun getDeletedChecks(): Flow<List<CheckEntity>>

    @Query("SELECT * FROM checks WHERE customerId = :customerId AND isDeleted = 0 ORDER BY dueDate ASC")
    fun getChecksForCustomer(customerId: String): Flow<List<CheckEntity>>

    @Query("SELECT * FROM checks WHERE id = :id AND isDeleted = 0")
    suspend fun getCheckById(id: String): CheckEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheck(check: CheckEntity)

    @Update
    suspend fun updateCheck(check: CheckEntity)

    @Query("UPDATE checks SET isDeleted = 1, updatedAt = :updatedAt, userId = :userId WHERE id = :id")
    suspend fun softDeleteCheck(id: String, updatedAt: Long = System.currentTimeMillis(), userId: String)

    @Query("UPDATE checks SET isDeleted = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun restoreCheck(id: String, updatedAt: Long = System.currentTimeMillis())

    // Expenses
    @Query("SELECT * FROM expenses WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE isDeleted = 1 ORDER BY date DESC")
    fun getDeletedExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE invoiceId = :invoiceId AND isDeleted = 0 ORDER BY date DESC")
    fun getExpensesForInvoice(invoiceId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE invoiceId = :invoiceId AND isDeleted = 0 ORDER BY date DESC")
    suspend fun getExpensesForInvoiceOnce(invoiceId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE projectId = :projectId AND isDeleted = 0 ORDER BY date DESC")
    fun getExpensesForProject(projectId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE projectId = :projectId AND isDeleted = 0 ORDER BY date DESC")
    suspend fun getExpensesForProjectOnce(projectId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id AND isDeleted = 0")
    suspend fun getExpenseById(id: String): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("UPDATE expenses SET isDeleted = 1, updatedAt = :updatedAt, userId = :userId WHERE id = :id")
    suspend fun softDeleteExpense(id: String, updatedAt: Long = System.currentTimeMillis(), userId: String)

    @Query("UPDATE expenses SET isDeleted = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun restoreExpense(id: String, updatedAt: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<PaymentEntity>)

    @Query("DELETE FROM payments")
    suspend fun deleteAllPayments()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecks(checks: List<CheckEntity>)

    @Query("DELETE FROM checks")
    suspend fun deleteAllChecks()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()
}

@Dao
interface ContractDao {
    @Query("SELECT * FROM contracts WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllContracts(): Flow<List<ContractEntity>>

    @Query("SELECT * FROM contracts WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun getDeletedContracts(): Flow<List<ContractEntity>>

    @Query("SELECT * FROM contracts WHERE customerId = :customerId AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getContractsForCustomer(customerId: String): Flow<List<ContractEntity>>

    @Query("SELECT * FROM contracts WHERE projectId = :projectId AND isDeleted = 0 LIMIT 1")
    fun getContractForProject(projectId: String): Flow<ContractEntity?>

    @Query("SELECT * FROM contracts WHERE projectId = :projectId AND isDeleted = 0 LIMIT 1")
    suspend fun getContractForProjectOnce(projectId: String): ContractEntity?

    @Query("SELECT * FROM contracts WHERE id = :id AND isDeleted = 0")
    suspend fun getContractById(id: String): ContractEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContract(contract: ContractEntity)

    @Update
    suspend fun updateContract(contract: ContractEntity)

    @Query("UPDATE contracts SET isDeleted = 1, updatedAt = :updatedAt, userId = :userId WHERE id = :id")
    suspend fun softDeleteContract(id: String, updatedAt: Long = System.currentTimeMillis(), userId: String)

    @Query("UPDATE contracts SET isDeleted = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun restoreContract(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT SUM(totalAmount) FROM contracts WHERE isDeleted = 0")
    fun getTotalContractsAmount(): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContracts(contracts: List<ContractEntity>)

    @Query("DELETE FROM contracts")
    suspend fun deleteAllContracts()
}

@Dao
interface SettingsDao {
    // Expense Categories
    @Query("SELECT * FROM expense_categories WHERE isDeleted = 0 ORDER BY title ASC")
    fun getAllExpenseCategories(): Flow<List<ExpenseCategoryEntity>>

    @Query("SELECT * FROM expense_categories WHERE isDeleted = 0 ORDER BY title ASC")
    suspend fun getAllExpenseCategoriesOnce(): List<ExpenseCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseCategory(category: ExpenseCategoryEntity)

    @Update
    suspend fun updateExpenseCategory(category: ExpenseCategoryEntity)

    @Query("UPDATE expense_categories SET isDeleted = 1, updatedAt = :updatedAt, userId = :userId WHERE id = :id")
    suspend fun softDeleteExpenseCategory(id: String, updatedAt: Long = System.currentTimeMillis(), userId: String)

    // User / Auth
    @Query("SELECT * FROM users LIMIT 1")
    fun getUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getUserOnce(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: UserEntity)
}

@Dao
interface AuditDao {
    @Insert
    suspend fun insertAuditLog(log: AuditLogEntity)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentAuditLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE entityType = :type AND entityId = :entityId ORDER BY timestamp DESC")
    fun getLogsForEntity(type: String, entityId: String): Flow<List<AuditLogEntity>>
}

package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entities.*
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.ContractorInfo
import com.example.data.repository.CustomerFinancialSummary
import com.example.data.repository.SignageRepository
import com.example.util.BackupMetadata
import com.example.util.BackupRestoreManager
import com.example.util.JalaliDate
import com.example.util.SecurityUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class SignageViewModel(application: Application) : AndroidViewModel(application) {
    val database = AppDatabase.getDatabase(application)
    val repository = SignageRepository(database)
    val preferences = AppPreferences(application)

    // Current logged in user / auth state
    val currentUser = repository.getUserFlow().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val isAuthEnabled = preferences.isAuthEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val isBiometricEnabled = preferences.isBiometricEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val currencyUnit = preferences.currencyUnit.stateIn(viewModelScope, SharingStarted.Eagerly, "TOMAN")
    val themeMode = preferences.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, "SYSTEM")
    val primaryColorKey = preferences.primaryColorKey.stateIn(viewModelScope, SharingStarted.Eagerly, "AMBER")
    val contractorInfo = preferences.contractorInfo.stateIn(viewModelScope, SharingStarted.Eagerly, ContractorInfo())
    val warnUnsavedChanges = preferences.warnUnsavedChanges.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    // -------------------------------------------------------------
    // Customers State
    // -------------------------------------------------------------
    val customers = repository.getAllCustomers().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // -------------------------------------------------------------
    // Projects & Stages State
    // -------------------------------------------------------------
    val projects = repository.getAllProjects().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val stages = repository.getAllStages().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // -------------------------------------------------------------
    // Financials State
    // -------------------------------------------------------------
    val invoices = repository.getAllInvoices().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val payments = repository.getAllPayments().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val checks = repository.getAllChecks().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val expenses = repository.getAllExpenses().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val contracts = repository.getAllContracts().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val expenseCategories = repository.getAllExpenseCategories().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val recentAudits = repository.getRecentAuditLogs().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val trashItems = repository.getTrashItemsFlow().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // -------------------------------------------------------------
    // Backup Files List
    // -------------------------------------------------------------
    private val _backupList = MutableStateFlow<List<BackupMetadata>>(emptyList())
    val backupList: StateFlow<List<BackupMetadata>> = _backupList.asStateFlow()

    init {
        refreshBackupList()
    }

    fun refreshBackupList() {
        val backups = BackupRestoreManager.listBackups(getApplication())
        _backupList.value = backups
    }

    // -------------------------------------------------------------
    // Actions: Customers
    // -------------------------------------------------------------
    fun saveCustomer(customer: CustomerEntity, isNew: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value?.username ?: "admin"
            repository.saveCustomer(customer, isNew, user)
            onDone()
        }
    }

    fun deleteCustomer(customer: CustomerEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value?.username ?: "admin"
            repository.deleteCustomer(customer.id, customer.name, user)
            onDone()
        }
    }

    fun getCustomerFinancialSummary(customerId: String): Flow<CustomerFinancialSummary> {
        return repository.getCustomerFinancialSummary(customerId)
    }

    // -------------------------------------------------------------
    // Actions: Projects & Specs
    // -------------------------------------------------------------
    fun saveProject(project: ProjectEntity, isNew: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value?.username ?: "admin"
            repository.saveProject(project, isNew, user)
            onDone()
        }
    }

    fun deleteProject(project: ProjectEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value?.username ?: "admin"
            repository.deleteProject(project.id, project.title, user)
            onDone()
        }
    }

    fun updateProjectStages(projectId: String, stageId: String?, completedStageIds: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateProjectStages(projectId, stageId, completedStageIds)
            onDone()
        }
    }

    fun updateProjectStages(projectId: String, stageId: String?, completedStageIds: List<String>, onDone: () -> Unit = {}) {
        updateProjectStages(projectId, stageId, completedStageIds.joinToString(","), onDone)
    }

    fun updateProjectStatus(projectId: String, status: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateProjectStatus(projectId, status)
            onDone()
        }
    }

    fun saveStage(stage: ProjectStageEntity, isNew: Boolean, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveStage(stage, isNew)
            onDone()
        }
    }

    fun deleteStage(stage: ProjectStageEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteStage(stage.id, stage.name)
            onDone()
        }
    }

    fun reorderStages(stages: List<ProjectStageEntity>) {
        viewModelScope.launch {
            repository.reorderStages(stages)
        }
    }

    fun restoreTrashItem(item: TrashItem, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.restoreTrashItem(item)
            onDone()
        }
    }

    fun setWarnUnsavedChanges(warn: Boolean) {
        viewModelScope.launch {
            preferences.setWarnUnsavedChanges(warn)
        }
    }

    fun getSpecsForProject(projectId: String): Flow<List<ProjectSpecEntity>> {
        return repository.getSpecsForProject(projectId)
    }

    fun saveSpec(spec: ProjectSpecEntity, isNew: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value?.username ?: "admin"
            repository.saveSpec(spec, isNew, user)
            onDone()
        }
    }

    fun deleteSpec(spec: ProjectSpecEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value?.username ?: "admin"
            repository.deleteSpec(spec.id, spec.subTitle, user)
            onDone()
        }
    }

    // -------------------------------------------------------------
    // Actions: Invoices & Items
    // -------------------------------------------------------------
    fun getInvoiceForProject(projectId: String): Flow<InvoiceEntity?> {
        return repository.getInvoiceForProject(projectId)
    }

    fun getInvoiceItems(invoiceId: String): Flow<List<InvoiceItemEntity>> {
        return repository.getInvoiceItems(invoiceId)
    }

    fun saveInvoice(invoice: InvoiceEntity, isNew: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value?.username ?: "admin"
            repository.saveInvoice(invoice, isNew, user)
            onDone()
        }
    }

    fun deleteInvoice(invoice: InvoiceEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value?.username ?: "admin"
            repository.deleteInvoice(invoice.id, user)
            onDone()
        }
    }

    fun saveInvoiceItem(item: InvoiceItemEntity, isNew: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value?.username ?: "admin"
            repository.saveInvoiceItem(item, isNew, user)
            onDone()
        }
    }

    fun deleteInvoiceItem(item: InvoiceItemEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value?.username ?: "admin"
            repository.deleteInvoiceItem(item.id, item.serviceType, user)
            onDone()
        }
    }

    // -------------------------------------------------------------
    // Actions: Payments, Checks, Expenses
    // -------------------------------------------------------------
    fun savePayment(payment: PaymentEntity, isNew: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value?.username ?: "admin"
            repository.savePayment(payment, isNew, user)
            onDone()
        }
    }

    fun deletePayment(payment: PaymentEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value?.username ?: "admin"
            repository.deletePayment(payment.id, payment.amount, payment.type, user)
            onDone()
        }
    }

    fun saveCheck(check: CheckEntity, isNew: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value?.username ?: "admin"
            repository.saveCheck(check, isNew, user)
            onDone()
        }
    }

    fun deleteCheck(check: CheckEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value?.username ?: "admin"
            repository.deleteCheck(check.id, check.amount, user)
            onDone()
        }
    }

    fun saveExpense(expense: ExpenseEntity, isNew: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value?.username ?: "admin"
            repository.saveExpense(expense, isNew, user)
            onDone()
        }
    }

    fun deleteExpense(expense: ExpenseEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value?.username ?: "admin"
            repository.deleteExpense(expense.id, expense.category, user)
            onDone()
        }
    }

    // -------------------------------------------------------------
    // Actions: Contracts
    // -------------------------------------------------------------
    fun getContractForProject(projectId: String): Flow<ContractEntity?> {
        return repository.getContractForProject(projectId)
    }

    fun saveContract(contract: ContractEntity, isNew: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value?.username ?: "admin"
            repository.saveContract(contract, isNew, user)
            onDone()
        }
    }

    fun deleteContract(contract: ContractEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value?.username ?: "admin"
            repository.deleteContract(contract.id, user)
            onDone()
        }
    }

    // -------------------------------------------------------------
    // Actions: Settings & Expense Categories
    // -------------------------------------------------------------
    fun saveExpenseCategory(cat: ExpenseCategoryEntity, isNew: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value?.username ?: "admin"
            repository.saveExpenseCategory(cat, isNew, user)
            onDone()
        }
    }

    fun deleteExpenseCategory(cat: ExpenseCategoryEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value?.username ?: "admin"
            repository.deleteExpenseCategory(cat.id, cat.title, user)
            onDone()
        }
    }

    fun setCurrencyUnit(unit: String) {
        viewModelScope.launch { preferences.setCurrencyUnit(unit) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setPrimaryColorKey(key: String) {
        viewModelScope.launch { preferences.setPrimaryColorKey(key) }
    }

    fun updateContractorInfo(info: ContractorInfo) {
        viewModelScope.launch { preferences.updateContractorInfo(info) }
    }

    fun updateSecuritySettings(
        pinEnabled: Boolean,
        biometricEnabled: Boolean,
        newPasswordRaw: String?,
        hint: String,
        fullName: String,
        username: String
    ) {
        viewModelScope.launch {
            preferences.setAuthEnabled(pinEnabled)
            preferences.setBiometricEnabled(biometricEnabled)
            val current = currentUser.value ?: UserEntity()
            val newHash = if (!newPasswordRaw.isNullOrBlank()) {
                SecurityUtils.hashPassword(newPasswordRaw)
            } else {
                current.passwordHash
            }
            val updated = current.copy(
                fullName = fullName,
                username = username,
                passwordHash = newHash,
                securityHint = hint,
                isPinEnabled = pinEnabled,
                isBiometricEnabled = biometricEnabled
            )
            repository.saveUser(updated)
        }
    }

    // -------------------------------------------------------------
    // Actions: Backup / Restore
    // -------------------------------------------------------------
    fun performBackup(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val file = BackupRestoreManager.createBackup(getApplication(), database)
                refreshBackupList()
                onResult(true, "فایل پشتیبان با موفقیت در مسیر ذیل ذخیره شد:\n${file.name}")
            } catch (e: Exception) {
                onResult(false, "خطا در تهیه پشتیبان: ${e.message}")
            }
        }
    }

    fun performRestore(filePath: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val file = File(filePath)
                val res = BackupRestoreManager.restoreBackup(database, file)
                if (res.isSuccess) {
                    onResult(true, "اطلاعات با موفقیت بازیابی شد.")
                } else {
                    onResult(false, res.exceptionOrNull()?.message ?: "خطا در بازیابی")
                }
            } catch (e: Exception) {
                onResult(false, "خطا در بازیابی: ${e.message}")
            }
        }
    }

    // -------------------------------------------------------------
    // Actions: Sample Data Management
    // -------------------------------------------------------------
    fun clearSampleData(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.clearSampleData()
            onDone()
        }
    }

    fun seedSampleData(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.seedSampleData()
            onDone()
        }
    }
}

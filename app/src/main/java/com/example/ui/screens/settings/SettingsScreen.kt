package com.example.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entities.*
import com.example.data.preferences.ContractorInfo
import com.example.ui.components.common.*
import com.example.ui.theme.*
import com.example.ui.viewmodels.SignageViewModel
import com.example.util.BackupMetadata
import com.example.util.FormatUtils
import com.example.util.JalaliDate
import java.util.UUID

enum class SettingsSubScreen {
    MAIN,
    EXPENSE_CATEGORIES,
    PROJECT_STAGES,
    BACKUP_RESTORE,
    DATABASE_MANAGEMENT
}

@Composable
fun SettingsScreen(
    viewModel: SignageViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currencyUnit by viewModel.currencyUnit.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val primaryColorKey by viewModel.primaryColorKey.collectAsState()
    val contractorInfo by viewModel.contractorInfo.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isAuthEnabled by viewModel.isAuthEnabled.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val categories by viewModel.expenseCategories.collectAsState()
    val backupList by viewModel.backupList.collectAsState()
    val stages by viewModel.stages.collectAsState()
    val trashItems by viewModel.trashItems.collectAsState()
    val recentAudits by viewModel.recentAudits.collectAsState()
    val warnUnsavedChanges by viewModel.warnUnsavedChanges.collectAsState()

    var currentSubScreen by remember { mutableStateOf(SettingsSubScreen.MAIN) }

    // Dialog & Sheet states
    var showContractorDialog by remember { mutableStateOf(false) }
    var showUserDialog by remember { mutableStateOf(false) }
    var showTrashDialog by remember { mutableStateOf(false) }
    var itemToRestore by remember { mutableStateOf<TrashItem?>(null) }
    var showAuditLogsDialog by remember { mutableStateOf(false) }

    when (currentSubScreen) {
        SettingsSubScreen.MAIN -> {
            SettingsContent(
                contractorInfo = contractorInfo,
                currencyUnit = currencyUnit,
                themeMode = themeMode,
                primaryColorKey = primaryColorKey,
                warnUnsavedChanges = warnUnsavedChanges,
                isAuthEnabled = isAuthEnabled,
                isBiometricEnabled = isBiometricEnabled,
                categoriesCount = categories.size,
                stagesCount = stages.size,
                backupCount = backupList.size,
                trashCount = trashItems.size,
                auditsCount = recentAudits.size,
                onBack = onBack,
                onEditContractor = { showContractorDialog = true },
                onSetCurrency = { viewModel.setCurrencyUnit(it) },
                onSetTheme = { viewModel.setThemeMode(it) },
                onSetPrimaryColor = { viewModel.setPrimaryColorKey(it) },
                onSetWarnUnsaved = { viewModel.setWarnUnsavedChanges(it) },
                onOpenExpenseCategories = { currentSubScreen = SettingsSubScreen.EXPENSE_CATEGORIES },
                onOpenProjectStages = { currentSubScreen = SettingsSubScreen.PROJECT_STAGES },
                onOpenTrash = { showTrashDialog = true },
                onOpenAuditLogs = { showAuditLogsDialog = true },
                onTogglePin = { checked ->
                    val user = currentUser
                    viewModel.updateSecuritySettings(
                        pinEnabled = checked,
                        biometricEnabled = isBiometricEnabled,
                        newPasswordRaw = null,
                        hint = user?.securityHint ?: "1234",
                        fullName = user?.fullName ?: "امیرحسین حسنخانی",
                        username = user?.username ?: "admin"
                    )
                },
                onToggleBiometric = { checked ->
                    val user = currentUser
                    viewModel.updateSecuritySettings(
                        pinEnabled = isAuthEnabled,
                        biometricEnabled = checked,
                        newPasswordRaw = null,
                        hint = user?.securityHint ?: "1234",
                        fullName = user?.fullName ?: "امیرحسین حسنخانی",
                        username = user?.username ?: "admin"
                    )
                },
                onEditUserSecurity = { showUserDialog = true },
                onOpenBackupRestore = { currentSubScreen = SettingsSubScreen.BACKUP_RESTORE },
                onOpenDatabaseManagement = { currentSubScreen = SettingsSubScreen.DATABASE_MANAGEMENT }
            )
        }
        SettingsSubScreen.EXPENSE_CATEGORIES -> {
            ExpenseCategoriesSettingsScreen(
                categories = categories,
                currencyUnit = currencyUnit,
                onBack = { currentSubScreen = SettingsSubScreen.MAIN },
                onSaveCategory = { entity, isNew ->
                    viewModel.saveExpenseCategory(entity, isNew) {}
                },
                onDeleteCategory = { entity ->
                    viewModel.deleteExpenseCategory(entity) {}
                },
                warnUnsavedChanges = warnUnsavedChanges
            )
        }
        SettingsSubScreen.PROJECT_STAGES -> {
            ProjectStagesSettingsScreen(
                stages = stages,
                onBack = { currentSubScreen = SettingsSubScreen.MAIN },
                onSaveStage = { entity, isNew ->
                    viewModel.saveStage(entity, isNew) {}
                },
                onDeleteStage = { entity ->
                    viewModel.deleteStage(entity) {}
                },
                onReorderStages = { reordered ->
                    viewModel.reorderStages(reordered)
                },
                warnUnsavedChanges = warnUnsavedChanges
            )
        }
        SettingsSubScreen.BACKUP_RESTORE -> {
            BackupRestoreSettingsScreen(
                backupList = backupList,
                onBack = { currentSubScreen = SettingsSubScreen.MAIN },
                onCreateBackup = {
                    viewModel.performBackup { success, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                },
                onRestoreBackup = { b ->
                    viewModel.performRestore(b.filePath) { success, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
        SettingsSubScreen.DATABASE_MANAGEMENT -> {
            DatabaseManagementSettingsScreen(
                onBack = { currentSubScreen = SettingsSubScreen.MAIN },
                onClearSampleData = {
                    viewModel.clearSampleData {
                        Toast.makeText(context, "اطلاعات فرضی با موفقیت پاک شد و دیتابیس آماده کار است.", Toast.LENGTH_LONG).show()
                    }
                },
                onSeedSampleData = {
                    viewModel.seedSampleData {
                        Toast.makeText(context, "اطلاعات فرضی تابلوسازی با موفقیت بارگذاری شد.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    // Contractor Info Dialog
    if (showContractorDialog) {
        ContractorEditDialog(
            currentInfo = contractorInfo,
            warnUnsavedChanges = warnUnsavedChanges,
            onDismiss = { showContractorDialog = false },
            onSave = { updated ->
                viewModel.updateContractorInfo(updated)
                showContractorDialog = false
                Toast.makeText(context, "مشخصات تابلوسازی ذخیره شد.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // User Profile Dialog
    if (showUserDialog) {
        UserProfileDialog(
            currentUser = currentUser,
            warnUnsavedChanges = warnUnsavedChanges,
            onDismiss = { showUserDialog = false },
            onSave = { name, user, newPass, hint ->
                viewModel.updateSecuritySettings(
                    pinEnabled = isAuthEnabled,
                    biometricEnabled = isBiometricEnabled,
                    newPasswordRaw = newPass,
                    hint = hint,
                    fullName = name,
                    username = user
                )
                showUserDialog = false
                Toast.makeText(context, "اطلاعات امنیتی بروزرسانی شد.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Trash Dialog
    if (showTrashDialog) {
        TrashDialog(
            trashItems = trashItems,
            onDismiss = { showTrashDialog = false },
            onRequestRestore = { item -> itemToRestore = item }
        )
    }

    // Pre-restore confirmation dialog (Rule: confirm before restore)
    DeleteConfirmationDialog(
        show = itemToRestore != null,
        title = "تأیید بازیابی از سطل زباله",
        message = "آیا از بازیابی «${itemToRestore?.title}» (${itemToRestore?.categoryLabel}) به فهرست فعال اطمینان دارید؟",
        confirmButtonText = "بازیابی",
        onConfirm = {
            itemToRestore?.let { itm ->
                viewModel.restoreTrashItem(itm) {
                    itemToRestore = null
                    Toast.makeText(context, "با موفقیت بازیابی شد.", Toast.LENGTH_SHORT).show()
                }
            }
        },
        onDismiss = { itemToRestore = null }
    )

    // Audit Logs Dialog
    if (showAuditLogsDialog) {
        AuditLogsDialog(
            logs = recentAudits,
            onDismiss = { showAuditLogsDialog = false }
        )
    }
}

@Composable
fun SettingsContent(
    contractorInfo: ContractorInfo,
    currencyUnit: String,
    themeMode: String,
    primaryColorKey: String,
    warnUnsavedChanges: Boolean,
    isAuthEnabled: Boolean,
    isBiometricEnabled: Boolean,
    categoriesCount: Int,
    stagesCount: Int,
    backupCount: Int,
    trashCount: Int,
    auditsCount: Int,
    onBack: () -> Unit,
    onEditContractor: () -> Unit,
    onSetCurrency: (String) -> Unit,
    onSetTheme: (String) -> Unit,
    onSetPrimaryColor: (String) -> Unit,
    onSetWarnUnsaved: (Boolean) -> Unit,
    onOpenExpenseCategories: () -> Unit,
    onOpenProjectStages: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenAuditLogs: () -> Unit,
    onTogglePin: (Boolean) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onEditUserSecurity: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onOpenDatabaseManagement: () -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "تنظیمات نرم‌افزار",
                subtitle = "پیکربندی تابلوسازی، امنیت، تم و پشتیبان‌گیری",
                onBack = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Contractor & Business Details
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("اطلاعات پیمانکار و کارگاه تابلوسازی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                AppCard {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(contractorInfo.businessName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("مدیریت: ${contractorInfo.name} • ${contractorInfo.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("نشانی: ${contractorInfo.address}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("کارت: ${contractorInfo.cardNumber} (بانک ملی)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = onEditContractor, modifier = Modifier.testTag("edit_contractor_btn")) {
                                Icon(Icons.Default.Edit, contentDescription = "ویرایش مشخصات", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // Section 2: Currency & Appearance (Theme, Colors)
            item {
                Text("واحد پول و ظاهر برنامه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                AppCard {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Currency Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("واحد پول محاسبات:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = currencyUnit == "TOMAN",
                                    onClick = { onSetCurrency("TOMAN") },
                                    label = { Text("تومان") }
                                )
                                FilterChip(
                                    selected = currencyUnit == "RIAL",
                                    onClick = { onSetCurrency("RIAL") },
                                    label = { Text("ریال") }
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Theme Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("حالت تم:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = themeMode == "SYSTEM",
                                    onClick = { onSetTheme("SYSTEM") },
                                    label = { Text("سیستم") }
                                )
                                FilterChip(
                                    selected = themeMode == "LIGHT",
                                    onClick = { onSetTheme("LIGHT") },
                                    label = { Text("روشن") }
                                )
                                FilterChip(
                                    selected = themeMode == "DARK",
                                    onClick = { onSetTheme("DARK") },
                                    label = { Text("تاریک") }
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Primary Color Palette
                        Column {
                            Text("رنگ اصلی برنامه:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(10.dp))
                            val colorOptions = listOf(
                                "AMBER" to ("کهربایی" to PrimaryAmber),
                                "BLUE" to ("آبی" to PrimaryBlue),
                                "CYAN" to ("فیروزه‌ای" to PrimaryCyan),
                                "EMERALD" to ("زمردی" to PrimaryEmerald),
                                "ROSE" to ("زرشکی" to PrimaryRose),
                                "PURPLE" to ("بنفش" to PrimaryPurple)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                colorOptions.forEach { (key, pair) ->
                                    val (label, col) = pair
                                    val isSelected = primaryColorKey == key
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(col)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else col.copy(alpha = 0.5f),
                                                shape = CircleShape
                                            )
                                            .clickable { onSetPrimaryColor(key) }
                                            .testTag("color_circle_$key"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "انتخاب شده: $label",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Warn on unsaved changes switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("هشدار تغییرات ذخیره‌نشده", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("هنگام خروج از فرم‌ها قبل از ذخیره، پیام هشدار نمایش داده شود", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = warnUnsavedChanges,
                                onCheckedChange = onSetWarnUnsaved,
                                modifier = Modifier.testTag("switch_warn_unsaved")
                            )
                        }
                    }
                }
            }

            // Section 3: Workshop Management (Categories & Stages) - Accordion / Row navigation per Agreement 1.1
            item {
                Text("مدیریت کارگاه و تابلوسازی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                AppCard {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Expense Categories
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenExpenseCategories() }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("مدیریت عنوان هزینه‌ها", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("پیکربندی عناوین هزینه‌های کارگاهی و مبالغ پیش‌فرض ($categoriesCount عنوان)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Project Stages
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenProjectStages() }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Timeline, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("مدیریت مراحل پروژه", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("تنظیم مراحل پیشرفت کارگاه، ترتیب و نشانگرهای رنگی ($stagesCount مرحله)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Section 4: Trash & Activity Log
            item {
                Text("سطل زباله و سابقه فعالیت‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                AppCard {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Trash Option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenTrash() }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("سطل زباله", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("مشاهده و بازیابی موارد حذف‌شده ($trashCount مورد)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Audit Log Option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenAuditLogs() }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("سابقه فعالیت‌ها و رویدادها", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("مشاهده آخرین تغییرات و ثبت اطلاعات ($auditsCount رویداد)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Section 5: Security & User Profile (PIN / Biometric)
            item {
                Text("امنیت و کنترل دسترسی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                AppCard {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("قفل ورود به برنامه با رمز / PIN", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("در صورت فعال‌بودن، هنگام باز شدن برنامه رمز خواسته می‌شود", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = isAuthEnabled,
                                onCheckedChange = onTogglePin
                            )
                        }

                        if (isAuthEnabled) {
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("ورود با اثر انگشت (بیومتریک)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text("استفاده از حسگر اثر انگشت گوشی برای ورود سریع", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = isBiometricEnabled,
                                    onCheckedChange = onToggleBiometric
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Button(
                            onClick = onEditUserSecurity,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("edit_security_profile_btn")
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تغییر رمز عبور و راهنمای یادآوری (Hint)")
                        }
                    }
                }
            }

            // Section 6: Backup & Database Management - Row navigation per Agreement 1.1
            item {
                Text("پشتیبان‌گیری و پایگاه داده", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                AppCard {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Backup & Restore
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenBackupRestore() }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("پشتیبان‌گیری و بازیابی اطلاعات", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("تهیه فایل پشتیبان JSON و بازیابی اطلاعات کارگاه ($backupCount فایل موجود)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Database & Sample Data
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenDatabaseManagement() }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("مدیریت دیتابیس و داده‌های فرضی", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("پاک‌سازی اطلاعات فرضی و راه‌اندازی برای کار واقعی کارگاه", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractorEditDialog(
    currentInfo: ContractorInfo,
    onDismiss: () -> Unit,
    onSave: (ContractorInfo) -> Unit,
    warnUnsavedChanges: Boolean = true
) {
    var name by remember { mutableStateOf(currentInfo.name) }
    var businessName by remember { mutableStateOf(currentInfo.businessName) }
    var address by remember { mutableStateOf(currentInfo.address) }
    var phone by remember { mutableStateOf(currentInfo.phone) }
    var accountNumber by remember { mutableStateOf(currentInfo.accountNumber) }
    var shabaNumber by remember { mutableStateOf(currentInfo.shabaNumber) }
    var cardNumber by remember { mutableStateOf(currentInfo.cardNumber) }
    var bankName by remember { mutableStateOf(currentInfo.bankName) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    val isDirty = remember(name, businessName, address, phone, accountNumber, shabaNumber, cardNumber, bankName) {
        name != currentInfo.name ||
                businessName != currentInfo.businessName ||
                address != currentInfo.address ||
                phone != currentInfo.phone ||
                accountNumber != currentInfo.accountNumber ||
                shabaNumber != currentInfo.shabaNumber ||
                cardNumber != currentInfo.cardNumber ||
                bankName != currentInfo.bankName
    }

    val attemptDismiss = {
        if (warnUnsavedChanges && isDirty) {
            showUnsavedDialog = true
        } else {
            onDismiss()
        }
    }

    val executeSave = {
        onSave(
            currentInfo.copy(
                name = name.trim(),
                businessName = businessName.trim(),
                address = address.trim(),
                phone = phone.trim(),
                accountNumber = accountNumber.trim(),
                shabaNumber = shabaNumber.trim(),
                cardNumber = cardNumber.trim(),
                bankName = bankName.trim()
            )
        )
    }

    AppFormBottomSheet(
        title = "مشخصات پیمانکار و کارگاه",
        subtitle = "اطلاعات ثبتی، نشانی کارگاه و شماره حساب‌های بانکی",
        onDismissRequest = onDismiss,
        isDirty = isDirty,
        warnUnsavedChanges = warnUnsavedChanges,
        onAttemptDismiss = attemptDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PersianTextField(value = name, onValueChange = { name = it }, label = "نام پیمانکار")
            PersianTextField(value = businessName, onValueChange = { businessName = it }, label = "نام مجموعه یا کارگاه")
            PersianTextField(value = phone, onValueChange = { phone = it }, label = "تلفن تماس")
            PersianTextField(value = address, onValueChange = { address = it }, label = "نشانی کارگاه")
            PersianTextField(value = accountNumber, onValueChange = { accountNumber = it }, label = "شماره حساب بانک ملی")
            PersianTextField(value = cardNumber, onValueChange = { cardNumber = it }, label = "شماره کارت")
            PersianTextField(value = shabaNumber, onValueChange = { shabaNumber = it }, label = "شماره شبا (IR)")
        }

        StickyBottomActionBar(
            entityName = "مشخصات پیمانکار",
            saveButtonText = "ذخیره مشخصات پیمانکار",
            onSave = executeSave,
            onCancel = onDismiss,
            isDirty = isDirty,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showUnsavedDialog) {
        UnsavedChangesDialog(
            show = true,
            onSaveAndExit = {
                showUnsavedDialog = false
                executeSave()
            },
            onDiscardAndExit = {
                showUnsavedDialog = false
                onDismiss()
            },
            onDismiss = { showUnsavedDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileDialog(
    currentUser: com.example.data.local.entities.UserEntity?,
    onDismiss: () -> Unit,
    onSave: (fullName: String, username: String, newPass: String?, hint: String) -> Unit,
    warnUnsavedChanges: Boolean = true
) {
    var fullName by remember { mutableStateOf(currentUser?.fullName ?: "امیرحسین حسنخانی") }
    var username by remember { mutableStateOf(currentUser?.username ?: "admin") }
    var newPassword by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf(currentUser?.securityHint ?: "رمز پیش‌فرض: 1234") }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    val isDirty = remember(fullName, username, newPassword, hint) {
        fullName != (currentUser?.fullName ?: "") ||
                username != (currentUser?.username ?: "") ||
                newPassword.isNotBlank() ||
                hint != (currentUser?.securityHint ?: "")
    }

    val attemptDismiss = {
        if (warnUnsavedChanges && isDirty) {
            showUnsavedDialog = true
        } else {
            onDismiss()
        }
    }

    val executeSave = {
        onSave(
            fullName.trim(),
            username.trim(),
            if (newPassword.isNotBlank()) newPassword.trim() else null,
            hint.trim()
        )
    }

    AppFormBottomSheet(
        title = "تنظیمات رمز عبور و پروفایل",
        subtitle = "مدیریت حساب کاربری ادمین و راهنمای یادآوری رمز",
        onDismissRequest = onDismiss,
        isDirty = isDirty,
        warnUnsavedChanges = warnUnsavedChanges,
        onAttemptDismiss = attemptDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PersianTextField(value = fullName, onValueChange = { fullName = it }, label = "نام کاربر")
            PersianTextField(value = username, onValueChange = { username = it }, label = "نام کاربری")
            PersianTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = "رمز عبور جدید (در صورت تمایل به تغییر)",
                placeholder = "خالی بگذارید اگر تغییر نمی‌کند"
            )
            PersianTextField(
                value = hint,
                onValueChange = { hint = it },
                label = "راهنمای یادآوری رمز (Hint)",
                placeholder = "متنی که در صورت فراموشی به عنوان راهنما نشان داده می‌شود"
            )
        }

        StickyBottomActionBar(
            entityName = "اطلاعات کاربر",
            saveButtonText = "ذخیره اطلاعات کاربر",
            onSave = executeSave,
            onCancel = onDismiss,
            isDirty = isDirty,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showUnsavedDialog) {
        UnsavedChangesDialog(
            show = true,
            onSaveAndExit = {
                showUnsavedDialog = false
                executeSave()
            },
            onDiscardAndExit = {
                showUnsavedDialog = false
                onDismiss()
            },
            onDismiss = { showUnsavedDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashDialog(
    trashItems: List<TrashItem>,
    onDismiss: () -> Unit,
    onRequestRestore: (TrashItem) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    AppFormBottomSheet(
        title = "سطل زباله",
        subtitle = "${trashItems.size} مورد حذف‌شده قابل بازیابی",
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("فهرست یکپارچه") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("گروه‌بندی دسته‌ها") }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (trashItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "سطل زباله خالی است.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (selectedTab == 0) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                ) {
                    items(trashItems, key = { it.id }) { item ->
                        TrashItemRow(item = item, onRestore = { onRequestRestore(item) })
                    }
                }
            } else {
                val grouped = remember(trashItems) { trashItems.groupBy { it.categoryLabel } }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                ) {
                    grouped.forEach { (categoryName, items) ->
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "$categoryName (${items.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                        items(items, key = { it.id }) { item ->
                            TrashItemRow(item = item, onRestore = { onRequestRestore(item) })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text("بستن")
            }
        }
    }
}

@Composable
fun TrashItemRow(
    item: TrashItem,
    onRestore: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.categoryLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(item.details, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            FilledTonalButton(
                onClick = onRestore,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("بازیابی", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogsDialog(
    logs: List<com.example.data.local.entities.AuditLogEntity>,
    onDismiss: () -> Unit
) {
    AppFormBottomSheet(
        title = "سابقه فعالیت‌ها و رویدادها",
        subtitle = "${logs.size} رویداد ثبت‌شده در سیستم",
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("هیچ رویدادی ثبت نشده است.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs, key = { it.id }) { log ->
                        val actionColor = when (log.action) {
                            "INSERT" -> MoneyPositive
                            "UPDATE" -> MaterialTheme.colorScheme.primary
                            "DELETE" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.outline
                        }
                        val actionLabel = when (log.action) {
                            "INSERT" -> "ثبت جدید"
                            "UPDATE" -> "ویرایش"
                            "DELETE" -> "حذف"
                            "RESTORE" -> "بازیابی"
                            else -> log.action
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = actionColor.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = actionLabel,
                                                color = actionColor,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(log.entityType, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                    if (log.details.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(log.details, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Text(
                                    text = JalaliDate.fromMillis(log.timestamp).formatShort(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text("بستن")
            }
        }
    }
}

@Preview(name = "Contractor Dialog Preview", showBackground = true)
@Composable
fun ContractorEditDialogPreview() {
    MyApplicationTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ContractorEditDialog(
                currentInfo = ContractorInfo(
                    name = "امیرحسین حسنخانی",
                    businessName = "تابلوسازی آلپا",
                    address = "تهران، شهرک صنعتی چهاردانگه",
                    phone = "09121234567"
                ),
                onDismiss = {},
                onSave = {}
            )
        }
    }
}

@Preview(name = "Settings Screen Full Preview", showBackground = true)
@Composable
fun SettingsScreenPreview() {
    MyApplicationTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsContent(
                contractorInfo = ContractorInfo(
                    name = "امیرحسین حسنخانی",
                    businessName = "تابلوسازی تبلیغاتی آلپا",
                    address = "فردوسی ۲، نبش خیابان فاضل",
                    phone = "09144862950",
                    cardNumber = "6037998209583854"
                ),
                currencyUnit = "TOMAN",
                themeMode = "SYSTEM",
                primaryColorKey = "AMBER",
                warnUnsavedChanges = true,
                isAuthEnabled = true,
                isBiometricEnabled = false,
                categoriesCount = 8,
                stagesCount = 5,
                backupCount = 3,
                trashCount = 2,
                auditsCount = 14,
                onBack = {},
                onEditContractor = {},
                onSetCurrency = {},
                onSetTheme = {},
                onSetPrimaryColor = {},
                onSetWarnUnsaved = {},
                onOpenExpenseCategories = {},
                onOpenProjectStages = {},
                onOpenTrash = {},
                onOpenAuditLogs = {},
                onTogglePin = {},
                onToggleBiometric = {},
                onEditUserSecurity = {},
                onOpenBackupRestore = {},
                onOpenDatabaseManagement = {}
            )
        }
    }
}




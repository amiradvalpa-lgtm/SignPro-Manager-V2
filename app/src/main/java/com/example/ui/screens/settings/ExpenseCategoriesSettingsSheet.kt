package com.example.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.data.local.entities.ExpenseCategoryEntity
import com.example.ui.components.common.*
import com.example.ui.theme.MyApplicationTheme
import com.example.util.FormatUtils
import java.util.UUID

/**
 * صفحه تمام‌صفحه اختصاصی مدیریت عنوان هزینه‌ها بر اساس بند ۱.۱ و ۱.۲ توافقنامه
 */
@Composable
fun ExpenseCategoriesSettingsScreen(
    categories: List<ExpenseCategoryEntity>,
    currencyUnit: String,
    onBack: () -> Unit,
    onSaveCategory: (ExpenseCategoryEntity, Boolean) -> Unit,
    onDeleteCategory: (ExpenseCategoryEntity) -> Unit,
    warnUnsavedChanges: Boolean = true
) {
    BackHandler { onBack() }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCategoryForEdit by remember { mutableStateOf<ExpenseCategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<ExpenseCategoryEntity?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "مدیریت عنوان هزینه‌ها",
                subtitle = "پیکربندی عناوین هزینه‌های کارگاهی و مبالغ پیش‌فرض",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "عناوین ثبت‌شده (${categories.size} مورد)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FilledTonalButton(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_expense_category_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("افزودن عنوان جدید", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (categories.isEmpty()) {
                EmptyStateView(
                    title = "عنوان هزینه‌ای تعریف نشده است",
                    message = "برای تعریف عنوان‌های پرکاربرد کارگاه روی دکمه «افزودن عنوان جدید» کلیک کنید.",
                    icon = Icons.Default.Category,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categories, key = { it.id }) { cat ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cat.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (cat.defaultAmount > 0) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "مبلغ پیش‌فرض: ${FormatUtils.formatCurrency(cat.defaultAmount, currencyUnit)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (cat.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = cat.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { selectedCategoryForEdit = cat },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "ویرایش عنوان",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { categoryToDelete = cat },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "حذف عنوان",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        ExpenseCategoryDialog(
            category = null,
            currencyUnit = currencyUnit,
            warnUnsavedChanges = warnUnsavedChanges,
            onDismiss = { showAddDialog = false },
            onSave = { entity, isNew ->
                onSaveCategory(entity, isNew)
                showAddDialog = false
            }
        )
    }

    // Edit dialog
    selectedCategoryForEdit?.let { cat ->
        ExpenseCategoryDialog(
            category = cat,
            currencyUnit = currencyUnit,
            warnUnsavedChanges = warnUnsavedChanges,
            onDismiss = { selectedCategoryForEdit = null },
            onSave = { entity, isNew ->
                onSaveCategory(entity, isNew)
                selectedCategoryForEdit = null
            }
        )
    }

    // Delete dialog
    DeleteConfirmationDialog(
        show = categoryToDelete != null,
        title = "حذف عنوان هزینه",
        message = "آیا از حذف عنوان هزینه «${categoryToDelete?.title}» اطمینان دارید؟",
        onConfirm = {
            categoryToDelete?.let { cat ->
                onDeleteCategory(cat)
                categoryToDelete = null
            }
        },
        onDismiss = { categoryToDelete = null }
    )
}

/**
 * مودال افزودن و ویرایش عنوان هزینه (مستقر در فایل اختصاصی تنظیمات هزینه طبق اصل ۱.۱ توافقنامه)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseCategoryDialog(
    category: ExpenseCategoryEntity?,
    currencyUnit: String,
    onDismiss: () -> Unit,
    onSave: (ExpenseCategoryEntity, Boolean) -> Unit,
    warnUnsavedChanges: Boolean = true
) {
    val isNew = category == null
    var title by remember { mutableStateOf(category?.title ?: "") }
    var defaultAmount by remember { mutableStateOf(category?.defaultAmount ?: 0L) }
    var description by remember { mutableStateOf(category?.description ?: "") }
    var titleError by remember { mutableStateOf<String?>(null) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    val isDirty = remember(title, defaultAmount, description) {
        if (isNew) {
            title.isNotBlank() || defaultAmount > 0L || description.isNotBlank()
        } else {
            title != (category?.title ?: "") ||
                    defaultAmount != (category?.defaultAmount ?: 0L) ||
                    description != (category?.description ?: "")
        }
    }

    val attemptDismiss = {
        if (warnUnsavedChanges && isDirty) {
            showUnsavedDialog = true
        } else {
            onDismiss()
        }
    }

    val executeSave = {
        if (title.trim().isEmpty()) {
            titleError = "عنوان هزینه الزامی است."
        } else {
            val entity = category?.copy(
                title = title.trim(),
                defaultAmount = defaultAmount,
                description = description.trim()
            ) ?: ExpenseCategoryEntity(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                defaultAmount = defaultAmount,
                description = description.trim()
            )
            onSave(entity, isNew)
        }
    }

    AppFormBottomSheet(
        title = if (isNew) "افزودن عنوان هزینه جدید" else "ویرایش عنوان هزینه",
        subtitle = "تعریف عناوین پرکاربرد هزینه‌های کارگاهی و مبلغ پیش‌فرض",
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
            PersianTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = null
                },
                label = "عنوان هزینه",
                placeholder = "مثال: کرایه بالابر، ورق کامپوزیت",
                isError = titleError != null,
                errorMessage = titleError
            )
            CurrencyInputField(
                amount = defaultAmount,
                onAmountChange = { defaultAmount = it },
                label = "مبلغ پیش‌فرض (اختیاری)",
                currencyUnit = currencyUnit
            )
            PersianTextField(
                value = description,
                onValueChange = { description = it },
                label = "توضیحات",
                placeholder = "توضیح اختیاری در مورد این نوع هزینه"
            )
        }

        StickyBottomActionBar(
            entityName = "عنوان هزینه",
            saveButtonText = "ذخیره عنوان هزینه",
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

/**
 * سازگاری نام قبلی جهت جلوگیری از شکست وابستگی‌ها
 */
@Composable
fun ExpenseCategoriesSettingsSheet(
    categories: List<ExpenseCategoryEntity>,
    currencyUnit: String,
    onDismiss: () -> Unit,
    onSaveCategory: (ExpenseCategoryEntity, Boolean) -> Unit,
    onDeleteCategory: (ExpenseCategoryEntity) -> Unit,
    warnUnsavedChanges: Boolean = true
) {
    ExpenseCategoriesSettingsScreen(
        categories = categories,
        currencyUnit = currencyUnit,
        onBack = onDismiss,
        onSaveCategory = onSaveCategory,
        onDeleteCategory = onDeleteCategory,
        warnUnsavedChanges = warnUnsavedChanges
    )
}

@Preview(name = "Expense Categories Screen Preview", showBackground = true)
@Composable
fun ExpenseCategoriesSettingsScreenPreview() {
    MyApplicationTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ExpenseCategoriesSettingsScreen(
                categories = listOf(
                    ExpenseCategoryEntity(
                        id = "1",
                        title = "ورق کامپوزیت آلومینیوم",
                        defaultAmount = 1450000L,
                        description = "ورق ضخامت ۰.۳ و ۰.۴ میلیمتر برای زیرسازی"
                    ),
                    ExpenseCategoryEntity(
                        id = "2",
                        title = "پلکسی گلاس تایوانی",
                        defaultAmount = 2800000L,
                        description = "رنگ‌های شیری، دوغی و طلایی"
                    ),
                    ExpenseCategoryEntity(
                        id = "3",
                        title = "کرایه بالابر و داربست",
                        defaultAmount = 1800000L,
                        description = "نصب حروف در ارتفاع بالای ۴ متر"
                    )
                ),
                currencyUnit = "TOMAN",
                onBack = {},
                onSaveCategory = { _, _ -> },
                onDeleteCategory = {}
            )
        }
    }
}

package com.example.ui.screens.settings

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
 * فرم اختصاصی مدیریت عنوان هزینه‌ها بر اساس بند ۱.۱ و ۱.۲ توافقنامه
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseCategoriesSettingsSheet(
    categories: List<ExpenseCategoryEntity>,
    currencyUnit: String,
    onDismiss: () -> Unit,
    onSaveCategory: (ExpenseCategoryEntity, Boolean) -> Unit,
    onDeleteCategory: (ExpenseCategoryEntity) -> Unit,
    warnUnsavedChanges: Boolean = true
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCategoryForEdit by remember { mutableStateOf<ExpenseCategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<ExpenseCategoryEntity?>(null) }

    AppFormBottomSheet(
        title = "مدیریت عنوان هزینه‌ها",
        subtitle = "پیکربندی عناوین هزینه‌های کارگاهی و مبالغ پیش‌فرض",
        onDismissRequest = onDismiss,
        isDirty = false,
        warnUnsavedChanges = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "عناوین ثبت‌شده (${categories.size} مورد)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FilledTonalButton(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_expense_category_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("افزودن عنوان جدید", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (categories.isEmpty()) {
                EmptyStateView(
                    title = "عنوان هزینه‌ای تعریف نشده است",
                    message = "برای تعریف عنوان‌های پرتکرار کارگاه روی دکمه افزودن عنوان جدید کلیک کنید.",
                    icon = Icons.Default.Category,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories, key = { it.id }) { cat ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cat.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (cat.defaultAmount > 0) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "مبلغ پیش‌فرض: ${FormatUtils.formatCurrency(cat.defaultAmount, currencyUnit)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (cat.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = cat.description,
                                            style = MaterialTheme.typography.bodySmall,
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
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("close_categories_sheet_btn")
            ) {
                Text("بستن")
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

    // Delete confirmation dialog
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

@Preview(name = "Expense Categories Sheet Preview", showBackground = true)
@Composable
fun ExpenseCategoriesSettingsSheetPreview() {
    MyApplicationTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ExpenseCategoriesSettingsSheet(
                categories = listOf(
                    ExpenseCategoryEntity(
                        id = "1",
                        title = "ورق کامپوزیت",
                        defaultAmount = 4500000L,
                        description = "خرید ورق از بازار آهن"
                    ),
                    ExpenseCategoryEntity(
                        id = "2",
                        title = "کرایه جرثقیل و بالابر",
                        defaultAmount = 1800000L,
                        description = "هزینه نصب در ارتفاع"
                    ),
                    ExpenseCategoryEntity(
                        id = "3",
                        title = "ال‌ای‌دی SMD بلوکی",
                        defaultAmount = 850000L,
                        description = "روشنایی حروف برجسته"
                    )
                ),
                currencyUnit = "تومان",
                onDismiss = {},
                onSaveCategory = { _, _ -> },
                onDeleteCategory = {}
            )
        }
    }
}

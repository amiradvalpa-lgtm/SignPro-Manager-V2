package com.example.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.components.common.*
import com.example.ui.theme.MyApplicationTheme

/**
 * فرم اختصاصی مدیریت دیتابیس و داده‌های فرضی کارگاه بر اساس بند ۱.۱ و ۱.۲ توافقنامه
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseManagementSettingsSheet(
    onDismiss: () -> Unit,
    onClearSampleData: () -> Unit,
    onSeedSampleData: () -> Unit
) {
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showSeedConfirmDialog by remember { mutableStateOf(false) }

    AppFormBottomSheet(
        title = "مدیریت دیتابیس و داده‌های فرضی",
        subtitle = "پاک‌سازی داده‌های نمونه، آماده‌سازی کارگاه و لود اطلاعات تستی",
        onDismissRequest = onDismiss,
        isDirty = false,
        warnUnsavedChanges = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "وضعیت دیتابیس و آماده‌سازی برای کار واقعی",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "نرم‌افزار با اطلاعات فرضی جامع (شامل مشتریان، سفارشات چلنیوم و کامپوزیت، فاکتورها، چک‌ها و قراردادها) راه‌اندازی شده است تا بتوانید گزارشات و محیط برنامه را قبل از ثبت کار واقعی تست کنید.\n\nبرای شروع کار واقعی کارگاه تابلوسازی، دکمه «پاک‌سازی اطلاعات فرضی» را بزنید تا دیتابیس کاملاً سفید و آماده ثبت سفارشات واقعی شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "نکته: با پاک‌سازی اطلاعات فرضی، نام کارگاه، حساب کاربری و دسته‌بندی‌های هزینه پاک نمی‌شوند و محفوظ می‌مانند.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Button(
                onClick = { showClearConfirmDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("clear_sample_data_sheet_btn")
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("پاک‌سازی اطلاعات فرضی و راه‌اندازی کارگاه", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { showSeedConfirmDialog = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("seed_sample_data_sheet_btn")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("بارگذاری مجدد اطلاعات نمونه کارگاه")
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("close_database_sheet_btn")
            ) {
                Text("بستن")
            }
        }
    }

    // Clear confirmation dialog
    DeleteConfirmationDialog(
        show = showClearConfirmDialog,
        title = "پاک‌سازی اطلاعات فرضی و راه‌اندازی کارگاه",
        message = "آیا مطمئن هستید؟ تمامی داده‌های تستی شامل مشتریان، پروژه‌ها، فاکتورها، چک‌ها و قراردادهای فرضی حذف خواهند شد و دیتابیس برای ورود اطلاعات واقعی کارگاه آماده می‌شود.\n(اطلاعات حساب کاربری، مشخصات کارگاه و دسته‌های هزینه محفوظ می‌مانند)",
        confirmButtonText = "پاک‌سازی کامل داده‌های تستی",
        onConfirm = {
            showClearConfirmDialog = false
            onClearSampleData()
        },
        onDismiss = { showClearConfirmDialog = false }
    )

    // Seed confirmation dialog
    DeleteConfirmationDialog(
        show = showSeedConfirmDialog,
        title = "بارگذاری مجدد اطلاعات نمونه",
        message = "آیا می‌خواهید داده‌های فرضی تابلوسازی (۳ مشتری، ۳ سفارش، فاکتورها، هزینه‌ها و چک‌ها) مجدداً در دیتابیس بارگذاری شوند؟",
        confirmButtonText = "بارگذاری نمونه‌ها",
        onConfirm = {
            showSeedConfirmDialog = false
            onSeedSampleData()
        },
        onDismiss = { showSeedConfirmDialog = false }
    )
}

@Preview(name = "Database Management Sheet Preview", showBackground = true)
@Composable
fun DatabaseManagementSettingsSheetPreview() {
    MyApplicationTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            DatabaseManagementSettingsSheet(
                onDismiss = {},
                onClearSampleData = {},
                onSeedSampleData = {}
            )
        }
    }
}

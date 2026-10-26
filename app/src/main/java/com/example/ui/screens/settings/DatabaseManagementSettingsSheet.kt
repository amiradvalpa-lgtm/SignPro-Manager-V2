package com.example.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
 * صفحه تمام‌صفحه اختصاصی مدیریت دیتابیس و داده‌های فرضی کارگاه بر اساس بند ۱.۱ و ۱.۲ توافقنامه
 */
@Composable
fun DatabaseManagementSettingsScreen(
    onBack: () -> Unit,
    onClearSampleData: () -> Unit,
    onSeedSampleData: () -> Unit
) {
    BackHandler { onBack() }

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showSeedConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "مدیریت دیتابیس و داده‌های فرضی",
                subtitle = "پاک‌سازی داده‌های نمونه، آماده‌سازی کارگاه و لود اطلاعات تستی",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "وضعیت دیتابیس و آماده‌سازی برای کار واقعی",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "نرم‌افزار با اطلاعات فرضی جامع (شامل مشتریان، سفارشات چلنیوم و کامپوزیت، فاکتورها، چک‌ها و قراردادها) راه‌اندازی شده است تا بتوانید گزارشات و محیط برنامه را قبل از ثبت کار واقعی تست و بررسی کنید.\n\nبرای شروع کار واقعی کارگاه تابلوسازی، دکمه «پاک‌سازی اطلاعات فرضی» را بزنید تا دیتابیس کاملاً سفید و آماده ثبت سفارشات واقعی شما شود.",
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
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "نکته مهم: با پاک‌سازی اطلاعات فرضی، نام کارگاه، حساب کاربری و دسته‌بندی‌های هزینه پاک نمی‌شوند و محفوظ می‌مانند.",
                        style = MaterialTheme.typography.bodySmall,
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
                    .height(48.dp)
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
                    .height(48.dp)
                    .testTag("seed_sample_data_sheet_btn")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("بارگذاری مجدد اطلاعات نمونه کارگاه", fontWeight = FontWeight.SemiBold)
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

/**
 * سازگاری نام قبلی جهت جلوگیری از شکست وابستگی‌ها
 */
@Composable
fun DatabaseManagementSettingsSheet(
    onDismiss: () -> Unit,
    onClearSampleData: () -> Unit,
    onSeedSampleData: () -> Unit
) {
    DatabaseManagementSettingsScreen(
        onBack = onDismiss,
        onClearSampleData = onClearSampleData,
        onSeedSampleData = onSeedSampleData
    )
}

@Preview(name = "Database Management Screen Preview", showBackground = true)
@Composable
fun DatabaseManagementSettingsScreenPreview() {
    MyApplicationTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            DatabaseManagementSettingsScreen(
                onBack = {},
                onClearSampleData = {},
                onSeedSampleData = {}
            )
        }
    }
}

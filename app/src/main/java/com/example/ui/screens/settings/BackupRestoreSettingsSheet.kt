package com.example.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
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
import com.example.util.BackupMetadata

/**
 * صفحه تمام‌صفحه اختصاصی پشتیبان‌گیری و بازیابی اطلاعات بر اساس بند ۱.۱ و ۱.۲ توافقنامه
 */
@Composable
fun BackupRestoreSettingsScreen(
    backupList: List<BackupMetadata>,
    onBack: () -> Unit,
    onCreateBackup: () -> Unit,
    onRestoreBackup: (BackupMetadata) -> Unit
) {
    BackHandler { onBack() }

    var backupToRestore by remember { mutableStateOf<BackupMetadata?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "پشتیبان‌گیری و بازیابی اطلاعات",
                subtitle = "ذخیره‌سازی امن پایگاه داده و فایل‌های پشتیبان کارگاه",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "حفاظت از اطلاعات کارگاه",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "با ایجاد فایل پشتیبان، تمام اطلاعات مشتریان، سفارشات، فاکتورها، چک‌ها و اسناد مالی در قالب استاندارد JSON در حافظه دستگاه ذخیره می‌شود تا در صورت تعویض گوشی یا بروز هرگونه مشکل، اطلاعات فوراً قابل بازیابی باشند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onCreateBackup,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("create_backup_sheet_btn")
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("ایجاد فایل پشتیبان جدید (هم‌اکنون)", fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Text(
                text = "فایل‌های پشتیبان موجود (${backupList.size} فایل):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            if (backupList.isEmpty()) {
                EmptyStateView(
                    title = "فایل پشتیبانی یافت نشد",
                    message = "برای تهیه اولین نسخه، دکمه «ایجاد فایل پشتیبان جدید» را بزنید.",
                    icon = Icons.Default.Storage,
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
                    items(backupList, key = { it.filePath }) { bkp ->
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
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bkp.fileName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "حجم: ${bkp.fileSizeFormatted} • تاریخ: ${bkp.jalaliDate}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { backupToRestore = bkp },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("بازیابی", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Restore confirmation dialog
    DeleteConfirmationDialog(
        show = backupToRestore != null,
        title = "تأیید بازیابی اطلاعات (Restore)",
        message = "هشدار: با بازیابی این فایل، داده‌های جاری دیتابیس با اطلاعات نسخه پشتیبان جایگزین خواهند شد. آیا ادامه می‌دهید؟",
        itemDetail = backupToRestore?.fileName,
        confirmButtonText = "بازیابی اطلاعات",
        onConfirm = {
            backupToRestore?.let { b ->
                onRestoreBackup(b)
                backupToRestore = null
            }
        },
        onDismiss = { backupToRestore = null }
    )
}

/**
 * سازگاری نام قبلی جهت جلوگیری از شکست وابستگی‌ها
 */
@Composable
fun BackupRestoreSettingsSheet(
    backupList: List<BackupMetadata>,
    onDismiss: () -> Unit,
    onCreateBackup: () -> Unit,
    onRestoreBackup: (BackupMetadata) -> Unit
) {
    BackupRestoreSettingsScreen(
        backupList = backupList,
        onBack = onDismiss,
        onCreateBackup = onCreateBackup,
        onRestoreBackup = onRestoreBackup
    )
}

@Preview(name = "Backup Restore Screen Preview", showBackground = true)
@Composable
fun BackupRestoreSettingsScreenPreview() {
    MyApplicationTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            BackupRestoreSettingsScreen(
                backupList = listOf(
                    BackupMetadata(
                        fileName = "AlpaSignage_Backup_14030805_1200.json",
                        filePath = "/storage/emulated/0/AlpaSignage/backup1.json",
                        fileSizeFormatted = "240 KB",
                        jalaliDate = "۱۴۰۳/۰۸/۰۵ ۱۲:۰۰",
                        timestamp = 1730000000000L,
                        recordsCount = 45
                    ),
                    BackupMetadata(
                        fileName = "AlpaSignage_Backup_14030801_0930.json",
                        filePath = "/storage/emulated/0/AlpaSignage/backup2.json",
                        fileSizeFormatted = "180 KB",
                        jalaliDate = "۱۴۰۳/۰۸/۰۱ ۰۹:۳۰",
                        timestamp = 1729600000000L,
                        recordsCount = 38
                    )
                ),
                onBack = {},
                onCreateBackup = {},
                onRestoreBackup = {}
            )
        }
    }
}

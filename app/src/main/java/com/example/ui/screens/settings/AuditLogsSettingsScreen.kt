package com.example.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.AuditLogEntity
import com.example.ui.components.common.AppCard
import com.example.ui.components.common.AppTopBar
import com.example.ui.components.common.EmptyStateView
import com.example.ui.theme.MoneyNegative
import com.example.ui.theme.MoneyPositive
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryAmber
import com.example.ui.theme.PrimaryBlue
import com.example.util.FormatUtils
import com.example.util.JalaliDate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AuditLogsSettingsScreen(
    logs: List<AuditLogEntity>,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var selectedActionFilter by remember { mutableStateOf("ALL") }

    val filteredLogs = remember(logs, selectedActionFilter) {
        if (selectedActionFilter == "ALL") logs
        else logs.filter { it.action == selectedActionFilter }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "سابقه فعالیت‌ها و رویدادها",
                subtitle = "${FormatUtils.toPersianDigits(logs.size.toString())} رویداد ثبت‌شده در سیستم",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    "ALL" to "همه",
                    "CREATE" to "ثبت جدید",
                    "UPDATE" to "ویرایش",
                    "DELETE" to "حذف"
                )
                for ((action, label) in filters) {
                    val isSelected = selectedActionFilter == action
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedActionFilter = action },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            if (filteredLogs.isEmpty()) {
                EmptyStateView(
                    title = "هیچ رویدادی یافت نشد",
                    message = "برای فیلتر انتخابی، رکوردی در سیستم ثبت نشده است.",
                    icon = Icons.Default.HistoryEdu,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        AuditLogItem(audit = log)
                    }
                }
            }
        }
    }
}

/**
 * کارت نمایش سابقه فعالیت‌ها (منتقل‌شده از داشبورد و ارتقایافته مطابق استانداردهای طراحی)
 */
@Composable
fun AuditLogItem(audit: AuditLogEntity) {
    val (actionIcon, actionColor, actionTitle) = when (audit.action) {
        "CREATE" -> Triple(Icons.Default.AddCircle, MoneyPositive, "ثبت جدید")
        "UPDATE" -> Triple(Icons.Default.Edit, PrimaryBlue, "ویرایش")
        "DELETE" -> Triple(Icons.Default.Delete, MoneyNegative, "حذف")
        else -> Triple(Icons.Default.Info, PrimaryAmber, "عملیات")
    }

    val (persianDate, timeString) = remember(audit.timestamp) {
        val jalali = JalaliDate.fromMillis(audit.timestamp)
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        FormatUtils.toPersianDigits(jalali.formatShort()) to FormatUtils.toPersianDigits(sdf.format(Date(audit.timestamp)))
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("audit_item_${audit.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = actionColor.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = null,
                        tint = actionColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = actionColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = actionTitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = actionColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "$persianDate • ساعت $timeString",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = audit.details,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "کاربر اقدام‌کننده: ${audit.userId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(name = "Audit Logs Screen Light Preview", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun AuditLogsSettingsScreenPreviewLight() {
    MyApplicationTheme(themeMode = "LIGHT") {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
        ) {
            Surface(color = MaterialTheme.colorScheme.background) {
                AuditLogsSettingsScreen(
                    logs = listOf(
                        AuditLogEntity(
                            id = "1",
                            action = "CREATE",
                            entityType = "CUSTOMER",
                            entityId = "c1",
                            details = "ثبت مشتری جدید: حاج محمد رضایی",
                            userId = "مدیر کارگاه",
                            timestamp = System.currentTimeMillis()
                        ),
                        AuditLogEntity(
                            id = "2",
                            action = "UPDATE",
                            entityType = "PROJECT",
                            entityId = "p1",
                            details = "تغییر مرحله پروژه تابلوی چلنیوم به: ساخت کارگاهی",
                            userId = "مدیر کارگاه",
                            timestamp = System.currentTimeMillis() - 3600000
                        )
                    ),
                    onBack = {}
                )
            }
        }
    }
}

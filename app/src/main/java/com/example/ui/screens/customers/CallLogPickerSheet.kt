package com.example.ui.screens.customers

import android.Manifest
import android.provider.CallLog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.components.common.AppFormBottomSheet
import com.example.ui.components.common.StickyBottomActionBar
import com.example.ui.theme.MoneyNegative
import com.example.ui.theme.MoneyPositive
import com.example.ui.theme.MyApplicationTheme
import com.example.util.CallLogEntry
import com.example.util.CallLogHelper
import com.example.util.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CallLogPickerSheet(
    onDismiss: () -> Unit,
    onCallSelected: (name: String?, number: String) -> Unit
) {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current

    var hasPermission by remember {
        mutableStateOf(if (isInspection) true else CallLogHelper.hasCallLogPermission(context))
    }
    var calls by remember { mutableStateOf<List<CallLogEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCallId by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && !isInspection) {
            isLoading = true
            val logs = withContext(Dispatchers.IO) {
                CallLogHelper.getRecentCallLogs(context, limit = 200)
            }
            calls = logs
            isLoading = false
        } else if (isInspection) {
            calls = samplePreviewCalls()
        }
    }

    val filteredCalls = remember(calls, searchQuery) {
        if (searchQuery.isBlank()) {
            calls
        } else {
            val q = FormatUtils.toEnglishDigits(searchQuery.trim()).lowercase()
            calls.filter { entry ->
                (entry.name?.lowercase()?.contains(q) == true) ||
                        entry.number.replace(Regex("[^0-9]"), "").contains(q)
            }
        }
    }

    val selectedEntry = remember(selectedCallId, calls) {
        calls.firstOrNull { it.id == selectedCallId }
    }

    AppFormBottomSheet(
        title = "انتخاب از تاریخچه تماس‌ها",
        subtitle = "جدیدترین تماس‌های گوشی بر اساس زمان (جدیدترین در بالا)",
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            if (!hasPermission) {
                // Permission Request Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        Text(
                            text = "نیاز به دسترسی به سابقه تماس‌ها",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "برای پر کردن سریع فرم مشتری جدید از روی تماس‌های اخیر، لطفاً اجازه دسترسی به سابقه تماس‌های دستگاه را تأیید فرمایید.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.READ_CALL_LOG) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            Text("درخواست دسترسی")
                        }
                    }
                }
            } else {
                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "جستجوی نام یا شماره تماس...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "پاک کردن")
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("call_log_search_input")
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                    }
                } else if (filteredCalls.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (searchQuery.isNotBlank()) "تماسی با این مشخصات یافت نشد." else "سابقه تماسی در دستگاه یافت نشد.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredCalls, key = { it.id }) { call ->
                            val isSelected = call.id == selectedCallId
                            CallLogItemRow(
                                entry = call,
                                isSelected = isSelected,
                                onClick = {
                                    selectedCallId = if (isSelected) null else call.id
                                }
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons
        StickyBottomActionBar(
            entityName = "تماس انتخابی",
            saveButtonText = "تأیید و انتقال به فرم",
            onSave = {
                selectedEntry?.let {
                    onCallSelected(it.name, it.number)
                    onDismiss()
                }
            },
            onCancel = onDismiss,
            isDirty = selectedEntry != null,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CallLogItemRow(
    entry: CallLogEntry,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (typeIcon, iconTint) = when (entry.type) {
        CallLog.Calls.MISSED_TYPE -> Icons.AutoMirrored.Filled.CallMissed to MoneyNegative
        CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade to MaterialTheme.colorScheme.primary
        else -> Icons.AutoMirrored.Filled.CallReceived to MoneyPositive
    }

    val typeLabel = when (entry.type) {
        CallLog.Calls.MISSED_TYPE -> "بی‌پاسخ"
        CallLog.Calls.OUTGOING_TYPE -> "صادره"
        else -> "وارده"
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .testTag("call_log_item_${entry.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = typeLabel,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Contact Name & Date
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name?.ifBlank { "مخاطب بدون نام" } ?: "مخاطب ناشناس",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = iconTint,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CallLogHelper.formatCallDateTime(entry.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Phone Number in LTR
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = FormatUtils.formatPhoneNumber(entry.number),
                    style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
            )
        }
    }
}

private fun samplePreviewCalls() = listOf(
    CallLogEntry("1", "مهندس اکبری", "09121112233", System.currentTimeMillis() - 1000 * 60 * 30, CallLog.Calls.INCOMING_TYPE),
    CallLogEntry("2", "فروشگاه الماس", "09145556677", System.currentTimeMillis() - 1000 * 60 * 180, CallLog.Calls.OUTGOING_TYPE),
    CallLogEntry("3", null, "09359998877", System.currentTimeMillis() - 1000 * 60 * 60 * 12, CallLog.Calls.MISSED_TYPE)
)

@Preview(name = "پیش‌نمایش شیت انتخاب تماس", showBackground = true)
@Composable
fun CallLogPickerSheetPreview() {
    MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            CallLogPickerSheet(
                onDismiss = {},
                onCallSelected = { _, _ -> }
            )
        }
    }
}

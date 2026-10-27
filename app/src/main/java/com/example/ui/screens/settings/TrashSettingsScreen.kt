package com.example.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.local.entities.TrashItem
import com.example.ui.components.common.AppTopBar
import com.example.ui.components.common.DeleteConfirmationDialog
import com.example.ui.components.common.EmptyStateView
import com.example.ui.theme.MyApplicationTheme
import com.example.util.FormatUtils

@Composable
fun TrashSettingsScreen(
    trashItems: List<TrashItem>,
    onBack: () -> Unit,
    onRestoreItem: (TrashItem) -> Unit
) {
    BackHandler(onBack = onBack)

    var selectedTab by remember { mutableIntStateOf(0) }
    var itemToRestore by remember { mutableStateOf<TrashItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = remember(trashItems, searchQuery) {
        if (searchQuery.isBlank()) trashItems
        else {
            val q = searchQuery.trim()
            trashItems.filter {
                it.title.contains(q, ignoreCase = true) ||
                        it.categoryLabel.contains(q, ignoreCase = true) ||
                        it.details.contains(q, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "سطل زباله",
                subtitle = "${FormatUtils.toPersianDigits(trashItems.size.toString())} مورد حذف‌شده قابل بازیابی",
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

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "فهرست یکپارچه (${FormatUtils.toPersianDigits(trashItems.size.toString())})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "گروه‌بندی دسته‌ها",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            if (trashItems.isEmpty()) {
                EmptyStateView(
                    title = "سطل زباله خالی است",
                    message = "کلیه اطلاعات فعال هستند و هیچ موردی حذف نشده است.",
                    icon = Icons.Default.DeleteSweep,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (selectedTab == 0) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredItems, key = { it.id }) { item ->
                                TrashItemCard(
                                    item = item,
                                    onRestore = { itemToRestore = item }
                                )
                            }
                        }
                    } else {
                        val grouped = remember(filteredItems) { filteredItems.groupBy { it.categoryLabel } }
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            grouped.forEach { (categoryName, items) ->
                                item(key = "header_$categoryName") {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = categoryName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Text(
                                                text = "${FormatUtils.toPersianDigits(items.size.toString())} مورد",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                                items(items, key = { it.id }) { item ->
                                    TrashItemCard(
                                        item = item,
                                        onRestore = { itemToRestore = item }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Pre-restore confirmation dialog per ADR-006
    DeleteConfirmationDialog(
        show = itemToRestore != null,
        title = "تأیید بازیابی از سطل زباله",
        message = "آیا از بازیابی «${itemToRestore?.title}» (${itemToRestore?.categoryLabel}) به فهرست فعال اطمینان دارید؟",
        confirmButtonText = "بازیابی",
        onConfirm = {
            itemToRestore?.let { itm ->
                onRestoreItem(itm)
                itemToRestore = null
            }
        },
        onDismiss = { itemToRestore = null }
    )
}

@Composable
fun TrashItemCard(
    item: TrashItem,
    onRestore: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trash_item_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.categoryLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (item.details.isNotBlank()) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = item.details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            FilledTonalButton(
                onClick = onRestore,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("restore_btn_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "بازیابی",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(name = "Trash Settings Light Preview", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun TrashSettingsScreenPreviewLight() {
    MyApplicationTheme(themeMode = "LIGHT") {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
        ) {
            Surface(color = MaterialTheme.colorScheme.background) {
                TrashSettingsScreen(
                    trashItems = listOf(
                        TrashItem(id = "1", title = "پروژه چلنیوم الماس", type = "PROJECT", categoryLabel = "پروژه", details = "کارفرما: رضایی", deletedAt = System.currentTimeMillis()),
                        TrashItem(id = "2", title = "احمد کریمی", type = "CUSTOMER", categoryLabel = "مشتری", details = "تلفن: ۰۹۱۲۳۴۵۶۷۸۹", deletedAt = System.currentTimeMillis() - 86400000),
                        TrashItem(id = "3", title = "هزینه خرید لامپ SMD", type = "EXPENSE", categoryLabel = "هزینه", details = "مبلغ: ۱,۵۰۰,۰۰۰ تومان", deletedAt = System.currentTimeMillis() - 172800000)
                    ),
                    onBack = {},
                    onRestoreItem = {}
                )
            }
        }
    }
}

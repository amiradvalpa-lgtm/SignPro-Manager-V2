package com.example.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.ui.components.common.AppCard
import com.example.ui.components.common.AppTopBar
import com.example.ui.components.common.DeleteConfirmationDialog
import com.example.ui.components.common.EmptyStateView
import com.example.ui.screens.projects.SpecImageViewerDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.util.FileStorageHelper
import com.example.util.SpecAttachmentItem
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecAttachmentsSettingsScreen(
    attachedItems: List<SpecAttachmentItem>,
    unattachedItems: List<SpecAttachmentItem>,
    onBack: () -> Unit,
    onDeleteFile: (SpecAttachmentItem) -> Unit
) {
    val context = LocalContext.current
    BackHandler(onBack = onBack)

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedItemForViewer by remember { mutableStateOf<SpecAttachmentItem?>(null) }
    var itemToDeleteDirectly by remember { mutableStateOf<SpecAttachmentItem?>(null) }

    val totalFiles = attachedItems.size + unattachedItems.size
    val totalBytes = (attachedItems + unattachedItems).sumOf { it.file.length() }
    val totalFormatted = FileStorageHelper.formatFileSize(totalBytes)

    Scaffold(
        topBar = {
            AppTopBar(
                title = "مدیریت تصاویر ضمیمه",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header Stats Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "آرشیو تصاویر اختصاصی کارگاه",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "مجموع: $totalFiles فایل ($totalFormatted) در حافظه داخلی برنامه",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "$totalFiles عکس",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Tab Row (M3 Tabs)
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
                            text = "تصاویر متصل (${attachedItems.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = { Icon(Icons.Default.Link, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "بدون اتصال / آزاد (${unattachedItems.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = { Icon(Icons.Default.LinkOff, contentDescription = null) }
                )
            }

            // Tab Content
            val currentList = if (selectedTab == 0) attachedItems else unattachedItems

            if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedTab == 0) {
                        EmptyStateView(
                            title = "تصویر متصلی یافت نشد",
                            message = "هیچ تصویری در پروژه‌های جاری متصل نیست.",
                            icon = Icons.Default.Image
                        )
                    } else {
                        EmptyStateView(
                            title = "تصویر بدون اتصالی وجود ندارد",
                            message = "تمام تصاویر برنامه به پروژه‌های فعال متصل هستند و هیچ فایل رهاشده‌ای وجود ندارد.",
                            icon = Icons.Default.CheckCircle
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selectedTab == 1) {
                        item(span = { GridItemSpan(2) }) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "این فایل‌ها متعلق به پروژه‌ها یا مشخصاتی هستند که از برنامه حذف شده‌اند ولی فایل در حافظه مانده است. می‌توانید آن‌ها را ذخیره یا پاکسازی کنید.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    items(currentList, key = { it.file.absolutePath }) { item ->
                        AttachmentCard(
                            item = item,
                            onClick = { selectedItemForViewer = item },
                            onDeleteDirectly = if (!item.isAttached) {
                                { itemToDeleteDirectly = item }
                            } else null
                        )
                    }
                }
            }
        }
    }

    // Full screen zoomable image viewer dialog
    selectedItemForViewer?.let { item ->
        SpecImageViewerDialog(
            imageUri = item.uriString,
            title = item.signType ?: "طرح ضمیمه",
            projectName = item.projectTitle,
            customerName = item.customerName,
            isOrphaned = !item.isAttached,
            onDeleteFile = if (!item.isAttached) {
                {
                    onDeleteFile(item)
                    selectedItemForViewer = null
                }
            } else null,
            onDismiss = { selectedItemForViewer = null }
        )
    }

    // Direct delete confirmation dialog from grid
    itemToDeleteDirectly?.let { item ->
        DeleteConfirmationDialog(
            show = true,
            title = "حذف فایل از حافظه برنامه",
            message = "آیا از حذف دائمی این تصویر از حافظه اطمینان دارید؟",
            itemDetail = "${item.projectTitle} (${item.fileSizeFormatted})",
            onConfirm = {
                onDeleteFile(item)
                itemToDeleteDirectly = null
            },
            onDismiss = { itemToDeleteDirectly = null }
        )
    }
}

@Composable
private fun AttachmentCard(
    item: SpecAttachmentItem,
    onClick: () -> Unit,
    onDeleteDirectly: (() -> Unit)? = null
) {
    AppCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("attachment_card_${item.file.name}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color.Black.copy(alpha = 0.05f))
            ) {
                Image(
                    painter = rememberAsyncImagePainter(item.uriString),
                    contentDescription = item.projectTitle ?: "تصویر ضمیمه",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // File size badge
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = item.fileSizeFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Delete button for unattached images
                if (onDeleteDirectly != null) {
                    IconButton(
                        onClick = onDeleteDirectly,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.error, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف فایل",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.projectTitle ?: "پروژه نامشخص",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!item.customerName.isNullOrBlank()) {
                    Text(
                        text = "کارفرما: ${item.customerName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!item.signType.isNullOrBlank()) {
                    Text(
                        text = item.signType,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Preview(name = "Spec Attachments Settings Screen", showBackground = true)
@Composable
fun SpecAttachmentsSettingsScreenPreview() {
    MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            SpecAttachmentsSettingsScreen(
                attachedItems = listOf(
                    SpecAttachmentItem(
                        file = File("/mock/p1.jpg"),
                        uriString = "mock_uri_1",
                        projectId = "p1",
                        projectTitle = "تابلو صرافی پارس",
                        customerName = "رضا کریمی",
                        signType = "چلنیوم طلایی",
                        isAttached = true,
                        fileSizeFormatted = "1.2 MB"
                    )
                ),
                unattachedItems = listOf(
                    SpecAttachmentItem(
                        file = File("/mock/p2.jpg"),
                        uriString = "mock_uri_2",
                        projectId = "p2",
                        projectTitle = "طرح اولیه رستوران باران",
                        customerName = "علی رضایی",
                        signType = null,
                        isAttached = false,
                        fileSizeFormatted = "850 KB"
                    )
                ),
                onBack = {},
                onDeleteFile = {}
            )
        }
    }
}

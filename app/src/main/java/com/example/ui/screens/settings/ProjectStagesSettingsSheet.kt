package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.data.local.entities.ProjectStageEntity
import com.example.ui.components.common.*
import com.example.ui.theme.MyApplicationTheme

/**
 * فرم اختصاصی مدیریت مراحل پروژه بر اساس بند ۱.۱ و ۱.۲ توافقنامه
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectStagesSettingsSheet(
    stages: List<ProjectStageEntity>,
    onDismiss: () -> Unit,
    onSaveStage: (ProjectStageEntity, Boolean) -> Unit,
    onDeleteStage: (ProjectStageEntity) -> Unit,
    onReorderStages: (List<ProjectStageEntity>) -> Unit,
    warnUnsavedChanges: Boolean = true
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedStageForEdit by remember { mutableStateOf<ProjectStageEntity?>(null) }
    var stageToDelete by remember { mutableStateOf<ProjectStageEntity?>(null) }

    val sortedStages = remember(stages) { stages.sortedBy { it.displayOrder } }

    AppFormBottomSheet(
        title = "مدیریت مراحل پروژه",
        subtitle = "تنظیم مراحل پیشرفت کارگاه، ترتیب و نشانگرهای رنگی",
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
                    text = "مراحل فعال (${sortedStages.size} مرحله)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FilledTonalButton(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_project_stage_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("افزودن مرحله جدید", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (sortedStages.isEmpty()) {
                EmptyStateView(
                    title = "مرحله‌ای تعریف نشده است",
                    message = "برای تعریف مراحل ساخت تابلوسازی روی دکمه افزودن مرحله جدید کلیک کنید.",
                    icon = Icons.Default.Timeline,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(sortedStages, key = { _, stage -> stage.id }) { index, stage ->
                        val stageColor = try {
                            Color(android.graphics.Color.parseColor(stage.colorHex))
                        } catch (_: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

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
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(stageColor)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = stage.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "اولویت ترتیب: ${stage.displayOrder}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val mutable = sortedStages.toMutableList()
                                                val prev = mutable[index - 1]
                                                mutable[index - 1] = stage
                                                mutable[index] = prev
                                                onReorderStages(mutable)
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.KeyboardArrowUp,
                                            contentDescription = "انتقال به بالا",
                                            modifier = Modifier.size(20.dp),
                                            tint = if (index > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (index < sortedStages.size - 1) {
                                                val mutable = sortedStages.toMutableList()
                                                val next = mutable[index + 1]
                                                mutable[index + 1] = stage
                                                mutable[index] = next
                                                onReorderStages(mutable)
                                            }
                                        },
                                        enabled = index < sortedStages.size - 1,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = "انتقال به پایین",
                                            modifier = Modifier.size(20.dp),
                                            tint = if (index < sortedStages.size - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = { selectedStageForEdit = stage },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "ویرایش مرحله",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = { stageToDelete = stage },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "حذف مرحله",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
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
                    .testTag("close_stages_sheet_btn")
            ) {
                Text("بستن")
            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        val nextOrder = (sortedStages.maxOfOrNull { it.displayOrder } ?: 0) + 1
        StageEditDialog(
            stage = null,
            nextOrder = nextOrder,
            warnUnsavedChanges = warnUnsavedChanges,
            onDismiss = { showAddDialog = false },
            onSave = { entity, isNew ->
                onSaveStage(entity, isNew)
                showAddDialog = false
            }
        )
    }

    // Edit dialog
    selectedStageForEdit?.let { stg ->
        StageEditDialog(
            stage = stg,
            nextOrder = stg.displayOrder,
            warnUnsavedChanges = warnUnsavedChanges,
            onDismiss = { selectedStageForEdit = null },
            onSave = { entity, isNew ->
                onSaveStage(entity, isNew)
                selectedStageForEdit = null
            }
        )
    }

    // Delete dialog
    DeleteConfirmationDialog(
        show = stageToDelete != null,
        title = "حذف مرحله کارگاهی",
        message = "آیا از حذف مرحله «${stageToDelete?.title}» اطمینان دارید؟",
        onConfirm = {
            stageToDelete?.let { stg ->
                onDeleteStage(stg)
                stageToDelete = null
            }
        },
        onDismiss = { stageToDelete = null }
    )
}

@Preview(name = "Project Stages Sheet Preview", showBackground = true)
@Composable
fun ProjectStagesSettingsSheetPreview() {
    MyApplicationTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ProjectStagesSettingsSheet(
                stages = listOf(
                    ProjectStageEntity(
                        id = "1",
                        name = "طراحی و تأیید طرح",
                        displayOrder = 1,
                        colorHex = "#3B82F6"
                    ),
                    ProjectStageEntity(
                        id = "2",
                        name = "برش لیزر و ورق‌کاری",
                        displayOrder = 2,
                        colorHex = "#F59E0B"
                    ),
                    ProjectStageEntity(
                        id = "3",
                        name = "مونتاژ و سیم‌کشی SMD",
                        displayOrder = 3,
                        colorHex = "#10B981"
                    ),
                    ProjectStageEntity(
                        id = "4",
                        name = "نصب در محل و تحویل",
                        displayOrder = 4,
                        colorHex = "#8B5CF6"
                    )
                ),
                onDismiss = {},
                onSaveStage = { _, _ -> },
                onDeleteStage = {},
                onReorderStages = {}
            )
        }
    }
}

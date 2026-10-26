package com.example.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import java.util.UUID

/**
 * صفحه تمام‌صفحه اختصاصی مدیریت مراحل پروژه بر اساس بند ۱.۱ و ۱.۲ توافقنامه
 */
@Composable
fun ProjectStagesSettingsScreen(
    stages: List<ProjectStageEntity>,
    onBack: () -> Unit,
    onSaveStage: (ProjectStageEntity, Boolean) -> Unit,
    onDeleteStage: (ProjectStageEntity) -> Unit,
    onReorderStages: (List<ProjectStageEntity>) -> Unit,
    warnUnsavedChanges: Boolean = true
) {
    BackHandler { onBack() }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedStageForEdit by remember { mutableStateOf<ProjectStageEntity?>(null) }
    var stageToDelete by remember { mutableStateOf<ProjectStageEntity?>(null) }

    val sortedStages = remember(stages) { stages.sortedBy { it.displayOrder } }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "مدیریت مراحل پروژه",
                subtitle = "تنظیم مراحل پیشرفت کارگاه، ترتیب و نشانگرهای رنگی",
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
                    text = "مراحل فعال (${sortedStages.size} مرحله)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FilledTonalButton(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_project_stage_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("افزودن مرحله جدید", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (sortedStages.isEmpty()) {
                EmptyStateView(
                    title = "مرحله‌ای تعریف نشده است",
                    message = "برای تعریف مراحل ساخت تابلوسازی روی دکمه «افزودن مرحله جدید» کلیک کنید.",
                    icon = Icons.Default.Timeline,
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
                    itemsIndexed(sortedStages, key = { _, stage -> stage.id }) { index, stage ->
                        val stageColor = try {
                            Color(android.graphics.Color.parseColor(stage.colorHex))
                        } catch (_: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

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
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(stageColor)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = stage.title,
                                            style = MaterialTheme.typography.titleSmall,
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
                                        modifier = Modifier.size(34.dp)
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
                                        modifier = Modifier.size(34.dp)
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
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "ویرایش مرحله",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    IconButton(
                                        onClick = { stageToDelete = stage },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "حذف مرحله",
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

/**
 * مودال افزودن و ویرایش مرحله پروژه (مستقر در فایل اختصاصی مراحل پروژه طبق اصل ۱.۱ توافقنامه)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StageEditDialog(
    stage: ProjectStageEntity?,
    nextOrder: Int,
    onDismiss: () -> Unit,
    onSave: (ProjectStageEntity, Boolean) -> Unit,
    warnUnsavedChanges: Boolean = true
) {
    val isNew = stage == null
    var title by remember { mutableStateOf(stage?.title ?: "") }
    var selectedColor by remember { mutableStateOf(stage?.colorHex ?: "#3B82F6") }
    var titleError by remember { mutableStateOf<String?>(null) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    val presetColors = listOf(
        "#3B82F6", "#10B981", "#F59E0B", "#EF4444",
        "#8B5CF6", "#EC4899", "#14B8A6", "#6366F1"
    )

    val isDirty = remember(title, selectedColor) {
        if (isNew) {
            title.isNotBlank()
        } else {
            title != (stage?.title ?: "") || selectedColor != (stage?.colorHex ?: "")
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
            titleError = "عنوان مرحله الزامی است."
        } else {
            val entity = stage?.copy(
                name = title.trim(),
                colorHex = selectedColor,
                updatedAt = System.currentTimeMillis()
            ) ?: ProjectStageEntity(
                id = UUID.randomUUID().toString(),
                name = title.trim(),
                displayOrder = nextOrder,
                colorHex = selectedColor
            )
            onSave(entity, isNew)
        }
    }

    AppFormBottomSheet(
        title = if (isNew) "افزودن مرحله جدید" else "ویرایش مرحله کارگاهی",
        subtitle = "تنظیم عنوان مرحله و رنگ شاخص پیشرفت سفارش",
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PersianTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = null
                },
                label = "عنوان مرحله",
                placeholder = "مثال: خم‌کاری چلنیوم، جوشکاری اسکلت",
                isError = titleError != null,
                errorMessage = titleError
            )

            Column {
                Text(
                    text = "رنگ نشانگر مرحله:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(presetColors) { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else color.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "انتخاب شده",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        StickyBottomActionBar(
            entityName = "مرحله پروژه",
            saveButtonText = "ذخیره مرحله",
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
fun ProjectStagesSettingsSheet(
    stages: List<ProjectStageEntity>,
    onDismiss: () -> Unit,
    onSaveStage: (ProjectStageEntity, Boolean) -> Unit,
    onDeleteStage: (ProjectStageEntity) -> Unit,
    onReorderStages: (List<ProjectStageEntity>) -> Unit,
    warnUnsavedChanges: Boolean = true
) {
    ProjectStagesSettingsScreen(
        stages = stages,
        onBack = onDismiss,
        onSaveStage = onSaveStage,
        onDeleteStage = onDeleteStage,
        onReorderStages = onReorderStages,
        warnUnsavedChanges = warnUnsavedChanges
    )
}

@Preview(name = "Project Stages Screen Preview", showBackground = true)
@Composable
fun ProjectStagesSettingsScreenPreview() {
    MyApplicationTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ProjectStagesSettingsScreen(
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
                        name = "نصب در محل و تحویل نهایی",
                        displayOrder = 4,
                        colorHex = "#8B5CF6"
                    )
                ),
                onBack = {},
                onSaveStage = { _, _ -> },
                onDeleteStage = {},
                onReorderStages = {}
            )
        }
    }
}

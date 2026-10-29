package com.example.ui.screens.projects

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.ui.components.common.AppFormBottomSheet
import com.example.ui.components.common.StickyBottomActionBar
import com.example.ui.components.common.UnsavedChangesDialog
import com.example.ui.theme.MyApplicationTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectStagesToggleSheet(
    projectTitle: String,
    stages: List<ProjectStageEntity>,
    completedStageIds: List<String>,
    warnUnsavedChanges: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val sortedStages = remember(stages) { stages.sortedBy { it.displayOrder } }

    // Map of stageId -> Boolean (isCompleted)
    val currentSelected = remember(completedStageIds) {
        mutableStateMapOf<String, Boolean>().apply {
            sortedStages.forEach { stage ->
                put(stage.id, completedStageIds.contains(stage.id))
            }
        }
    }

    var showUnsavedDialog by remember { mutableStateOf(false) }

    val isDirty = remember(currentSelected.toMap(), completedStageIds) {
        val selectedIds = currentSelected.filter { it.value }.keys.toSet()
        val originalIds = completedStageIds.toSet()
        selectedIds != originalIds
    }

    val attemptDismiss = {
        if (warnUnsavedChanges && isDirty) {
            showUnsavedDialog = true
        } else {
            onDismiss()
        }
    }

    val executeSave = {
        val finalCompletedList = sortedStages.filter { currentSelected[it.id] == true }.map { it.id }
        onSave(finalCompletedList)
    }

    AppFormBottomSheet(
        title = "ویرایش مراحل پروژه",
        subtitle = projectTitle,
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "وضعیت پیشرفت مراحل اجرایی کارگاه را مشخص کنید:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            sortedStages.forEach { stage ->
                val isChecked = currentSelected[stage.id] == true
                val stageColor = try {
                    Color(AndroidColor.parseColor(stage.colorHex))
                } catch (_: Exception) {
                    MaterialTheme.colorScheme.primary
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isChecked) stageColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isChecked) stageColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Circular order badge
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isChecked) stageColor else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stage.displayOrder.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isChecked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "مرحله ${stage.name} تکمیل شده است",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (stage.description.isNotBlank()) {
                                    Text(
                                        text = stage.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Switch(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                currentSelected[stage.id] = checked
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = stageColor
                            ),
                            modifier = Modifier.testTag("toggle_stage_${stage.id}")
                        )
                    }
                }
            }
        }

        // Sticky Bottom Action Bar with 2:1 ratio and Dirty state
        StickyBottomActionBar(
            entityName = "مراحل",
            saveButtonText = "ذخیره تغییرات مراحل",
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

@Preview(name = "Stages Toggle Sheet Preview", showBackground = true)
@Composable
fun ProjectStagesToggleSheetPreview() {
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
    ) {
        MyApplicationTheme {
            Surface(color = MaterialTheme.colorScheme.background) {
                ProjectStagesToggleSheet(
                    projectTitle = "تابلو چلنیوم طلا و جواهر رحمانی",
                    stages = listOf(
                        ProjectStageEntity("1", "مذاکره", "مذاکره و تاییدیه", 1, "#10B981"),
                        ProjectStageEntity("2", "طراحی", "طراحی و شبیه‌سازی", 2, "#0284C7"),
                        ProjectStageEntity("3", "ساخت", "ساخت و مونتاژ کارگاه", 3, "#E11D48"),
                        ProjectStageEntity("4", "نصب", "نصب و راه‌اندازی", 4, "#7C3AED"),
                        ProjectStageEntity("5", "تسویه", "تسویه حساب و تحویل", 5, "#0D9488")
                    ),
                    completedStageIds = listOf("1", "2"),
                    onDismiss = {},
                    onSave = {}
                )
            }
        }
    }
}

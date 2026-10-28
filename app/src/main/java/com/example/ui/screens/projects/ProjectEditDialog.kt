package com.example.ui.screens.projects

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.data.local.entities.CustomerEntity
import com.example.data.local.entities.ProjectEntity
import com.example.ui.components.common.AppFormBottomSheet
import com.example.ui.components.common.PersianDateField
import com.example.ui.components.common.PersianTextField
import com.example.ui.components.common.StickyBottomActionBar
import com.example.ui.components.common.UnsavedChangesDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.util.JalaliDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectEditDialog(
    project: ProjectEntity?,
    customers: List<CustomerEntity>,
    preselectedCustomerId: String? = null,
    onDismiss: () -> Unit,
    onSave: (ProjectEntity, Boolean) -> Unit,
    warnUnsavedChanges: Boolean = true
) {
    val isNew = project == null

    var title by remember { mutableStateOf(project?.title ?: "") }
    var selectedCustomerId by remember {
        mutableStateOf(project?.customerId ?: preselectedCustomerId ?: customers.firstOrNull()?.id ?: "")
    }
    var startDate by remember {
        mutableStateOf(project?.startDate ?: JalaliDate.today().formatShort())
    }
    var description by remember { mutableStateOf(project?.description ?: "") }

    var titleError by remember { mutableStateOf<String?>(null) }
    var customerError by remember { mutableStateOf<String?>(null) }
    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    val isDirty = remember(title, selectedCustomerId, startDate, description) {
        if (isNew) {
            title.isNotBlank() || description.isNotBlank() || selectedCustomerId != (preselectedCustomerId ?: customers.firstOrNull()?.id ?: "")
        } else {
            title != (project?.title ?: "") ||
                    selectedCustomerId != (project?.customerId ?: "") ||
                    startDate != (project?.startDate ?: "") ||
                    description != (project?.description ?: "")
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
        var hasError = false
        if (title.trim().isEmpty()) {
            titleError = "عنوان پروژه الزامی است."
            hasError = true
        }
        if (selectedCustomerId.isEmpty()) {
            customerError = "انتخاب مشتری الزامی است."
            hasError = true
        }

        if (!hasError) {
            val entity = project?.copy(
                title = title.trim(),
                customerId = selectedCustomerId,
                startDate = startDate,
                description = description.trim()
            ) ?: ProjectEntity(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                customerId = selectedCustomerId,
                startDate = startDate,
                status = "در حال ساخت",
                description = description.trim()
            )
            onSave(entity, isNew)
        }
    }

    AppFormBottomSheet(
        title = if (isNew) "تعریف پروژه جدید" else "ویرایش پروژه",
        subtitle = "اطلاعات پروژه تابلوسازی و کارفرمای مربوطه",
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
                label = "عنوان پروژه تابلوسازی",
                placeholder = "مثال: تابلوی چلنیوم فروشگاه الماس",
                leadingIcon = Icons.Default.Business,
                isError = titleError != null,
                errorMessage = titleError,
                testTag = "project_title_input"
            )

            // Customer Selection Dropdown
            val selectedCustomerName = customers.firstOrNull { it.id == selectedCustomerId }?.name ?: "انتخاب مشتری"
            ExposedDropdownMenuBox(
                expanded = customerDropdownExpanded,
                onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCustomerName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("مشتری یا کارفرمای مرتبط") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                    isError = customerError != null,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = customerDropdownExpanded,
                    onDismissRequest = { customerDropdownExpanded = false }
                ) {
                    customers.forEach { cust ->
                        DropdownMenuItem(
                            text = { Text("${cust.name} (${cust.phone})") },
                            onClick = {
                                selectedCustomerId = cust.id
                                customerError = null
                                customerDropdownExpanded = false
                            }
                        )
                    }
                }
            }
            if (customerError != null) {
                Text(
                    text = customerError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // Start Date
            PersianDateField(
                dateString = startDate,
                onDateSelected = { startDate = it },
                label = "تاریخ شروع پروژه"
            )

            PersianTextField(
                value = description,
                onValueChange = { description = it },
                label = "توضیحات و نیازمندی‌های پروژه",
                placeholder = "مشخصات محل نصب، ارتفاع، هماهنگی‌های ویژه و ...",
                singleLine = false,
                testTag = "project_description_input"
            )
        }

        // Sticky Bottom Action Bar with 2:1 ratio and Dirty state
        StickyBottomActionBar(
            entityName = "پروژه",
            saveButtonText = "ذخیره پروژه",
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

@Preview(name = "Project Form Preview", showBackground = true)
@Composable
fun ProjectEditDialogPreview() {
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
    ) {
        MyApplicationTheme {
            Surface(color = MaterialTheme.colorScheme.background) {
                ProjectEditDialog(
                    project = ProjectEntity(
                        title = "تابلو چلنیوم طلا فروشی کیمیا",
                        customerId = "c1",
                        startDate = "1403/08/15",
                        description = "نصب در ارتفاع ۴ متری، زیرسازی کامپوزیت طوسی"
                    ),
                    customers = listOf(
                        CustomerEntity(id = "c1", name = "آقای رحمانی", phone = "09121112233")
                    ),
                    onDismiss = {},
                    onSave = { _, _ -> }
                )
            }
        }
    }
}

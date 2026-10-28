package com.example.ui.screens.customers

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entities.CustomerEntity
import com.example.ui.components.common.AppFormBottomSheet
import com.example.ui.components.common.PersianTextField
import com.example.ui.components.common.PhoneInputField
import com.example.ui.components.common.StickyBottomActionBar
import com.example.ui.components.common.UnsavedChangesDialog
import com.example.util.FormatUtils
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerEditDialog(
    customer: CustomerEntity?,
    onDismiss: () -> Unit,
    onSave: (CustomerEntity, Boolean) -> Unit,
    warnUnsavedChanges: Boolean = true
) {
    val isNew = customer == null

    var name by remember { mutableStateOf(customer?.name ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var nationalCode by remember { mutableStateOf(customer?.nationalCode ?: "") }
    var address by remember { mutableStateOf(customer?.address ?: "") }
    var description by remember { mutableStateOf(customer?.description ?: "") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var nationalCodeError by remember { mutableStateOf<String?>(null) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    val isDirty = remember(name, phone, nationalCode, address, description) {
        if (isNew) {
            name.isNotBlank() || phone.isNotBlank() || nationalCode.isNotBlank() || address.isNotBlank() || description.isNotBlank()
        } else {
            name != (customer?.name ?: "") ||
                    phone != (customer?.phone ?: "") ||
                    nationalCode != (customer?.nationalCode ?: "") ||
                    address != (customer?.address ?: "") ||
                    description != (customer?.description ?: "")
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
        if (name.trim().isEmpty()) {
            nameError = "نام مشتری الزامی است."
            hasError = true
        }
        if (phone.trim().isEmpty()) {
            phoneError = "شماره تلفن همراه الزامی است."
            hasError = true
        } else if (!FormatUtils.isValidPhoneNumber(phone)) {
            phoneError = "شماره تماس باید ۱۰ رقمی معتبر باشد."
            hasError = true
        }

        if (nationalCode.trim().isNotEmpty()) {
            if (!FormatUtils.isValidIranianNationalCode(nationalCode)) {
                nationalCodeError = "کد ملی ۱۰ رقمی وارد شده نامعتبر است."
                hasError = true
            }
        }

        if (!hasError) {
            val entity = customer?.copy(
                name = name.trim(),
                phone = phone.trim(),
                nationalCode = nationalCode.trim(),
                address = address.trim(),
                description = description.trim()
            ) ?: CustomerEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                phone = phone.trim(),
                nationalCode = nationalCode.trim(),
                address = address.trim(),
                description = description.trim()
            )
            onSave(entity, isNew)
        }
    }

    AppFormBottomSheet(
        title = if (isNew) "ثبت مشتری جدید" else "ویرایش اطلاعات مشتری",
        subtitle = null,
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
                value = name,
                onValueChange = {
                    name = it
                    nameError = null
                },
                label = "نام مشتری یا شرکت",
                placeholder = "مثال: حاج محمد رضایی",
                leadingIcon = Icons.Default.Person,
                isError = nameError != null,
                errorMessage = nameError,
                testTag = "customer_name_input"
            )

            PhoneInputField(
                value = phone,
                onValueChange = {
                    phone = it
                    phoneError = null
                },
                isError = phoneError != null,
                errorMessage = phoneError,
                testTag = "customer_phone_input"
            )

            PersianTextField(
                value = nationalCode,
                onValueChange = { input ->
                    val digits = FormatUtils.toEnglishDigits(input).filter { it.isDigit() }.take(10)
                    nationalCode = digits
                    nationalCodeError = null
                },
                label = "کد ملی (اختیاری)",
                placeholder = "۱۰ رقم کد ملی (اختیاری)",
                leadingIcon = Icons.Default.Badge,
                keyboardType = KeyboardType.Number,
                isError = nationalCodeError != null,
                errorMessage = nationalCodeError,
                testTag = "customer_national_code_input"
            )

            PersianTextField(
                value = address,
                onValueChange = { address = it },
                label = "نشانی محل کسب یا کارگاه",
                placeholder = "خیابان، پلاک، طبقه",
                leadingIcon = Icons.Default.LocationOn,
                testTag = "customer_address_input"
            )

            PersianTextField(
                value = description,
                onValueChange = { description = it },
                label = "توضیحات و یادداشت‌ها",
                placeholder = "یادداشت‌های فنی، نحوه آشنایی و سوابق مشتری",
                singleLine = false,
                testTag = "customer_description_input"
            )
        }

        // Sticky Bottom Action Bar with 2:1 ratio and Dirty state
        StickyBottomActionBar(
            entityName = "مشتری",
            saveButtonText = "ذخیره مشتری",
            onSave = executeSave,
            onCancel = onDismiss, // Direct cancel per Agreement 3.2
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

@androidx.compose.ui.tooling.preview.Preview(name = "Customer Form Preview", showBackground = true)
@Composable
fun CustomerEditDialogPreview() {
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
    ) {
        com.example.ui.theme.MyApplicationTheme {
            Surface(color = MaterialTheme.colorScheme.background) {
                CustomerEditDialog(
                    customer = CustomerEntity(
                        name = "حاج محمد رضایی",
                        phone = "09121234567",
                        nationalCode = "1234567890",
                        address = "تهران، میدان تجریش",
                        description = "مشتری همکار و وفادار"
                    ),
                    onDismiss = {},
                    onSave = { _, _ -> }
                )
            }
        }
    }
}

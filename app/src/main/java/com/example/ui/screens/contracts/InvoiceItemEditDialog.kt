package com.example.ui.screens.contracts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.data.local.entities.InvoiceItemEntity
import com.example.ui.components.common.AppFormBottomSheet
import com.example.ui.components.common.CurrencyInputField
import com.example.ui.components.common.PersianTextField
import com.example.ui.components.common.StickyBottomActionBar
import com.example.ui.components.common.UnitSelectorDropdown
import com.example.ui.components.common.UnsavedChangesDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.util.FormatUtils
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceItemEditDialog(
    invoiceId: String,
    item: InvoiceItemEntity?,
    currencyUnit: String = "TOMAN",
    warnUnsavedChanges: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (InvoiceItemEntity, Boolean) -> Unit
) {
    val isNew = item == null

    var serviceType by remember { mutableStateOf(item?.serviceType ?: "ساخت و مونتاژ حروف چلنیوم") }
    var itemDescription by remember { mutableStateOf(item?.itemDescription ?: "") }
    var quantityStr by remember { mutableStateOf(if (item != null) item.quantity.toString() else "1") }
    var unit by remember { mutableStateOf(item?.unit ?: "عدد") }
    var unitPrice by remember { mutableStateOf(item?.unitPrice ?: 0L) }
    var description by remember { mutableStateOf(item?.description ?: "") }

    var serviceDropdownExpanded by remember { mutableStateOf(false) }
    var serviceTypeError by remember { mutableStateOf<String?>(null) }
    var quantityError by remember { mutableStateOf<String?>(null) }
    var unitPriceError by remember { mutableStateOf<String?>(null) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    val servicePresets = listOf(
        "ساخت و مونتاژ حروف چلنیوم (متر محیط)",
        "حروف برجسته استیل طلایی / نقره‌ای",
        "حروف پلاستیک لبه سوئدی و دوبل",
        "نئون فلکسی روی پلکسی شفاف",
        "زیرسازی و ورق کامپوزیت نما (متر مربع)",
        "چوب ترمووود فنلاندی شاسی‌کشی شده",
        "طراحی و ساخت لایت‌باکس فریم‌لس",
        "نصب، سیم‌کشی و راه‌اندازی در محل",
        "منبع تغذیه (ترانس سوئیچینگ ضدآب)",
        "برش لیزر پلکسی و آهن"
    )

    val quantity = quantityStr.toDoubleOrNull() ?: 0.0
    val calculatedTotal = (quantity * unitPrice).toLong()

    val isDirty by remember {
        derivedStateOf {
            if (item == null) {
                serviceType.isNotBlank() || itemDescription.isNotBlank() || quantityStr != "1" || unitPrice > 0L || description.isNotBlank()
            } else {
                serviceType != item.serviceType ||
                        itemDescription != item.itemDescription ||
                        quantityStr != item.quantity.toString() ||
                        unit != item.unit ||
                        unitPrice != item.unitPrice ||
                        description != item.description
            }
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
        if (serviceType.trim().isEmpty()) {
            serviceTypeError = "نوع خدمت الزامی است."
            hasError = true
        }
        val q = quantityStr.toDoubleOrNull()
        if (q == null || q <= 0) {
            quantityError = "مقدار معتبر نیست."
            hasError = true
        }
        if (unitPrice <= 0) {
            unitPriceError = "قیمت واحد الزامی است."
            hasError = true
        }

        if (!hasError) {
            val entity = item?.copy(
                serviceType = serviceType.trim(),
                itemDescription = itemDescription.trim(),
                quantity = q ?: 1.0,
                unit = unit,
                unitPrice = unitPrice,
                calculatedTotal = ((q ?: 1.0) * unitPrice).toLong(),
                description = description.trim()
            ) ?: InvoiceItemEntity(
                id = UUID.randomUUID().toString(),
                invoiceId = invoiceId,
                serviceType = serviceType.trim(),
                itemDescription = itemDescription.trim(),
                quantity = q ?: 1.0,
                unit = unit,
                unitPrice = unitPrice,
                calculatedTotal = ((q ?: 1.0) * unitPrice).toLong(),
                description = description.trim()
            )
            onSave(entity, isNew)
        }
    }

    AppFormBottomSheet(
        title = if (isNew) "افزودن ردیف فاکتور" else "ویرایش ردیف فاکتور",
        subtitle = "تعیین کالا، خدمات، متراژ، قیمت واحد و محاسبه خودکار",
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
            // Service Type Dropdown
            ExposedDropdownMenuBox(
                expanded = serviceDropdownExpanded,
                onExpandedChange = { serviceDropdownExpanded = !serviceDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = serviceType,
                    onValueChange = {
                        serviceType = it
                        serviceTypeError = null
                    },
                    label = { Text("نوع خدمت یا کالا") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceDropdownExpanded) },
                    isError = serviceTypeError != null,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable)
                )
                ExposedDropdownMenu(
                    expanded = serviceDropdownExpanded,
                    onDismissRequest = { serviceDropdownExpanded = false }
                ) {
                    servicePresets.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(preset) },
                            onClick = {
                                serviceType = preset
                                serviceTypeError = null
                                serviceDropdownExpanded = false
                            }
                        )
                    }
                }
            }
            if (serviceTypeError != null) {
                Text(text = serviceTypeError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            PersianTextField(
                value = itemDescription,
                onValueChange = { itemDescription = it },
                label = "شرح تفصیلی ردیف",
                placeholder = "مثال: قطر قلم ۳۰ سانتیمتر با پلکسی تایوان و اس‌ام‌دی لنزدار کره"
            )

            // Row with Amount (مقدار) and Unit Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PersianTextField(
                    value = quantityStr,
                    onValueChange = {
                        quantityStr = it
                        quantityError = null
                    },
                    label = "مقدار",
                    placeholder = "مثال: ۱۲.۵",
                    keyboardType = KeyboardType.Number,
                    isError = quantityError != null,
                    errorMessage = quantityError,
                    modifier = Modifier.weight(1.3f),
                    testTag = "item_quantity_input"
                )

                UnitSelectorDropdown(
                    selectedUnit = unit,
                    onUnitSelected = { unit = it },
                    modifier = Modifier.weight(1f)
                )
            }

            // Next Row: Unit Price (قیمت واحد)
            CurrencyInputField(
                amount = unitPrice,
                onAmountChange = {
                    unitPrice = it
                    unitPriceError = null
                },
                label = "قیمت واحد",
                currencyUnit = currencyUnit,
                isError = unitPriceError != null,
                errorMessage = unitPriceError,
                modifier = Modifier.fillMaxWidth(),
                testTag = "item_unit_price_input"
            )

            // Automatic calculation card: Subtle primary background with hairline border
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "مبلغ کل محاسبه‌شده ردیف (مقدار × فی):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${FormatUtils.formatCurrency(calculatedTotal, currencyUnit)} ($quantity $unit)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Additional notes with double height
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("توضیحات تکمیلی") },
                placeholder = { Text("گارانتی، نحوه شاسی‌کشی و...") },
                minLines = 4,
                maxLines = 6,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("item_description_input")
            )
        }

        // Sticky Bottom Action Bar with 2:1 ratio and Dirty state
        StickyBottomActionBar(
            entityName = "ردیف فاکتور",
            saveButtonText = "ذخیره ردیف فاکتور",
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

@Preview(name = "Invoice Item Form Preview", showBackground = true)
@Composable
fun InvoiceItemEditDialogPreview() {
    MyApplicationTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            InvoiceItemEditDialog(
                invoiceId = "inv1",
                item = InvoiceItemEntity(
                    invoiceId = "inv1",
                    serviceType = "ساخت و مونتاژ حروف چلنیوم",
                    itemDescription = "رنگ طلایی براق با لبه پانچ",
                    quantity = 15.5,
                    unit = "متر",
                    unitPrice = 1200000L,
                    calculatedTotal = 18600000L,
                    description = "شامل ۱۲ ماه ضمانت ترانس و اس‌ام‌دی"
                ),
                currencyUnit = "TOMAN",
                onDismiss = {},
                onSave = { _, _ -> }
            )
        }
    }
}

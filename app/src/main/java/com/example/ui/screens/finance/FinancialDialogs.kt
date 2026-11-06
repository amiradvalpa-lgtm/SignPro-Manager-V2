package com.example.ui.screens.finance

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.data.local.entities.*
import com.example.ui.components.common.AppDropdownField
import com.example.ui.components.common.AppFormBottomSheet
import com.example.ui.components.common.CurrencyInputField
import com.example.ui.components.common.CustomerDropdownField
import com.example.ui.components.common.PersianDateField
import com.example.ui.components.common.PersianTextField
import com.example.ui.components.common.StickyBottomActionBar
import com.example.ui.components.common.UnsavedChangesDialog
import com.example.ui.components.common.appOutlinedTextFieldColors
import com.example.ui.theme.MyApplicationTheme
import com.example.util.JalaliDate
import java.util.UUID

private fun getPaymentMethodIcon(method: String): ImageVector {
    return when (method) {
        "کارت به کارت" -> Icons.Default.CreditCard
        "دستگاه پوز (POS)" -> Icons.Default.PointOfSale
        "نقدی" -> Icons.Default.Payments
        "حواله پایا / ساتنا" -> Icons.Default.AccountBalance
        "چک بانکی" -> Icons.Default.ReceiptLong
        else -> Icons.Default.Payment
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentEditDialog(
    payment: PaymentEntity?,
    customers: List<CustomerEntity>,
    preselectedCustomerId: String? = null,
    initialType: String = "RECEIPT",
    hideTypeSelector: Boolean = false,
    currencyUnit: String = "TOMAN",
    warnUnsavedChanges: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (PaymentEntity, Boolean) -> Unit
) {
    val isNew = payment == null

    var selectedCustomerId by remember {
        mutableStateOf(payment?.customerId ?: preselectedCustomerId ?: customers.firstOrNull()?.id ?: "")
    }
    var type by remember { mutableStateOf(payment?.type ?: initialType) } // RECEIPT or PAYMENT
    var amount by remember { mutableStateOf(payment?.amount ?: 0L) }
    var date by remember { mutableStateOf(payment?.date ?: JalaliDate.today().formatShort()) }
    var method by remember { mutableStateOf(payment?.method ?: "کارت به کارت") }
    var trackingCode by remember { mutableStateOf(payment?.trackingCode ?: "") }
    var description by remember { mutableStateOf(payment?.description ?: "") }

    var amountError by remember { mutableStateOf<String?>(null) }
    var customerError by remember { mutableStateOf<String?>(null) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    val methodOptions = listOf("کارت به کارت", "دستگاه پوز (POS)", "نقدی", "حواله پایا / ساتنا", "چک بانکی")
    val operationName = if (type == "RECEIPT") "دریافت از مشتری" else "پرداخت به مشتری"

    val isDirty = remember(selectedCustomerId, type, amount, date, method, trackingCode, description) {
        if (isNew) {
            amount > 0L || trackingCode.isNotBlank() || description.isNotBlank() || selectedCustomerId != (preselectedCustomerId ?: customers.firstOrNull()?.id ?: "")
        } else {
            selectedCustomerId != (payment?.customerId ?: "") ||
                    type != (payment?.type ?: "") ||
                    amount != (payment?.amount ?: 0L) ||
                    date != (payment?.date ?: "") ||
                    method != (payment?.method ?: "") ||
                    trackingCode != (payment?.trackingCode ?: "") ||
                    description != (payment?.description ?: "")
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
        if (selectedCustomerId.isEmpty()) {
            customerError = "انتخاب مشتری الزامی است."
            hasError = true
        }
        if (amount <= 0) {
            amountError = "مبلغ تراکنش باید بیشتر از صفر باشد."
            hasError = true
        }

        if (!hasError) {
            val entity = payment?.copy(
                customerId = selectedCustomerId,
                type = type,
                amount = amount,
                date = date,
                method = method,
                trackingCode = trackingCode.trim(),
                description = description.trim()
            ) ?: PaymentEntity(
                id = UUID.randomUUID().toString(),
                customerId = selectedCustomerId,
                type = type,
                amount = amount,
                date = date,
                method = method,
                trackingCode = trackingCode.trim(),
                description = description.trim()
            )
            onSave(entity, isNew)
        }
    }

    AppFormBottomSheet(
        title = if (isNew) "ثبت $operationName" else "ویرایش $operationName",
        subtitle = "ثبت اطلاعات تراکنش مالی و نحوه تسویه",
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
            // Operation type switch (only displayed if not hidden)
            if (!hideTypeSelector) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == "RECEIPT",
                        onClick = { type = "RECEIPT" },
                        label = { Text("دریافتی از مشتری", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == "PAYMENT",
                        onClick = { type = "PAYMENT" },
                        label = { Text("پرداخت به مشتری", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Customer selection with Search
            CustomerDropdownField(
                selectedCustomerId = selectedCustomerId,
                customers = customers,
                onCustomerSelected = { cust ->
                    selectedCustomerId = cust.id
                    customerError = null
                },
                label = "مشتری مرتبط",
                placeholder = "انتخاب مشتری",
                isError = customerError != null,
                errorMessage = customerError,
                testTag = "payment_customer_dropdown"
            )

            CurrencyInputField(
                amount = amount,
                onAmountChange = {
                    amount = it
                    amountError = null
                },
                label = "مبلغ تراکنش",
                currencyUnit = currencyUnit,
                isError = amountError != null,
                errorMessage = amountError,
                testTag = "payment_amount_input"
            )

            PersianDateField(
                dateString = date,
                onDateSelected = { date = it },
                label = "تاریخ تراکنش"
            )

            // Method with Icons
            AppDropdownField(
                value = method,
                label = "روش پرداخت / دریافت",
                options = methodOptions,
                optionIcon = { getPaymentMethodIcon(it) },
                leadingIcon = getPaymentMethodIcon(method),
                onOptionSelected = { method = it },
                testTag = "payment_method_dropdown"
            )

            PersianTextField(
                value = trackingCode,
                onValueChange = { trackingCode = it },
                label = "شماره پیگیری یا کد ارجاع",
                placeholder = "شماره پیگیری فیش واریزی یا پوز",
                testTag = "payment_tracking_code_input"
            )

            PersianTextField(
                value = description,
                onValueChange = { description = it },
                label = "توضیحات تراکنش",
                placeholder = "بابت پیش‌پرداخت، تسویه نهایی، واریز کارت به کارت و ...",
                singleLine = false,
                testTag = "payment_description_input"
            )
        }

        // Sticky Bottom Action Bar with 2:1 ratio and Dirty state
        StickyBottomActionBar(
            entityName = operationName,
            saveButtonText = "ذخیره $operationName",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckEditDialog(
    check: CheckEntity?,
    customers: List<CustomerEntity>,
    preselectedCustomerId: String? = null,
    currencyUnit: String = "TOMAN",
    warnUnsavedChanges: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (CheckEntity, Boolean) -> Unit
) {
    val isNew = check == null

    var selectedCustomerId by remember {
        mutableStateOf(check?.customerId ?: preselectedCustomerId ?: customers.firstOrNull()?.id ?: "")
    }
    var bankName by remember { mutableStateOf(check?.bankName ?: "ملی") }
    var branch by remember { mutableStateOf(check?.branch ?: "") }
    var checkNumber by remember { mutableStateOf(check?.checkNumber ?: "") }
    var sayadNumber by remember { mutableStateOf(check?.sayadNumber ?: "") }
    var amount by remember { mutableStateOf(check?.amount ?: 0L) }
    var dueDate by remember { mutableStateOf(check?.dueDate ?: JalaliDate.today().formatShort()) }
    var status by remember { mutableStateOf(check?.status ?: "PENDING") } // PENDING, CLEARED, BOUNCED, RETURNED
    var description by remember { mutableStateOf(check?.description ?: "") }

    var amountError by remember { mutableStateOf<String?>(null) }
    var customerError by remember { mutableStateOf<String?>(null) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    val statusOptions = listOf(
        "PENDING" to "در جریان وصول",
        "CLEARED" to "وصول شده",
        "BOUNCED" to "برگشت خورده",
        "RETURNED" to "عودت داده شده"
    )

    val isDirty = remember(selectedCustomerId, bankName, branch, checkNumber, sayadNumber, amount, dueDate, status, description) {
        if (isNew) {
            amount > 0L || checkNumber.isNotBlank() || sayadNumber.isNotBlank() || description.isNotBlank() || selectedCustomerId != (preselectedCustomerId ?: customers.firstOrNull()?.id ?: "")
        } else {
            selectedCustomerId != (check?.customerId ?: "") ||
                    bankName != (check?.bankName ?: "") ||
                    branch != (check?.branch ?: "") ||
                    checkNumber != (check?.checkNumber ?: "") ||
                    sayadNumber != (check?.sayadNumber ?: "") ||
                    amount != (check?.amount ?: 0L) ||
                    dueDate != (check?.dueDate ?: "") ||
                    status != (check?.status ?: "") ||
                    description != (check?.description ?: "")
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
        if (selectedCustomerId.isEmpty()) {
            customerError = "انتخاب مشتری الزامی است."
            hasError = true
        }
        if (amount <= 0) {
            amountError = "مبلغ چک باید بیشتر از صفر باشد."
            hasError = true
        }

        if (!hasError) {
            val entity = check?.copy(
                customerId = selectedCustomerId,
                bankName = bankName.trim(),
                branch = branch.trim(),
                checkNumber = checkNumber.trim(),
                sayadNumber = sayadNumber.trim(),
                amount = amount,
                dueDate = dueDate,
                status = status,
                description = description.trim()
            ) ?: CheckEntity(
                id = UUID.randomUUID().toString(),
                customerId = selectedCustomerId,
                bankName = bankName.trim(),
                branch = branch.trim(),
                checkNumber = checkNumber.trim(),
                sayadNumber = sayadNumber.trim(),
                amount = amount,
                dueDate = dueDate,
                status = status,
                description = description.trim()
            )
            onSave(entity, isNew)
        }
    }

    AppFormBottomSheet(
        title = if (isNew) "ثبت چک صیادی جدید" else "ویرایش چک صیادی",
        subtitle = "ثبت مشخصات چک بانکی، شناسه صیاد و سررسید",
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
            // Customer selection with Search
            CustomerDropdownField(
                selectedCustomerId = selectedCustomerId,
                customers = customers,
                onCustomerSelected = { cust ->
                    selectedCustomerId = cust.id
                    customerError = null
                },
                label = "مشتری صادرکننده چک",
                placeholder = "انتخاب مشتری",
                isError = customerError != null,
                errorMessage = customerError,
                testTag = "check_customer_dropdown"
            )

            CurrencyInputField(
                amount = amount,
                onAmountChange = {
                    amount = it
                    amountError = null
                },
                label = "مبلغ چک",
                currencyUnit = currencyUnit,
                isError = amountError != null,
                errorMessage = amountError,
                testTag = "check_amount_input"
            )

            PersianDateField(
                dateString = dueDate,
                onDateSelected = { dueDate = it },
                label = "تاریخ سررسید چک"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PersianTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = "نام بانک",
                    placeholder = "ملی، ملت، صادرات...",
                    modifier = Modifier.weight(1f),
                    testTag = "check_bank_input"
                )
                PersianTextField(
                    value = branch,
                    onValueChange = { branch = it },
                    label = "شعبه",
                    placeholder = "کد یا نام شعبه",
                    modifier = Modifier.weight(1f),
                    testTag = "check_branch_input"
                )
            }

            PersianTextField(
                value = sayadNumber,
                onValueChange = { sayadNumber = it },
                label = "شناسه ۱۶ رقمی صیاد",
                placeholder = "شماره صیادی مندرج روی چک",
                testTag = "check_sayad_input"
            )

            // Status dropdown
            val statusLabel = statusOptions.firstOrNull { it.first == status }?.second ?: "در جریان وصول"
            AppDropdownField(
                value = statusLabel,
                label = "وضعیت وصول چک",
                options = statusOptions,
                optionLabel = { it.second },
                onOptionSelected = { status = it.first },
                testTag = "check_status_dropdown"
            )

            PersianTextField(
                value = description,
                onValueChange = { description = it },
                label = "توضیحات چک",
                placeholder = "در وجه، بابت قرارداد تابلوسازی، نام ضامن و ...",
                singleLine = false,
                testTag = "check_description_input"
            )
        }

        // Sticky Bottom Action Bar with 2:1 ratio and Dirty state
        StickyBottomActionBar(
            entityName = "چک صیادی",
            saveButtonText = "ذخیره چک صیادی",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseEditDialog(
    expense: ExpenseEntity?,
    categories: List<ExpenseCategoryEntity>,
    invoices: List<InvoiceEntity> = emptyList(),
    currencyUnit: String = "TOMAN",
    warnUnsavedChanges: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (ExpenseEntity, Boolean) -> Unit,
    onQuickAddCategory: (String, Long) -> Unit
) {
    val isNew = expense == null

    var category by remember { mutableStateOf(expense?.category ?: categories.firstOrNull()?.title ?: "ورق پلکسی گلاس") }
    var amount by remember { mutableStateOf(expense?.amount ?: categories.firstOrNull { it.title == category }?.defaultAmount ?: 0L) }
    var date by remember { mutableStateOf(expense?.date ?: JalaliDate.today().formatShort()) }
    var invoiceId by remember { mutableStateOf(expense?.invoiceId) }
    var description by remember { mutableStateOf(expense?.description ?: "") }
    var amountError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    var showNewCategoryInput by remember { mutableStateOf(false) }
    var newCategoryTitle by remember { mutableStateOf("") }

    val isDirty = remember(category, amount, date, description) {
        if (isNew) {
            amount > 0L || description.isNotBlank()
        } else {
            category != (expense?.category ?: "") ||
                    amount != (expense?.amount ?: 0L) ||
                    date != (expense?.date ?: "") ||
                    description != (expense?.description ?: "")
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
        if (category.trim().isEmpty()) {
            categoryError = "عنوان هزینه الزامی است."
            hasError = true
        }
        if (amount <= 0) {
            amountError = "مبلغ هزینه باید بیشتر از صفر باشد."
            hasError = true
        }

        if (!hasError) {
            val entity = expense?.copy(
                invoiceId = invoiceId,
                category = category.trim(),
                amount = amount,
                date = date,
                description = description.trim()
            ) ?: ExpenseEntity(
                id = UUID.randomUUID().toString(),
                invoiceId = invoiceId,
                category = category.trim(),
                amount = amount,
                date = date,
                description = description.trim()
            )
            onSave(entity, isNew)
        }
    }

    AppFormBottomSheet(
        title = if (isNew) "ثبت هزینه جدید کارگاه" else "ویرایش هزینه کارگاه",
        subtitle = "ثبت مبالغ خریداری‌شده، کرایه‌ها، مصالح و ملزومات",
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
            // Category Selector with quick add button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("عنوان هزینه", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = { showNewCategoryInput = !showNewCategoryInput }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (showNewCategoryInput) "انتخاب از لیست" else "تعریف عنوان جدید")
                }
            }

            if (showNewCategoryInput) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PersianTextField(
                            value = newCategoryTitle,
                            onValueChange = { newCategoryTitle = it },
                            label = "عنوان هزینه جدید",
                            placeholder = "مثال: کرایه بالابر"
                        )
                        Button(
                            onClick = {
                                if (newCategoryTitle.trim().isNotEmpty()) {
                                    onQuickAddCategory(newCategoryTitle.trim(), 0L)
                                    category = newCategoryTitle.trim()
                                    showNewCategoryInput = false
                                    newCategoryTitle = ""
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ثبت و انتخاب این عنوان")
                        }
                    }
                }
            } else {
                AppDropdownField(
                    value = category,
                    onValueChange = {
                        category = it
                        categoryError = null
                    },
                    label = "انتخاب عنوان هزینه",
                    options = categories,
                    optionLabel = { it.title },
                    onOptionSelected = { cat ->
                        category = cat.title
                        if (amount == 0L && cat.defaultAmount > 0) {
                            amount = cat.defaultAmount
                        }
                        categoryError = null
                    },
                    isEditable = true,
                    isError = categoryError != null,
                    errorMessage = categoryError,
                    testTag = "expense_category_dropdown"
                )
            }

            CurrencyInputField(
                amount = amount,
                onAmountChange = {
                    amount = it
                    amountError = null
                },
                label = "مبلغ هزینه",
                currencyUnit = currencyUnit,
                isError = amountError != null,
                errorMessage = amountError,
                testTag = "expense_amount_input"
            )

            PersianDateField(
                dateString = date,
                onDateSelected = { date = it },
                label = "تاریخ پرداخت هزینه"
            )

            PersianTextField(
                value = description,
                onValueChange = { description = it },
                label = "توضیحات هزینه",
                placeholder = "نام فروشنده، شماره فاکتور خرید، متراژ ورق و ...",
                singleLine = false,
                testTag = "expense_description_input"
            )
        }

        // Sticky Bottom Action Bar with 2:1 ratio and Dirty state
        StickyBottomActionBar(
            entityName = "هزینه کارگاه",
            saveButtonText = "ذخیره هزینه کارگاه",
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

@Preview(name = "Payment Form Preview", showBackground = true)
@Composable
fun PaymentEditDialogPreview() {
    MyApplicationTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            PaymentEditDialog(
                payment = PaymentEntity(
                    customerId = "c1",
                    type = "RECEIPT",
                    amount = 15000000L,
                    date = "1403/08/20",
                    method = "کارت به کارت",
                    trackingCode = "98234123",
                    description = "پیش‌پرداخت اول تابلوی سوپرمارکت"
                ),
                customers = listOf(CustomerEntity(id = "c1", name = "علی محمدی", phone = "09120000000")),
                onDismiss = {},
                onSave = { _, _ -> }
            )
        }
    }
}

@Preview(name = "Check Form Preview", showBackground = true)
@Composable
fun CheckEditDialogPreview() {
    MyApplicationTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CheckEditDialog(
                check = CheckEntity(
                    customerId = "c1",
                    bankName = "ملت",
                    branch = "ونک",
                    checkNumber = "782312",
                    sayadNumber = "1234567890123456",
                    amount = 30000000L,
                    dueDate = "1403/09/30",
                    status = "PENDING"
                ),
                customers = listOf(CustomerEntity(id = "c1", name = "علی محمدی", phone = "09120000000")),
                onDismiss = {},
                onSave = { _, _ -> }
            )
        }
    }
}

@Preview(name = "Expense Form Preview", showBackground = true)
@Composable
fun ExpenseEditDialogPreview() {
    MyApplicationTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ExpenseEditDialog(
                expense = ExpenseEntity(
                    category = "ورق کامپوزیت",
                    amount = 8500000L,
                    date = "1403/08/18",
                    description = "خرید ۵ شیت کامپوزیت پرمیوم باند نقره‌ای"
                ),
                categories = listOf(ExpenseCategoryEntity(title = "ورق کامپوزیت", defaultAmount = 8500000L)),
                onDismiss = {},
                onSave = { _, _ -> },
                onQuickAddCategory = { _, _ -> }
            )
        }
    }
}

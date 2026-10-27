package com.example.ui.screens.finance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entities.CheckEntity
import com.example.data.local.entities.ExpenseCategoryEntity
import com.example.data.local.entities.ExpenseEntity
import com.example.data.local.entities.PaymentEntity
import com.example.ui.components.common.*
import com.example.ui.theme.MoneyNegative
import com.example.ui.theme.MoneyPositive
import com.example.ui.theme.MoneyWarning
import com.example.ui.theme.PrimaryAmber
import com.example.ui.viewmodels.SignageViewModel
import com.example.util.FormatUtils
import com.example.util.JalaliDate
import java.util.UUID

@Composable
fun FinanceScreen(
    viewModel: SignageViewModel
) {
    val customers by viewModel.customers.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val checks by viewModel.checks.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    val contracts by viewModel.contracts.collectAsState()
    val categories by viewModel.expenseCategories.collectAsState()
    val currencyUnit by viewModel.currencyUnit.collectAsState()
    val warnUnsavedChanges by viewModel.warnUnsavedChanges.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("دریافت از مشتری", "پرداخت به مشتری", "چک‌ها", "هزینه‌های کارگاه", "گزارش مالی")

    var paymentDialogType by remember { mutableStateOf("RECEIPT") }

    // Dialog states
    var showPaymentDialog by remember { mutableStateOf(false) }
    var selectedPaymentForEdit by remember { mutableStateOf<PaymentEntity?>(null) }
    var paymentToDelete by remember { mutableStateOf<PaymentEntity?>(null) }

    var showCheckDialog by remember { mutableStateOf(false) }
    var selectedCheckForEdit by remember { mutableStateOf<CheckEntity?>(null) }
    var checkToDelete by remember { mutableStateOf<CheckEntity?>(null) }

    var showExpenseDialog by remember { mutableStateOf(false) }
    var selectedExpenseForEdit by remember { mutableStateOf<ExpenseEntity?>(null) }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }

    val fabColor = when (selectedTabIndex) {
        0 -> MoneyPositive
        1 -> MoneyNegative
        else -> MaterialTheme.colorScheme.primary
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = "امور مالی و حسابداری",
                subtitle = "مدیریت دریافت، پرداخت، چک‌ها و هزینه‌ها"
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTabIndex) {
                        0 -> {
                            paymentDialogType = "RECEIPT"
                            showPaymentDialog = true
                        }
                        1 -> {
                            paymentDialogType = "PAYMENT"
                            showPaymentDialog = true
                        }
                        2 -> showCheckDialog = true
                        3 -> showExpenseDialog = true
                        else -> {
                            paymentDialogType = "RECEIPT"
                            showPaymentDialog = true
                        }
                    }
                },
                containerColor = fabColor,
                contentColor = androidx.compose.ui.graphics.Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("finance_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "افزودن سند مالی")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTabIndex) {
                0 -> { // Receipts from Customers (دریافت از مشتری)
                    val receipts = remember(payments) { payments.filter { it.type == "RECEIPT" } }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Prominent Green Action Button
                        item {
                            Button(
                                onClick = {
                                    paymentDialogType = "RECEIPT"
                                    showPaymentDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MoneyPositive),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("add_receipt_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("دریافت از مشتری", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (receipts.isEmpty()) {
                            item {
                                EmptyStateView(
                                    title = "دریافتی ثبت نشده است",
                                    message = "برای ثبت دریافت نقدی، پوز یا واریزی مشتری، دکمه بالا را لمس کنید.",
                                    icon = Icons.Default.Payments,
                                    actionButtonText = "دریافت از مشتری",
                                    onActionClick = {
                                        paymentDialogType = "RECEIPT"
                                        showPaymentDialog = true
                                    }
                                )
                            }
                        } else {
                            items(receipts, key = { it.id }) { pay ->
                                val customer = customers.firstOrNull { it.id == pay.customerId }
                                AppCard {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "دریافت از: ${customer?.name ?: "نامشخص"}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MoneyPositive
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "تاریخ: ${pay.date} • روش: ${pay.method} ${if (pay.trackingCode.isNotEmpty()) "• پیگیری: " + pay.trackingCode else ""}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (pay.description.isNotEmpty()) {
                                                Text(
                                                    text = "توضیح: ${pay.description}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = FormatUtils.formatCurrency(pay.amount, currencyUnit),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MoneyPositive
                                            )
                                            CardOverflowMenu(
                                                onEdit = {
                                                    paymentDialogType = "RECEIPT"
                                                    selectedPaymentForEdit = pay
                                                },
                                                onDelete = { paymentToDelete = pay }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> { // Payments to Customers (پرداخت به مشتری)
                    val customerPayments = remember(payments) { payments.filter { it.type == "PAYMENT" } }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Prominent Red Action Button
                        item {
                            Button(
                                onClick = {
                                    paymentDialogType = "PAYMENT"
                                    showPaymentDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MoneyNegative),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("add_payment_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("پرداخت به مشتری", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (customerPayments.isEmpty()) {
                            item {
                                EmptyStateView(
                                    title = "پرداختی به مشتری ثبت نشده است",
                                    message = "برای ثبت استرداد وجه یا پرداخت به مشتری، دکمه بالا را لمس کنید.",
                                    icon = Icons.Default.Payments,
                                    actionButtonText = "پرداخت به مشتری",
                                    onActionClick = {
                                        paymentDialogType = "PAYMENT"
                                        showPaymentDialog = true
                                    }
                                )
                            }
                        } else {
                            items(customerPayments, key = { it.id }) { pay ->
                                val customer = customers.firstOrNull { it.id == pay.customerId }
                                AppCard {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "پرداخت به: ${customer?.name ?: "نامشخص"}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MoneyNegative
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "تاریخ: ${pay.date} • روش: ${pay.method} ${if (pay.trackingCode.isNotEmpty()) "• پیگیری: " + pay.trackingCode else ""}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (pay.description.isNotEmpty()) {
                                                Text(
                                                    text = "توضیح: ${pay.description}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = FormatUtils.formatCurrency(pay.amount, currencyUnit),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MoneyNegative
                                            )
                                            CardOverflowMenu(
                                                onEdit = {
                                                    paymentDialogType = "PAYMENT"
                                                    selectedPaymentForEdit = pay
                                                },
                                                onDelete = { paymentToDelete = pay }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> { // Checks
                    if (checks.isEmpty()) {
                        EmptyStateView(
                            title = "چکی ثبت نشده است",
                            message = "ثبت مشخصات چک‌های صیادی و موعد سررسید آنها",
                            icon = Icons.Default.ReceiptLong,
                            actionButtonText = "ثبت چک جدید",
                            onActionClick = { showCheckDialog = true }
                        )
                    } else {
                        val todayStr = remember { JalaliDate.today().formatShort() }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(checks, key = { it.id }) { chk ->
                                val customer = customers.firstOrNull { it.id == chk.customerId }
                                val isOverdue = chk.status == "PENDING" && chk.dueDate <= todayStr
                                AppCard(
                                    borderColor = if (isOverdue) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "چک ${chk.bankName} - مشتری: ${customer?.name ?: "نامشخص"}",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "سررسید: ${chk.dueDate} ${if (isOverdue) "(سررسید شده!)" else ""}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (chk.sayadNumber.isNotEmpty()) {
                                                    Text("شناسه صیاد: ${chk.sayadNumber}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = FormatUtils.formatCurrency(chk.amount, currencyUnit),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                StatusBadge(
                                                    status = when (chk.status) {
                                                        "CLEARED" -> "وصول شده"
                                                        "BOUNCED" -> "برگشت خورده"
                                                        "RETURNED" -> "عودت داده شده"
                                                        else -> "در جریان وصول"
                                                    }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            if (chk.status == "PENDING") {
                                                TextButton(onClick = {
                                                    viewModel.saveCheck(chk.copy(status = "CLEARED"), isNew = false) {}
                                                }) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MoneyPositive, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("ثبت وصول", color = MoneyPositive)
                                                }
                                                TextButton(onClick = {
                                                    viewModel.saveCheck(chk.copy(status = "BOUNCED"), isNew = false) {}
                                                }) {
                                                    Icon(Icons.Default.Cancel, contentDescription = null, tint = MoneyNegative, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("برگشت", color = MoneyNegative)
                                                }
                                            }
                                            IconButton(onClick = { selectedCheckForEdit = chk }) {
                                                Icon(Icons.Default.Edit, contentDescription = "ویرایش", tint = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton(onClick = { checkToDelete = chk }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> { // Expenses
                    if (expenses.isEmpty()) {
                        EmptyStateView(
                            title = "هزینه‌ای ثبت نشده است",
                            message = "هزینه‌های کارگاه، متریال، بالابر، کارگر و قطعات را برای محاسبه دقیق سود و زیان ثبت کنید.",
                            icon = Icons.Default.MoneyOff,
                            actionButtonText = "ثبت هزینه جدید",
                            onActionClick = { showExpenseDialog = true }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(expenses, key = { it.id }) { exp ->
                                AppCard {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(exp.category, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text("تاریخ: ${exp.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (exp.description.isNotEmpty()) {
                                                Text("توضیح: ${exp.description}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = FormatUtils.formatCurrency(exp.amount, currencyUnit),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MoneyNegative
                                            )
                                            Row {
                                                IconButton(onClick = { selectedExpenseForEdit = exp }) {
                                                    Icon(Icons.Default.Edit, contentDescription = "ویرایش", tint = MaterialTheme.colorScheme.primary)
                                                }
                                                IconButton(onClick = { expenseToDelete = exp }) {
                                                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> { // Financial Report & Profit/Loss
                    val totalBilled = contracts.sumOf { it.totalAmount - it.discount }
                    val totalReceipts = payments.filter { it.type == "RECEIPT" }.sumOf { it.amount }
                    val totalRefunds = payments.filter { it.type == "PAYMENT" }.sumOf { it.amount }
                    val totalClearedChecks = checks.filter { it.status == "CLEARED" }.sumOf { it.amount }
                    val totalPendingChecks = checks.filter { it.status == "PENDING" }.sumOf { it.amount }
                    val totalExpenses = expenses.sumOf { it.amount }

                    val netRevenue = totalReceipts + totalClearedChecks - totalRefunds
                    val estimatedProfit = netRevenue - totalExpenses

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            AppCard(borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("گزارش تراز و جریان نقدینگی کارگاه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(14.dp))
                                    DetailItem("کل مبلغ قراردادهای رسمی", FormatUtils.formatCurrency(totalBilled, currencyUnit))
                                    DetailItem("مجموع دریافتی‌های نقدی و پوز", FormatUtils.formatCurrency(totalReceipts, currencyUnit), MoneyPositive)
                                    DetailItem("چک‌های وصول‌شده", FormatUtils.formatCurrency(totalClearedChecks, currencyUnit), MoneyPositive)
                                    DetailItem("چک‌های در انتظار وصول", FormatUtils.formatCurrency(totalPendingChecks, currencyUnit), PrimaryAmber)
                                    DetailItem("هزینه‌ها و مخارج خرید متریال", FormatUtils.formatCurrency(totalExpenses, currencyUnit), MoneyNegative)
                                    Divider(modifier = Modifier.padding(vertical = 10.dp))
                                    DetailItem(
                                        if (estimatedProfit >= 0) "سود ناخالص نقدی محقق‌شده" else "زیان دوره جاری",
                                        FormatUtils.formatCurrency(Math.abs(estimatedProfit), currencyUnit),
                                        if (estimatedProfit >= 0) MoneyPositive else MoneyNegative,
                                        isBold = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Payment Dialogs
    if (showPaymentDialog) {
        PaymentEditDialog(
            payment = null,
            customers = customers,
            initialType = paymentDialogType,
            hideTypeSelector = true,
            currencyUnit = currencyUnit,
            warnUnsavedChanges = warnUnsavedChanges,
            onDismiss = { showPaymentDialog = false },
            onSave = { entity, isNew ->
                viewModel.savePayment(entity, isNew) {
                    showPaymentDialog = false
                }
            }
        )
    }

    selectedPaymentForEdit?.let { pay ->
        PaymentEditDialog(
            payment = pay,
            customers = customers,
            initialType = pay.type,
            hideTypeSelector = true,
            currencyUnit = currencyUnit,
            warnUnsavedChanges = warnUnsavedChanges,
            onDismiss = { selectedPaymentForEdit = null },
            onSave = { entity, isNew ->
                viewModel.savePayment(entity, isNew) {
                    selectedPaymentForEdit = null
                }
            }
        )
    }

    DeleteConfirmationDialog(
        show = paymentToDelete != null,
        title = "حذف سند پرداخت / دریافت",
        message = "آیا از حذف این تراکنش به مبلغ ${FormatUtils.formatCurrency(paymentToDelete?.amount ?: 0L, currencyUnit)} اطمینان دارید؟",
        onConfirm = {
            paymentToDelete?.let { p ->
                viewModel.deletePayment(p) {
                    paymentToDelete = null
                }
            }
        },
        onDismiss = { paymentToDelete = null }
    )

    // Check Dialogs
    if (showCheckDialog) {
        CheckEditDialog(
            check = null,
            customers = customers,
            currencyUnit = currencyUnit,
            warnUnsavedChanges = warnUnsavedChanges,
            onDismiss = { showCheckDialog = false },
            onSave = { entity, isNew ->
                viewModel.saveCheck(entity, isNew) {
                    showCheckDialog = false
                }
            }
        )
    }

    selectedCheckForEdit?.let { chk ->
        CheckEditDialog(
            check = chk,
            customers = customers,
            currencyUnit = currencyUnit,
            warnUnsavedChanges = warnUnsavedChanges,
            onDismiss = { selectedCheckForEdit = null },
            onSave = { entity, isNew ->
                viewModel.saveCheck(entity, isNew) {
                    selectedCheckForEdit = null
                }
            }
        )
    }

    DeleteConfirmationDialog(
        show = checkToDelete != null,
        title = "حذف چک",
        message = "آیا از حذف این برگ چک به مبلغ ${FormatUtils.formatCurrency(checkToDelete?.amount ?: 0L, currencyUnit)} اطمینان دارید؟",
        onConfirm = {
            checkToDelete?.let { c ->
                viewModel.deleteCheck(c) {
                    checkToDelete = null
                }
            }
        },
        onDismiss = { checkToDelete = null }
    )

    // Expense Dialogs
    if (showExpenseDialog) {
        ExpenseEditDialog(
            expense = null,
            categories = categories,
            currencyUnit = currencyUnit,
            warnUnsavedChanges = warnUnsavedChanges,
            onDismiss = { showExpenseDialog = false },
            onSave = { entity, isNew ->
                viewModel.saveExpense(entity, isNew) {
                    showExpenseDialog = false
                }
            },
            onQuickAddCategory = { title, amt ->
                viewModel.saveExpenseCategory(
                    ExpenseCategoryEntity(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        defaultAmount = amt,
                        description = "عنوان هزینه تعریف‌شده در فرم"
                    ),
                    isNew = true
                ) {}
            }
        )
    }

    selectedExpenseForEdit?.let { exp ->
        ExpenseEditDialog(
            expense = exp,
            categories = categories,
            currencyUnit = currencyUnit,
            warnUnsavedChanges = warnUnsavedChanges,
            onDismiss = { selectedExpenseForEdit = null },
            onSave = { entity, isNew ->
                viewModel.saveExpense(entity, isNew) {
                    selectedExpenseForEdit = null
                }
            },
            onQuickAddCategory = { title, amt ->
                viewModel.saveExpenseCategory(
                    ExpenseCategoryEntity(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        defaultAmount = amt,
                        description = "عنوان هزینه تعریف‌شده در فرم"
                    ),
                    isNew = true
                ) {}
            }
        )
    }

    DeleteConfirmationDialog(
        show = expenseToDelete != null,
        title = "حذف هزینه",
        message = "آیا از حذف هزینه ${expenseToDelete?.category} اطمینان دارید؟",
        onConfirm = {
            expenseToDelete?.let { e ->
                viewModel.deleteExpense(e) {
                    expenseToDelete = null
                }
            }
        },
        onDismiss = { expenseToDelete = null }
    )
}

@Composable
private fun DetailItem(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium, color = valueColor)
    }
}

package com.example.ui.screens.finance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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

    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
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
                actions = {
                    IconButton(
                        onClick = { isSearchOpen = !isSearchOpen },
                        modifier = Modifier.testTag("finance_search_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isSearchOpen) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "جستجو",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
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
            AnimatedVisibility(
                visible = isSearchOpen,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Spacer(modifier = Modifier.height(4.dp))
                    AppSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "جستجو در اسناد مالی، طرف حساب یا شماره چک...",
                        testTag = "finance_search_input"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

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
                    val receipts = remember(payments, searchQuery, customers) {
                        var list = payments.filter { it.type == "RECEIPT" }
                        if (searchQuery.isNotBlank()) {
                            val q = searchQuery.trim()
                            list = list.filter { p ->
                                val cust = customers.firstOrNull { it.id == p.customerId }
                                (cust?.name?.contains(q, ignoreCase = true) == true) ||
                                        p.description.contains(q, ignoreCase = true) ||
                                        p.trackingCode.contains(q) ||
                                        p.date.contains(q)
                            }
                        }
                        list
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (receipts.isEmpty()) {
                            item {
                                EmptyStateView(
                                    title = "دریافتی ثبت نشده است",
                                    message = "برای ثبت دریافت نقدی، پوز یا واریزی مشتری، از دکمه شناور (+) استفاده کنید.",
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
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        // Top Row: Customer Name on right (Start), 3-Dots Menu on left (End)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = customer?.name ?: "نامشخص",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MoneyPositive,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(end = 8.dp)
                                            )
                                            CardOverflowMenu(
                                                onEdit = {
                                                    paymentDialogType = "RECEIPT"
                                                    selectedPaymentForEdit = pay
                                                },
                                                onDelete = { paymentToDelete = pay }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Bottom Row: Date & Method on right (Start), Amount on left (End)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "تاریخ: ${pay.date} • روش: ${pay.method}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (pay.description.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "توضیح: ${pay.description}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Text(
                                                text = FormatUtils.formatCurrency(pay.amount, currencyUnit),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MoneyPositive
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> { // Payments to Customers (پرداخت به مشتری)
                    val customerPayments = remember(payments, searchQuery, customers) {
                        var list = payments.filter { it.type == "PAYMENT" }
                        if (searchQuery.isNotBlank()) {
                            val q = searchQuery.trim()
                            list = list.filter { p ->
                                val cust = customers.firstOrNull { it.id == p.customerId }
                                (cust?.name?.contains(q, ignoreCase = true) == true) ||
                                        p.description.contains(q, ignoreCase = true) ||
                                        p.trackingCode.contains(q) ||
                                        p.date.contains(q)
                            }
                        }
                        list
                    }
                    var openedPaymentId by remember { mutableStateOf<String?>(null) }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (customerPayments.isEmpty()) {
                            item {
                                EmptyStateView(
                                    title = if (searchQuery.isNotEmpty()) "پرداختی یافت نشد" else "پرداختی به مشتری ثبت نشده است",
                                    message = "برای ثبت استرداد وجه یا پرداخت به مشتری، از دکمه شناور (+) استفاده کنید.",
                                    icon = Icons.Default.Payments,
                                    actionButtonText = if (searchQuery.isEmpty()) "پرداخت به مشتری" else null,
                                    onActionClick = {
                                        paymentDialogType = "PAYMENT"
                                        showPaymentDialog = true
                                    }
                                )
                            }
                        } else {
                            items(customerPayments, key = { it.id }) { pay ->
                                val customer = customers.firstOrNull { it.id == pay.customerId }
                                SwipeToRevealCard(
                                    isRevealed = (openedPaymentId == pay.id),
                                    onExpanded = { openedPaymentId = pay.id },
                                    onCollapsed = { if (openedPaymentId == pay.id) openedPaymentId = null },
                                    onEdit = {
                                        openedPaymentId = null
                                        paymentDialogType = "PAYMENT"
                                        selectedPaymentForEdit = pay
                                    },
                                    onDelete = {
                                        openedPaymentId = null
                                        paymentToDelete = pay
                                    },
                                    modifier = Modifier.testTag("payment_item_${pay.id}")
                                ) {
                                    AppCard {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            // Top Row: Customer Name on right (Start), Amount on left (End)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = customer?.name ?: "نامشخص",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MoneyNegative,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(end = 8.dp)
                                                )

                                                Text(
                                                    text = FormatUtils.formatCurrency(pay.amount, currencyUnit),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MoneyNegative
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            // Bottom Row: Date & Method on right (Start), Swipe hint on left (End)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "تاریخ: ${pay.date} • روش: ${pay.method}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    if (pay.description.isNotEmpty()) {
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = "توضیح: ${pay.description}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = "برای عملیات بکشید",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> { // Checks (چک‌ها)
                    val filteredChecks = remember(checks, searchQuery, customers) {
                        if (searchQuery.isBlank()) checks
                        else {
                            val q = searchQuery.trim()
                            checks.filter { chk ->
                                val customer = customers.firstOrNull { it.id == chk.customerId }
                                (customer?.name?.contains(q, ignoreCase = true) == true) ||
                                        chk.bankName.contains(q, ignoreCase = true) ||
                                        chk.sayadNumber.contains(q) ||
                                        chk.dueDate.contains(q)
                            }
                        }
                    }
                    if (filteredChecks.isEmpty()) {
                        EmptyStateView(
                            title = if (searchQuery.isNotEmpty()) "چکی یافت نشد" else "چکی ثبت نشده است",
                            message = "ثبت مشخصات چک‌های صیادی و موعد سررسید آنها",
                            icon = Icons.Default.ReceiptLong,
                            actionButtonText = if (searchQuery.isEmpty()) "ثبت چک جدید" else null,
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
                            items(filteredChecks, key = { it.id }) { chk ->
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
                    val filteredExpenses = remember(expenses, searchQuery) {
                        if (searchQuery.isBlank()) expenses
                        else {
                            val q = searchQuery.trim()
                            expenses.filter { exp ->
                                exp.category.contains(q, ignoreCase = true) ||
                                        exp.description.contains(q, ignoreCase = true) ||
                                        exp.date.contains(q)
                            }
                        }
                    }
                    if (filteredExpenses.isEmpty()) {
                        EmptyStateView(
                            title = if (searchQuery.isNotEmpty()) "هزینه‌ای یافت نشد" else "هزینه‌ای ثبت نشده است",
                            message = "هزینه‌های کارگاه، متریال، بالابر، کارگر و قطعات را برای محاسبه دقیق سود و زیان ثبت کنید.",
                            icon = Icons.Default.MoneyOff,
                            actionButtonText = if (searchQuery.isEmpty()) "ثبت هزینه جدید" else null,
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
                            items(filteredExpenses, key = { it.id }) { exp ->
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

@androidx.compose.ui.tooling.preview.Preview(
    name = "Finance Screen Preview",
    showBackground = true,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun FinanceScreenPreview() {
    com.example.ui.theme.MyApplicationTheme(themeMode = "LIGHT") {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
        ) {
            Surface(color = MaterialTheme.colorScheme.background) {
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        AppTopBar(
                            title = "امور مالی و حسابداری",
                            actions = {
                                IconButton(onClick = {}) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "جستجو",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {},
                            containerColor = MoneyPositive,
                            contentColor = androidx.compose.ui.graphics.Color.White,
                            shape = RoundedCornerShape(18.dp)
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
                            selectedTabIndex = 0,
                            edgePadding = 16.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("دریافت از مشتری", "پرداخت به مشتری", "چک‌ها", "هزینه‌های کارگاه", "گزارش مالی").forEachIndexed { index, title ->
                                Tab(
                                    selected = index == 0,
                                    onClick = {},
                                    text = { Text(title, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Button(
                                onClick = {},
                                colors = ButtonDefaults.buttonColors(containerColor = MoneyPositive),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("دریافت از مشتری", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            AppCard {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "دریافت از: مهندس علیرضا رضایی",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MoneyPositive
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "تاریخ: ۱۴۰۳/۰۸/۱۲ • روش: کارت به کارت • پیگیری: ۹۸۷۶۵۴",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = FormatUtils.formatCurrency(15000000L, "TOMAN"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MoneyPositive
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


package com.example.ui.screens.customers

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
import com.example.data.local.entities.CustomerEntity
import com.example.ui.components.common.*
import com.example.ui.navigation.Screen
import com.example.ui.theme.MoneyNegative
import com.example.ui.theme.MoneyPositive
import com.example.ui.viewmodels.SignageViewModel
import com.example.util.FormatUtils

@Composable
fun CustomerDetailScreen(
    customerId: String,
    viewModel: SignageViewModel,
    onBack: () -> Unit,
    onNavigateToProjectDetail: (String) -> Unit
) {
    val customer by viewModel.repository.getCustomerByIdFlow(customerId).collectAsState(initial = null)
    val projects by viewModel.repository.getProjectsForCustomer(customerId).collectAsState(initial = emptyList())
    val payments by viewModel.repository.getPaymentsForCustomer(customerId).collectAsState(initial = emptyList())
    val checks by viewModel.repository.getChecksForCustomer(customerId).collectAsState(initial = emptyList())
    val contracts by viewModel.repository.getContractsForCustomer(customerId).collectAsState(initial = emptyList())
    val summary by viewModel.getCustomerFinancialSummary(customerId).collectAsState(initial = null)
    val currencyUnit by viewModel.currencyUnit.collectAsState()
    val warnUnsaved by viewModel.warnUnsavedChanges.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("پروژه‌ها", "فاکتور و قرارداد", "پرداخت‌ها", "چک‌ها", "تراز مالی")

    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = "پرونده مشتری",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "ویرایش مشتری")
                    }
                }
            )
        }
    ) { padding ->
        if (customer == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val cust = customer!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Header Card: Summary & Balance
                AppCard(
                    modifier = Modifier.padding(16.dp),
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = cust.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "تلفن: ${FormatUtils.formatPhoneNumber(cust.phone)} | کد ملی: ${cust.nationalCode.ifEmpty { "قید نشده" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            summary?.let { s ->
                                val (statusText, statusColor) = when {
                                    s.balance > 0 -> "بدهکار" to MoneyNegative
                                    s.balance < 0 -> "بستانکار" to MoneyPositive
                                    else -> "تسویه" to MoneyPositive
                                }
                                Surface(
                                    color = statusColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        if (cust.address.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "نشانی: ${cust.address}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (cust.description.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "توضیحات: ${cust.description}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(12.dp))

                        summary?.let { s ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("کل قراردادها", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(FormatUtils.formatCurrency(s.totalInvoiced, currencyUnit), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("دریافتی نقدی", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(FormatUtils.formatCurrency(s.totalPaid, currencyUnit), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MoneyPositive)
                                }
                                Column {
                                    Text("مانده حساب", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = FormatUtils.formatCurrency(Math.abs(s.balance), currencyUnit),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (s.balance > 0) MoneyNegative else MoneyPositive
                                    )
                                }
                            }
                        }
                    }
                }

                // Tab Row
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content
                when (selectedTabIndex) {
                    0 -> { // Projects
                        if (projects.isEmpty()) {
                            EmptyStateView(
                                title = "پروژه‌ای برای این مشتری ثبت نشده",
                                message = "می‌توانید از منوی پروژه‌ها برای این کارفرما پروژه جدید ثبت نمایید.",
                                icon = Icons.Default.WorkOutline
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(projects, key = { it.id }) { proj ->
                                    AppCard(onClick = { onNavigateToProjectDetail(proj.id) }) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(proj.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Text("شروع: ${proj.startDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            StatusBadge(status = proj.status)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> { // Invoices & Contracts
                        if (contracts.isEmpty()) {
                            EmptyStateView(
                                title = "قراردادی ثبت نشده است",
                                message = "در صفحه پروژه می‌توانید قرارداد و فاکتور پروژه را تنظیم کنید.",
                                icon = Icons.Default.Description
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(contracts, key = { it.id }) { con ->
                                    AppCard {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("قرارداد مورخه ${con.contractDate}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text(FormatUtils.formatCurrency(con.totalAmount - con.discount, currencyUnit), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("پیش‌پرداخت: ${FormatUtils.formatCurrency(con.prepayment, currencyUnit)} | مدت اجرا: ${con.durationDays} روز", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> { // Payments
                        if (payments.isEmpty()) {
                            EmptyStateView(
                                title = "پرداخت یا دریافتی ثبت نشده است",
                                message = "از بخش امور مالی یا دکمه سریع داشبورد دریافتی جدید ثبت کنید.",
                                icon = Icons.Default.Payments
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(payments, key = { it.id }) { pay ->
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
                                                    if (pay.type == "RECEIPT") "دریافت از مشتری" else "استرداد/پرداخت به مشتری",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (pay.type == "RECEIPT") MoneyPositive else MoneyNegative
                                                )
                                                Text("روش: ${pay.method} • تاریخ: ${pay.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                if (pay.trackingCode.isNotEmpty()) {
                                                    Text("کد پیگیری: ${pay.trackingCode}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            Text(
                                                text = FormatUtils.formatCurrency(pay.amount, currencyUnit),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (pay.type == "RECEIPT") MoneyPositive else MoneyNegative
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    3 -> { // Checks
                        if (checks.isEmpty()) {
                            EmptyStateView(
                                title = "چکی برای این مشتری ثبت نشده",
                                message = "می‌توانید چک‌های صیادی مشتری را در بخش امور مالی ثبت کنید.",
                                icon = Icons.Default.ReceiptLong
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(checks, key = { it.id }) { chk ->
                                    AppCard {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("بانک: ${chk.bankName.ifEmpty { "نامشخص" }} (سررسید: ${chk.dueDate})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text("شماره صیاد: ${chk.sayadNumber.ifEmpty { chk.checkNumber.ifEmpty { "ثبت نشده" } }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(FormatUtils.formatCurrency(chk.amount, currencyUnit), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                StatusBadge(
                                                    status = when (chk.status) {
                                                        "CLEARED" -> "وصول شده"
                                                        "BOUNCED" -> "برگشت خورده"
                                                        else -> "در جریان وصول"
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    4 -> { // Financial Summary details
                        summary?.let { s ->
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    AppCard {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text("صورت‌حساب تجمیعی مشتری", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            DetailRow("مجموع کل فاکتورها و قراردادها", FormatUtils.formatCurrency(s.totalInvoiced, currencyUnit))
                                            DetailRow("کل پرداختی‌های نقدی و کارت", FormatUtils.formatCurrency(s.totalPaid, currencyUnit), MoneyPositive)
                                            DetailRow("چک‌های وصول‌شده", FormatUtils.formatCurrency(s.totalClearedChecks, currencyUnit), MoneyPositive)
                                            DetailRow("چک‌های منتظر وصول", FormatUtils.formatCurrency(s.pendingChecksTotal, currencyUnit), MaterialTheme.colorScheme.primary)
                                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                                            DetailRow(
                                                if (s.balance > 0) "مانده بدهی نهایی مشتری" else "مبلغ بستانکاری مشتری",
                                                FormatUtils.formatCurrency(Math.abs(s.balance), currencyUnit),
                                                if (s.balance > 0) MoneyNegative else MoneyPositive,
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
        }
    }

    if (showEditDialog && customer != null) {
        CustomerEditDialog(
            customer = customer,
            warnUnsavedChanges = warnUnsaved,
            onDismiss = { showEditDialog = false },
            onSave = { entity, isNew ->
                viewModel.saveCustomer(entity, isNew) {
                    showEditDialog = false
                }
            }
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium, color = valueColor)
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Customer Detail Screen Preview",
    showBackground = true,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun CustomerDetailScreenPreview() {
    com.example.ui.theme.MyApplicationTheme(themeMode = "LIGHT") {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
        ) {
            Surface(color = MaterialTheme.colorScheme.background) {
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        AppTopBar(
                            title = "پرونده مشتری",
                            onBack = {},
                            actions = {
                                IconButton(onClick = {}) {
                                    Icon(Icons.Default.Edit, contentDescription = "ویرایش مشتری")
                                }
                            }
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        AppCard(
                            modifier = Modifier.padding(16.dp),
                            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "مهندس علیرضا رضایی",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "تلفن: ${FormatUtils.formatPhoneNumber("09121112233")} | کد ملی: ۰۰۱۴۵۵۶۷۸۹",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        color = MoneyNegative.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "بدهکار",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MoneyNegative,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "نشانی: تهران، خیابان شریعتی، مجتمع میلاد",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        ScrollableTabRow(
                            selectedTabIndex = 0,
                            edgePadding = 16.dp
                        ) {
                            listOf("پروژه‌ها", "فاکتور و قرارداد", "پرداخت‌ها", "چک‌ها", "تراز مالی").forEachIndexed { index, tab ->
                                Tab(
                                    selected = index == 0,
                                    onClick = {},
                                    text = { Text(tab, fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Text("محتوای اطلاعات مشتری...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}


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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.local.entities.*
import com.example.data.repository.CustomerFinancialSummary
import com.example.ui.components.common.*
import com.example.ui.navigation.Screen
import com.example.ui.theme.MoneyNegative
import com.example.ui.theme.MoneyPositive
import com.example.ui.theme.MyApplicationTheme
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
                CustomerHeaderCard(
                    customer = cust,
                    summary = summary,
                    currencyUnit = currencyUnit,
                    modifier = Modifier.padding(16.dp)
                )

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
                                    CustomerProjectCard(
                                        project = proj,
                                        onClick = { onNavigateToProjectDetail(proj.id) }
                                    )
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
                                    CustomerContractCard(
                                        contract = con,
                                        currencyUnit = currencyUnit
                                    )
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
                                    CustomerPaymentCard(
                                        payment = pay,
                                        currencyUnit = currencyUnit
                                    )
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
                                    CustomerCheckCard(
                                        check = chk,
                                        currencyUnit = currencyUnit
                                    )
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
                                    CustomerFinancialSummaryCard(
                                        summary = s,
                                        currencyUnit = currencyUnit
                                    )
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

// =========================================================================
// Modular Cards
// =========================================================================

@Composable
fun CustomerHeaderCard(
    customer: CustomerEntity,
    summary: CustomerFinancialSummary?,
    currencyUnit: String,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier,
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
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "شماره تماس: ${FormatUtils.formatPhoneNumber(customer.phone)} | کد ملی: ${customer.nationalCode.ifEmpty { "قید نشده" }}",
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

            if (customer.address.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "نشانی: ${customer.address}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (customer.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "توضیحات: ${customer.description}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
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
}

@Composable
fun CustomerProjectCard(
    project: ProjectEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(project.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("شروع: ${project.startDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusBadge(status = project.status)
        }
    }
}

@Composable
fun CustomerContractCard(
    contract: ContractEntity,
    currencyUnit: String,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("قرارداد مورخه ${contract.contractDate}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(FormatUtils.formatCurrency(contract.totalAmount - contract.discount, currencyUnit), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("پیش‌پرداخت: ${FormatUtils.formatCurrency(contract.prepayment, currencyUnit)} | مدت اجرا: ${contract.durationDays} روز", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CustomerPaymentCard(
    payment: PaymentEntity,
    currencyUnit: String,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    if (payment.type == "RECEIPT") "دریافت از مشتری" else "استرداد/پرداخت به مشتری",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (payment.type == "RECEIPT") MoneyPositive else MoneyNegative
                )
                Text("روش: ${payment.method} • تاریخ: ${payment.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (payment.trackingCode.isNotEmpty()) {
                    Text("کد پیگیری: ${payment.trackingCode}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                text = FormatUtils.formatCurrency(payment.amount, currencyUnit),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (payment.type == "RECEIPT") MoneyPositive else MoneyNegative
            )
        }
    }
}

@Composable
fun CustomerCheckCard(
    check: CheckEntity,
    currencyUnit: String,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("بانک: ${check.bankName.ifEmpty { "نامشخص" }} (سررسید: ${check.dueDate})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("شماره صیاد: ${check.sayadNumber.ifEmpty { check.checkNumber.ifEmpty { "ثبت نشده" } }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(FormatUtils.formatCurrency(check.amount, currencyUnit), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                StatusBadge(
                    status = when (check.status) {
                        "CLEARED" -> "وصول شده"
                        "BOUNCED" -> "برگشت خورده"
                        else -> "در جریان وصول"
                    }
                )
            }
        }
    }
}

@Composable
fun CustomerFinancialSummaryCard(
    summary: CustomerFinancialSummary,
    currencyUnit: String,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("صورت‌حساب تجمیعی مشتری", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            DetailRow("مجموع کل فاکتورها و قراردادها", FormatUtils.formatCurrency(summary.totalInvoiced, currencyUnit))
            DetailRow("کل پرداختی‌های نقدی و کارت", FormatUtils.formatCurrency(summary.totalPaid, currencyUnit), MoneyPositive)
            DetailRow("چک‌های وصول‌شده", FormatUtils.formatCurrency(summary.totalClearedChecks, currencyUnit), MoneyPositive)
            DetailRow("چک‌های منتظر وصول", FormatUtils.formatCurrency(summary.pendingChecksTotal, currencyUnit), MaterialTheme.colorScheme.primary)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            DetailRow(
                if (summary.balance > 0) "مانده بدهی نهایی مشتری" else "مبلغ بستانکاری مشتری",
                FormatUtils.formatCurrency(Math.abs(summary.balance), currencyUnit),
                if (summary.balance > 0) MoneyNegative else MoneyPositive,
                isBold = true
            )
        }
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

// =========================================================================
// Previews (Full Screen + Individual Cards)
// =========================================================================

@Preview(
    name = "1. Customer Detail Full Screen",
    showBackground = true,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun CustomerDetailScreenPreview() {
    MyApplicationTheme(themeMode = "LIGHT") {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl
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
                        val sampleCustomer = CustomerEntity(
                            id = "c1",
                            name = "مهندس علیرضا رضایی",
                            phone = "09121112233",
                            nationalCode = "0014556789",
                            address = "تهران، خیابان شریعتی، مجتمع میلاد"
                        )
                        val sampleSummary = CustomerFinancialSummary(
                            customerId = "c1",
                            totalInvoiced = 145000000L,
                            totalPaid = 85000000L,
                            totalClearedChecks = 30000000L,
                            pendingChecksTotal = 30000000L,
                            balance = 30000000L
                        )

                        CustomerHeaderCard(
                            customer = sampleCustomer,
                            summary = sampleSummary,
                            currencyUnit = "TOMAN",
                            modifier = Modifier.padding(16.dp)
                        )

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

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                CustomerProjectCard(
                                    project = ProjectEntity(
                                        id = "p1",
                                        customerId = "c1",
                                        title = "تابلو چلنیوم طلا فروشی کیمیا",
                                        startDate = "1403/08/15",
                                        status = "ACTIVE"
                                    ),
                                    onClick = {}
                                )
                            }
                            item {
                                CustomerProjectCard(
                                    project = ProjectEntity(
                                        id = "p2",
                                        customerId = "c1",
                                        title = "لایت‌باکس ورودی پاساژ کوروش",
                                        startDate = "1403/07/20",
                                        status = "COMPLETED"
                                    ),
                                    onClick = {}
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "2. Header Summary Card", showBackground = true)
@Composable
fun CustomerHeaderCardPreview() {
    MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.padding(16.dp)) {
                CustomerHeaderCard(
                    customer = CustomerEntity(
                        id = "c1",
                        name = "مهندس علیرضا رضایی",
                        phone = "09121112233",
                        nationalCode = "0014556789",
                        address = "تهران، خیابان شریعتی، مجتمع میلاد"
                    ),
                    summary = CustomerFinancialSummary(
                        customerId = "c1",
                        totalInvoiced = 145000000L,
                        totalPaid = 85000000L,
                        totalClearedChecks = 30000000L,
                        pendingChecksTotal = 30000000L,
                        balance = 30000000L
                    ),
                    currencyUnit = "TOMAN"
                )
            }
        }
    }
}

@Preview(name = "3. Project Card", showBackground = true)
@Composable
fun CustomerProjectCardPreview() {
    MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.padding(16.dp)) {
                CustomerProjectCard(
                    project = ProjectEntity(
                        id = "p1",
                        customerId = "c1",
                        title = "تابلو چلنیوم طلا فروشی کیمیا",
                        startDate = "1403/08/15",
                        status = "ACTIVE"
                    ),
                    onClick = {}
                )
            }
        }
    }
}

@Preview(name = "4. Contract Card", showBackground = true)
@Composable
fun CustomerContractCardPreview() {
    MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.padding(16.dp)) {
                CustomerContractCard(
                    contract = ContractEntity(
                        id = "con1",
                        customerId = "c1",
                        projectId = "p1",
                        contractDate = "1403/08/16",
                        durationDays = 20,
                        totalAmount = 85000000L,
                        prepayment = 40000000L,
                        discount = 5000000L
                    ),
                    currencyUnit = "TOMAN"
                )
            }
        }
    }
}

@Preview(name = "5. Payment Card", showBackground = true)
@Composable
fun CustomerPaymentCardPreview() {
    MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.padding(16.dp)) {
                CustomerPaymentCard(
                    payment = PaymentEntity(
                        id = "pay1",
                        customerId = "c1",
                        amount = 40000000L,
                        type = "RECEIPT",
                        method = "کارت به کارت",
                        date = "1403/08/16",
                        trackingCode = "TRK-98214"
                    ),
                    currencyUnit = "TOMAN"
                )
            }
        }
    }
}

@Preview(name = "6. Check Card", showBackground = true)
@Composable
fun CustomerCheckCardPreview() {
    MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.padding(16.dp)) {
                CustomerCheckCard(
                    check = CheckEntity(
                        id = "chk1",
                        customerId = "c1",
                        amount = 30000000L,
                        bankName = "ملت",
                        checkNumber = "123456",
                        sayadNumber = "1234567890123456",
                        dueDate = "1403/09/30",
                        status = "PENDING"
                    ),
                    currencyUnit = "TOMAN"
                )
            }
        }
    }
}

@Preview(name = "7. Financial Summary Card", showBackground = true)
@Composable
fun CustomerFinancialSummaryCardPreview() {
    MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.padding(16.dp)) {
                CustomerFinancialSummaryCard(
                    summary = CustomerFinancialSummary(
                        customerId = "c1",
                        totalInvoiced = 145000000L,
                        totalPaid = 85000000L,
                        totalClearedChecks = 30000000L,
                        pendingChecksTotal = 30000000L,
                        balance = 30000000L
                    ),
                    currencyUnit = "TOMAN"
                )
            }
        }
    }
}


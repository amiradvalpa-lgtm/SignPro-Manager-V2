package com.example.ui.screens.customers

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.CustomerEntity
import com.example.ui.components.common.AppSearchBar
import com.example.ui.components.common.AppTopBar
import com.example.ui.components.common.DeleteConfirmationDialog
import com.example.ui.components.common.EmptyStateView
import com.example.ui.components.common.SwipeToRevealCard
import com.example.ui.theme.MoneyNegative
import com.example.ui.theme.MoneyPositive
import com.example.ui.viewmodels.SignageViewModel
import com.example.util.CallLogHelper
import com.example.util.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class CustomerFilterType {
    ALL,
    DEBTORS,
    SETTLED
}

@Composable
fun CustomersScreen(
    viewModel: SignageViewModel,
    onNavigateToCustomerDetail: (String) -> Unit
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsState()
    val contracts by viewModel.contracts.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val checks by viewModel.checks.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val currencyUnit by viewModel.currencyUnit.collectAsState()
    val warnUnsavedChanges by viewModel.warnUnsavedChanges.collectAsState()

    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(CustomerFilterType.ALL) }

    var selectedCustomerForEdit by remember { mutableStateOf<CustomerEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var customerToDelete by remember { mutableStateOf<CustomerEntity?>(null) }
    var openedCustomerId by remember { mutableStateOf<String?>(null) }

    // Call Log Permission & Lookup State
    var hasCallLogPermission by remember {
        mutableStateOf(CallLogHelper.hasCallLogPermission(context))
    }
    val callLogPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCallLogPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCallLogPermission) {
            callLogPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
        }
    }

    // Load last call timestamp for each customer asynchronously
    var lastCallMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var callLogRefreshTrigger by remember { mutableIntStateOf(0) }

    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        if (CallLogHelper.hasCallLogPermission(context)) {
            hasCallLogPermission = true
            callLogRefreshTrigger++
        }
    }

    LaunchedEffect(hasCallLogPermission, customers, callLogRefreshTrigger) {
        if (hasCallLogPermission && customers.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                val map = mutableMapOf<String, String>()
                for (cust in customers) {
                    val ts = CallLogHelper.getLastCallTimestamp(context, cust.phone)
                    val formatted = CallLogHelper.formatLastCallDate(ts)
                    if (formatted != null) {
                        map[cust.id] = formatted
                    }
                }
                withContext(Dispatchers.Main) {
                    lastCallMap = map
                }
            }
        }
    }

    // Pre-calculate balances per customer
    val customerBalances = remember(customers, contracts, payments, checks) {
        customers.associate { c ->
            val paid = payments.filter { it.customerId == c.id && it.type == "RECEIPT" }.sumOf { it.amount } -
                    payments.filter { it.customerId == c.id && it.type == "PAYMENT" }.sumOf { it.amount }
            val clearedChecks = checks.filter { it.customerId == c.id && it.status == "CLEARED" }.sumOf { it.amount }
            val contractSum = contracts.filter { it.customerId == c.id }.sumOf { it.totalAmount - it.discount }
            c.id to (contractSum - paid - clearedChecks)
        }
    }

    val totalDebtorsCount = remember(customerBalances) {
        customerBalances.values.count { it > 0 }
    }
    val totalSettledCustomersCount = remember(customerBalances) {
        customerBalances.values.count { it <= 0 }
    }
    val totalDebtAmount = remember(customerBalances) {
        customerBalances.values.filter { it > 0 }.sum()
    }
    val totalSettledProjectsCount = remember(projects) {
        projects.count { it.status == "COMPLETED" }
    }

    val filteredCustomers = remember(customers, searchQuery, selectedFilter, customerBalances) {
        var list = customers
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim()
            list = list.filter { it.name.contains(q, ignoreCase = true) || it.phone.contains(q) }
        }
        when (selectedFilter) {
            CustomerFilterType.ALL -> list
            CustomerFilterType.DEBTORS -> list.filter { (customerBalances[it.id] ?: 0L) > 0 }
            CustomerFilterType.SETTLED -> list.filter { (customerBalances[it.id] ?: 0L) <= 0 }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = "مشتریان و کارفرمایان",
                actions = {
                    IconButton(
                        onClick = { isSearchOpen = !isSearchOpen },
                        modifier = Modifier.testTag("customers_search_toggle_btn")
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
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "مشتری جدید")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Animated Search Bar row directly after header
            AnimatedVisibility(
                visible = isSearchOpen,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(4.dp))
                    AppSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "جستجوی نام کارفرما یا شماره تماس...",
                        testTag = "customers_search_input"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Three KPI Summary Cards
            SummaryCardsRow(
                totalCustomers = customers.size,
                totalDebt = totalDebtAmount,
                currencyUnit = currencyUnit,
                settledProjectsCount = totalSettledProjectsCount,
                settledCustomersCount = totalSettledCustomersCount
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Chips
            FilterChipsRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                totalCount = customers.size,
                debtorsCount = totalDebtorsCount,
                settledCount = totalSettledCustomersCount
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Customer List or Empty State
            if (filteredCustomers.isEmpty()) {
                EmptyStateView(
                    title = if (searchQuery.isNotEmpty()) "مشتری یافت نشد" else "هنوز مشتری ثبت نکرده‌اید",
                    message = if (searchQuery.isNotEmpty()) "موردی با عبارت جستجو شده تطابق ندارد." else "برای ثبت اولین کارفرما روی دکمه افزودن کلیک کنید.",
                    icon = Icons.Default.PeopleOutline,
                    actionButtonText = if (searchQuery.isEmpty()) "ثبت مشتری جدید" else null,
                    onActionClick = { showCreateDialog = true }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        val balance = customerBalances[customer.id] ?: 0L
                        CustomerCard(
                            customer = customer,
                            balance = balance,
                            currencyUnit = currencyUnit,
                            lastCallDate = lastCallMap[customer.id],
                            hasCallLogPermission = hasCallLogPermission,
                            onRequestCallLogPermission = {
                                callLogPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
                            },
                            isRevealed = (openedCustomerId == customer.id),
                            onExpanded = { openedCustomerId = customer.id },
                            onCollapsed = { if (openedCustomerId == customer.id) openedCustomerId = null },
                            onClick = { onNavigateToCustomerDetail(customer.id) },
                            onCall = { dialCustomerPhone(context, customer.phone) },
                            onEdit = {
                                openedCustomerId = null
                                selectedCustomerForEdit = customer
                            },
                            onDelete = {
                                openedCustomerId = null
                                customerToDelete = customer
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreateDialog) {
        CustomerEditDialog(
            customer = null,
            warnUnsavedChanges = warnUnsavedChanges,
            onDismiss = { showCreateDialog = false },
            onSave = { entity, isNew ->
                viewModel.saveCustomer(entity, isNew) {
                    showCreateDialog = false
                }
            }
        )
    }

    selectedCustomerForEdit?.let { customer ->
        CustomerEditDialog(
            customer = customer,
            warnUnsavedChanges = warnUnsavedChanges,
            onDismiss = { selectedCustomerForEdit = null },
            onSave = { entity, isNew ->
                viewModel.saveCustomer(entity, isNew) {
                    selectedCustomerForEdit = null
                }
            }
        )
    }

    DeleteConfirmationDialog(
        show = customerToDelete != null,
        title = "حذف مشتری",
        message = "آیا از حذف اطلاعات این کارفرما اطمینان دارید؟ کلیه پروژه‌ها و سوابق مالی این مشتری در سیستم ردیابی خواهند شد.",
        itemDetail = customerToDelete?.let { "${it.name} - ${it.phone}" },
        onConfirm = {
            customerToDelete?.let { cust ->
                viewModel.deleteCustomer(cust) {
                    customerToDelete = null
                }
            }
        },
        onDismiss = { customerToDelete = null }
    )
}

@Composable
private fun SummaryCardsRow(
    totalCustomers: Int,
    totalDebt: Long,
    currencyUnit: String,
    settledProjectsCount: Int,
    settledCustomersCount: Int
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val trackBgColor = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Right card in RTL: کل کارفرمایان
        StatKpiCard(
            modifier = Modifier.weight(1f),
            title = "کل کارفرمایان",
            valueText = FormatUtils.toPersianDigits(totalCustomers.toString()),
            valueSuffix = "نفر",
            icon = Icons.Default.Group,
            iconTint = primaryColor,
            valueColor = onSurfaceColor,
            progressBarColor = primaryColor,
            progressTrackColor = trackBgColor,
            progressRatio = if (totalCustomers > 0) 0.75f else 0f
        )

        // Center card in RTL: مجموع مطالبات
        StatKpiCard(
            modifier = Modifier.weight(1f),
            title = "مجموع مطالبات",
            valueText = FormatUtils.toPersianDigits(FormatUtils.formatNumber(totalDebt)),
            valueSubText = "${FormatUtils.getCurrencyLabel(currencyUnit)}",
            icon = Icons.Default.TrendingUp,
            iconTint = MoneyNegative,
            valueColor = MoneyNegative,
            progressBarColor = MoneyNegative,
            progressTrackColor = MoneyNegative.copy(alpha = 0.18f),
            progressRatio = if (totalDebt > 0) 0.65f else 0f
        )

        // Left card in RTL: تسویه‌شده‌ها
        StatKpiCard(
            modifier = Modifier.weight(1f),
            title = "تسویه‌شده‌ها",
            valueText = FormatUtils.toPersianDigits(
                if (settledProjectsCount > 0) settledProjectsCount.toString()
                else settledCustomersCount.toString()
            ),
            valueSuffix = if (settledProjectsCount > 0) "پروژه" else "نفر",
            icon = Icons.Default.CheckCircle,
            iconTint = MoneyPositive,
            valueColor = MoneyPositive,
            progressBarColor = MoneyPositive,
            progressTrackColor = MoneyPositive.copy(alpha = 0.18f),
            progressRatio = 1f
        )
    }
}

@Composable
private fun StatKpiCard(
    modifier: Modifier = Modifier,
    title: String,
    valueText: String,
    valueSuffix: String? = null,
    valueSubText: String? = null,
    icon: ImageVector,
    iconTint: Color,
    valueColor: Color,
    progressBarColor: Color,
    progressTrackColor: Color,
    progressRatio: Float
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            // Header: Title on Start (Right in RTL) + Icon on End (Left in RTL)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Value + Suffix / SubText
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = valueText,
                    fontSize = if (valueText.length > 8) 13.sp else 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = valueColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!valueSuffix.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = valueSuffix,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!valueSubText.isNullOrBlank()) {
                Text(
                    text = valueSubText,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Spacer(modifier = Modifier.height(13.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Rounded Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(progressTrackColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressRatio.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(progressBarColor)
                )
            }
        }
    }
}

@Composable
private fun FilterChipsRow(
    selectedFilter: CustomerFilterType,
    onFilterSelected: (CustomerFilterType) -> Unit,
    totalCount: Int,
    debtorsCount: Int,
    settledCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val items = listOf(
            Triple(CustomerFilterType.ALL, "همه (${FormatUtils.toPersianDigits(totalCount.toString())})", "all_filter_chip"),
            Triple(CustomerFilterType.DEBTORS, "بدهکاران (${FormatUtils.toPersianDigits(debtorsCount.toString())})", "debtors_filter_chip"),
            Triple(CustomerFilterType.SETTLED, "تسویه‌شده (${FormatUtils.toPersianDigits(settledCount.toString())})", "settled_filter_chip")
        )

        for ((type, label, tag) in items) {
            val isSelected = selectedFilter == type
            Surface(
                onClick = { onFilterSelected(type) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag(tag)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerCard(
    customer: CustomerEntity,
    balance: Long,
    currencyUnit: String,
    lastCallDate: String?,
    hasCallLogPermission: Boolean,
    onRequestCallLogPermission: () -> Unit,
    isRevealed: Boolean = false,
    onExpanded: (() -> Unit)? = null,
    onCollapsed: (() -> Unit)? = null,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    SwipeToRevealCard(
        isRevealed = isRevealed,
        onExpanded = onExpanded,
        onCollapsed = onCollapsed,
        onEdit = onEdit,
        onDelete = onDelete,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_item_${customer.id}")
    ) {
        Card(
            onClick = onClick,
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Right in RTL: Avatar
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 2. Center Info: Name + Financial status (Start-aligned for Persian RTL)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Financial Status Line
                    if (balance > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = MoneyNegative,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "بدهکار: ${FormatUtils.formatCurrency(balance, currencyUnit)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MoneyNegative
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MoneyPositive,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "تسویه‌شده کامل",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MoneyPositive
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // 3. Left in RTL: Action Button (Call) with Last Call Date underneath
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Call Button
                    Surface(
                        onClick = onCall,
                        shape = RoundedCornerShape(12.dp),
                        color = MoneyPositive.copy(alpha = 0.16f),
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("customer_call_btn_${customer.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "تماس تلفنی",
                                tint = MoneyPositive,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Last Contact Date Line (pure date without "تماس: ")
                    val callInfoText = when {
                        lastCallDate != null -> lastCallDate
                        !hasCallLogPermission -> "بدون دسترسی"
                        else -> "ثبت نشده"
                    }
                    Text(
                        text = callInfoText,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun dialCustomerPhone(context: Context, rawPhone: String) {
    val english = CallLogHelper.toEnglishDigits(rawPhone)
    val clean = english.replace(Regex("[^0-9+]"), "").trim()
    if (clean.isNotBlank()) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean"))
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Customers Screen Light Preview",
    showBackground = true,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun CustomersScreenPreviewLight() {
    com.example.ui.theme.MyApplicationTheme(themeMode = "LIGHT") {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
        ) {
            Surface(color = MaterialTheme.colorScheme.background) {
                CustomersPreviewContent()
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Customers Screen Dark Preview",
    showBackground = true,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun CustomersScreenPreviewDark() {
    com.example.ui.theme.MyApplicationTheme(themeMode = "DARK") {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
        ) {
            Surface(color = MaterialTheme.colorScheme.background) {
                CustomersPreviewContent()
            }
        }
    }
}

@Composable
private fun CustomersPreviewContent() {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = "مشتریان و کارفرمایان",
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
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "مشتری جدید")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))
            SummaryCardsRow(
                totalCustomers = 24,
                totalDebt = 18500000L,
                currencyUnit = "TOMAN",
                settledProjectsCount = 16,
                settledCustomersCount = 16
            )
            Spacer(modifier = Modifier.height(14.dp))
            FilterChipsRow(
                selectedFilter = CustomerFilterType.ALL,
                onFilterSelected = {},
                totalCount = 24,
                debtorsCount = 8,
                settledCount = 16
            )
            Spacer(modifier = Modifier.height(14.dp))
            CustomerCard(
                customer = CustomerEntity(id = "1", name = "مهندس علیرضا رضایی", phone = "09121112233"),
                balance = 4250000L,
                currencyUnit = "TOMAN",
                lastCallDate = null,
                hasCallLogPermission = true,
                onRequestCallLogPermission = {},
                onClick = {},
                onCall = {},
                onEdit = {},
                onDelete = {}
            )
            Spacer(modifier = Modifier.height(10.dp))
            CustomerCard(
                customer = CustomerEntity(id = "2", name = "خانم سارا محمدی", phone = "09123334455"),
                balance = 0L,
                currencyUnit = "TOMAN",
                lastCallDate = "۱۴۰۴/۰۵/۲۴",
                hasCallLogPermission = true,
                onRequestCallLogPermission = {},
                onClick = {},
                onCall = {},
                onEdit = {},
                onDelete = {}
            )
        }
    }
}



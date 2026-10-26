package com.example.ui.screens.customers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.ui.theme.MoneyWarning
import com.example.ui.viewmodels.SignageViewModel
import com.example.util.FormatUtils

@Composable
fun CustomersScreen(
    viewModel: SignageViewModel,
    onNavigateToCustomerDetail: (String) -> Unit
) {
    val customers by viewModel.customers.collectAsState()
    val currencyUnit by viewModel.currencyUnit.collectAsState()
    val warnUnsavedChanges by viewModel.warnUnsavedChanges.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCustomerForEdit by remember { mutableStateOf<CustomerEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var customerToDelete by remember { mutableStateOf<CustomerEntity?>(null) }

    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else {
            val q = searchQuery.trim()
            customers.filter { it.name.contains(q, ignoreCase = true) || it.phone.contains(q) }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "مشتریان و کارفرمایان",
                subtitle = "${customers.size} مخاطب ثبت شده"
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
            Spacer(modifier = Modifier.height(8.dp))

            // Search bar
            PersianTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "جستجوی مشتری با نام یا شماره تلفن...",
                leadingIcon = Icons.Default.Search,
                testTag = "customers_search_input"
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredCustomers.isEmpty()) {
                EmptyStateView(
                    title = if (searchQuery.isNotEmpty()) "مشتری یافت نشد" else "هنوز مشتری ثبت نکرده‌اید",
                    message = if (searchQuery.isNotEmpty()) "موردی با عبارت جستجو شده تطابق ندارد." else "برای ثبت اولین مشتری روی دکمه افزودن (+) کلیک کنید.",
                    icon = Icons.Default.PeopleOutline,
                    actionButtonText = if (searchQuery.isEmpty()) "ثبت مشتری جدید" else null,
                    onActionClick = { showCreateDialog = true }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        CustomerListItem(
                            customer = customer,
                            viewModel = viewModel,
                            currencyUnit = currencyUnit,
                            onClick = { onNavigateToCustomerDetail(customer.id) },
                            onEdit = { selectedCustomerForEdit = customer },
                            onDelete = { customerToDelete = customer }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
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
        message = "آیا از حذف اطلاعات این مشتری اطمینان دارید؟ کلیه پروژه‌ها و سوابق مالی این مشتری در سیستم ردیابی خواهند شد.",
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
private fun CustomerListItem(
    customer: CustomerEntity,
    viewModel: SignageViewModel,
    currencyUnit: String,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val summary by viewModel.getCustomerFinancialSummary(customer.id).collectAsState(initial = null)

    AppCard(
        onClick = onClick,
        modifier = Modifier.testTag("customer_item_${customer.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = customer.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (summary != null) {
                    val bal = summary?.balance ?: 0L
                    Spacer(modifier = Modifier.height(4.dp))
                    val (badgeText, badgeColor) = when {
                        bal > 0 -> "بدهکار: ${FormatUtils.formatCurrency(bal, currencyUnit)}" to MoneyNegative
                        bal < 0 -> "بستانکار: ${FormatUtils.formatCurrency(-bal, currencyUnit)}" to MoneyPositive
                        else -> "تسویه کامل" to MoneyPositive
                    }
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "ویرایش", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Customer Card Preview", showBackground = true)
@Composable
fun CustomerCardPreview() {
    com.example.ui.theme.MyApplicationTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            com.example.ui.components.common.AppCard(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("فروشگاه بزرگ کوروش", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("09123456789", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

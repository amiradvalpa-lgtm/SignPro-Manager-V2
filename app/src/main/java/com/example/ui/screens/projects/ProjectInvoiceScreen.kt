package com.example.ui.screens.projects

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entities.InvoiceEntity
import com.example.data.local.entities.InvoiceItemEntity
import com.example.ui.components.common.AppCard
import com.example.ui.components.common.AppTopBar
import com.example.ui.components.common.CardOverflowMenu
import com.example.ui.components.common.DeleteConfirmationDialog
import com.example.ui.components.common.EmptyStateView
import com.example.ui.screens.contracts.InvoiceItemEditDialog
import com.example.ui.theme.MoneyNegative
import com.example.ui.theme.MoneyPositive
import com.example.ui.viewmodels.SignageViewModel
import com.example.util.FormatUtils
import com.example.util.JalaliDate
import com.example.util.PdfGenerator
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectInvoiceScreen(
    projectId: String,
    viewModel: SignageViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val project by viewModel.repository.getProjectByIdFlow(projectId).collectAsState(initial = null)
    val invoice by viewModel.getInvoiceForProject(projectId).collectAsState(initial = null)
    val currencyUnit by viewModel.currencyUnit.collectAsState()
    val contractorInfo by viewModel.contractorInfo.collectAsState()
    val warnUnsaved by viewModel.warnUnsavedChanges.collectAsState()

    val invoiceItems = remember(invoice) {
        invoice?.let { inv -> viewModel.getInvoiceItems(inv.id) }
    }?.collectAsState(initial = emptyList())?.value ?: emptyList()

    val invoiceExpenses = remember(invoice) {
        invoice?.let { inv -> viewModel.repository.getExpensesForInvoice(inv.id) }
    }?.collectAsState(initial = emptyList())?.value ?: emptyList()

    val scope = rememberCoroutineScope()

    var showAddItemDialog by remember { mutableStateOf(false) }
    var selectedItemForEdit by remember { mutableStateOf<InvoiceItemEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<InvoiceItemEntity?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "فاکتور",
                subtitle = project?.title,
                onBack = onBack,
                actions = {
                    if (invoiceItems.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val proj = project ?: return@IconButton
                                val inv = invoice ?: return@IconButton
                                scope.launch {
                                    val customer = viewModel.repository.getCustomerById(proj.customerId)
                                    if (customer != null) {
                                        try {
                                            val expensesSum = invoiceExpenses.filter { it.expenseType == "INVOICE" }.sumOf { it.amount }
                                            val pdf = PdfGenerator.generateInvoicePdf(
                                                context = context,
                                                contractor = contractorInfo,
                                                customer = customer,
                                                project = proj,
                                                invoice = inv,
                                                items = invoiceItems,
                                                expensesTotal = expensesSum
                                            )
                                            PdfGenerator.sharePdf(context, pdf, "فاکتور تابلوسازی - ${proj.title}")
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "خطا در چاپ فاکتور: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "اطلاعات کارفرما یافت نشد", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.testTag("print_invoice_pdf_btn")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "چاپ فاکتور PDF", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    IconButton(
                        onClick = {
                            if (invoice == null) {
                                val newInvoice = InvoiceEntity(
                                    id = UUID.randomUUID().toString(),
                                    projectId = projectId,
                                    issueDate = JalaliDate.today().formatShort()
                                )
                                viewModel.saveInvoice(newInvoice, isNew = true) {
                                    showAddItemDialog = true
                                }
                            } else {
                                showAddItemDialog = true
                            }
                        },
                        modifier = Modifier.testTag("add_invoice_item_top_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "افزودن ردیف فاکتور")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (invoice == null) {
                        val newInvoice = InvoiceEntity(
                            id = UUID.randomUUID().toString(),
                            projectId = projectId,
                            issueDate = JalaliDate.today().formatShort()
                        )
                        viewModel.saveInvoice(newInvoice, isNew = true) {
                            showAddItemDialog = true
                        }
                    } else {
                        showAddItemDialog = true
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("افزودن ردیف فاکتور") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_invoice_item")
            )
        }
    ) { padding ->
        if (invoiceItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                EmptyStateView(
                    title = "ردیف فاکتور ثبت نشده است",
                    message = "خدمات و متریال تابلوسازی (برش لیزر، چلنیوم، کامپوزیت، نصب) را اضافه کنید.",
                    icon = Icons.Default.Receipt,
                    actionButtonText = "افزودن اولین ردیف فاکتور",
                    onActionClick = {
                        if (invoice == null) {
                            val newInvoice = InvoiceEntity(
                                id = UUID.randomUUID().toString(),
                                projectId = projectId,
                                issueDate = JalaliDate.today().formatShort()
                            )
                            viewModel.saveInvoice(newInvoice, isNew = true) {
                                showAddItemDialog = true
                            }
                        } else {
                            showAddItemDialog = true
                        }
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ردیف‌های خدمات و اقلام (${invoiceItems.size} ردیف)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(invoiceItems, key = { it.id }) { item ->
                    AppCard {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            // Overflow menu on top-left (per design rule)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                            ) {
                                CardOverflowMenu(
                                    onEdit = { selectedItemForEdit = item },
                                    onDelete = { itemToDelete = item }
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                                    .padding(end = 28.dp)
                            ) {
                                Text(
                                    text = item.serviceType,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                if (item.itemDescription.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.itemDescription,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "مقدار: ${item.quantity} ${item.unit} • فی: ${FormatUtils.formatCurrency(item.unitPrice, currencyUnit)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Text(
                                        text = FormatUtils.formatCurrency(item.calculatedTotal, currencyUnit),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (item.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "توضیحات: ${item.description}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Invoice Financial totals summary
                item {
                    val itemsSum = invoiceItems.sumOf { it.calculatedTotal }
                    val discount = invoice?.discount ?: 0L
                    // Filter expenses: INVOICE expenses directly affect customer invoice
                    val invoiceExpensesSum = invoiceExpenses.filter { it.expenseType == "INVOICE" }.sumOf { it.amount }
                    val finalNet = itemsSum - discount + invoiceExpensesSum

                    AppCard(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "خلاصه مالی فاکتور",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("جمع اقلام خدمات و ساخت:", style = MaterialTheme.typography.bodyMedium)
                                Text(FormatUtils.formatCurrency(itemsSum, currencyUnit), fontWeight = FontWeight.Bold)
                            }
                            if (discount > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("تخفیف:", style = MaterialTheme.typography.bodyMedium)
                                    Text("- ${FormatUtils.formatCurrency(discount, currencyUnit)}", fontWeight = FontWeight.Bold, color = MoneyPositive)
                                }
                            }
                            if (invoiceExpensesSum > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("هزینه‌های فاکتور:", style = MaterialTheme.typography.bodyMedium)
                                    Text("+ ${FormatUtils.formatCurrency(invoiceExpensesSum, currencyUnit)}", fontWeight = FontWeight.Bold, color = MoneyNegative)
                                }
                            }
                            Divider(modifier = Modifier.padding(vertical = 10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("مبلغ کل نهایی فاکتور:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    FormatUtils.formatCurrency(finalNet, currencyUnit),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    if (showAddItemDialog && invoice != null) {
        InvoiceItemEditDialog(
            invoiceId = invoice!!.id,
            item = null,
            currencyUnit = currencyUnit,
            warnUnsavedChanges = warnUnsaved,
            onDismiss = { showAddItemDialog = false },
            onSave = { entity, isNew ->
                viewModel.saveInvoiceItem(entity, isNew) {
                    showAddItemDialog = false
                }
            }
        )
    }

    selectedItemForEdit?.let { item ->
        InvoiceItemEditDialog(
            invoiceId = item.invoiceId,
            item = item,
            currencyUnit = currencyUnit,
            warnUnsavedChanges = warnUnsaved,
            onDismiss = { selectedItemForEdit = null },
            onSave = { entity, isNew ->
                viewModel.saveInvoiceItem(entity, isNew) {
                    selectedItemForEdit = null
                }
            }
        )
    }

    DeleteConfirmationDialog(
        show = itemToDelete != null,
        title = "حذف ردیف فاکتور",
        message = "آیا از حذف این ردیف فاکتور اطمینان دارید؟",
        itemDetail = itemToDelete?.serviceType,
        onConfirm = {
            itemToDelete?.let { itm ->
                viewModel.deleteInvoiceItem(itm) {
                    itemToDelete = null
                }
            }
        },
        onDismiss = { itemToDelete = null }
    )
}

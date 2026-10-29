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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.local.entities.ExpenseEntity
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
import com.example.ui.theme.MyApplicationTheme
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

    ProjectInvoiceContent(
        projectTitle = project?.title,
        invoiceItems = invoiceItems,
        invoiceExpenses = invoiceExpenses,
        discount = invoice?.discount ?: 0L,
        currencyUnit = currencyUnit,
        onBack = onBack,
        onPrintPdf = {
            val proj = project ?: return@ProjectInvoiceContent
            val inv = invoice ?: return@ProjectInvoiceContent
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
        onAddItem = {
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
        onEditItem = { item -> selectedItemForEdit = item },
        onDeleteItem = { item -> itemToDelete = item }
    )

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectInvoiceContent(
    projectTitle: String?,
    invoiceItems: List<InvoiceItemEntity>,
    invoiceExpenses: List<ExpenseEntity>,
    discount: Long,
    currencyUnit: String,
    onBack: () -> Unit,
    onPrintPdf: () -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (InvoiceItemEntity) -> Unit,
    onDeleteItem: (InvoiceItemEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = "فاکتور",
                subtitle = projectTitle,
                onBack = onBack,
                actions = {
                    if (invoiceItems.isNotEmpty()) {
                        IconButton(
                            onClick = onPrintPdf,
                            modifier = Modifier.testTag("print_invoice_pdf_btn")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "چاپ فاکتور PDF", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    IconButton(
                        onClick = onAddItem,
                        modifier = Modifier.testTag("add_invoice_item_top_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "افزودن ردیف فاکتور")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddItem,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("افزودن ردیف فاکتور", fontWeight = FontWeight.Bold) },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
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
                    onActionClick = onAddItem
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
                    InvoiceItemCard(
                        item = item,
                        currencyUnit = currencyUnit,
                        onEdit = { onEditItem(item) },
                        onDelete = { onDeleteItem(item) }
                    )
                }

                item {
                    val itemsSum = invoiceItems.sumOf { it.calculatedTotal }
                    val invoiceExpensesSum = invoiceExpenses.filter { it.expenseType == "INVOICE" }.sumOf { it.amount }
                    InvoiceSummaryCard(
                        itemsSum = itemsSum,
                        discount = discount,
                        invoiceExpensesSum = invoiceExpensesSum,
                        currencyUnit = currencyUnit
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
fun InvoiceItemCard(
    item: InvoiceItemEntity,
    currencyUnit: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                CardOverflowMenu(
                    onEdit = onEdit,
                    onDelete = onDelete
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

@Composable
fun InvoiceSummaryCard(
    itemsSum: Long,
    discount: Long,
    invoiceExpensesSum: Long,
    currencyUnit: String,
    modifier: Modifier = Modifier
) {
    val finalNet = itemsSum - discount + invoiceExpensesSum

    AppCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    ) {
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
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
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

// -------------------------------------------------------------
// PREVIEWS
// -------------------------------------------------------------

@Preview(
    name = "Project Invoice Screen - Phone",
    showBackground = true,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun ProjectInvoiceScreenPreview() {
    MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            ProjectInvoiceContent(
                projectTitle = "تابلو چلنیوم طلایی - فروشگاه صبا",
                invoiceItems = listOf(
                    InvoiceItemEntity(
                        id = "1",
                        invoiceId = "inv1",
                        serviceType = "حروف چلنیوم لبه طلایی",
                        itemDescription = "پلکسی دوبل تایوانی و SMD ضدآب",
                        quantity = 18.5,
                        unit = "متر محیط",
                        unitPrice = 1_850_000L,
                        calculatedTotal = 34_225_000L,
                        description = "گارانتی ۲ ساله ترانس و روشنایی"
                    ),
                    InvoiceItemEntity(
                        id = "2",
                        invoiceId = "inv1",
                        serviceType = "زیرسازی کامپوزیت",
                        itemDescription = "ورق ۰.۴ آلومکس نوک مدادی",
                        quantity = 14.0,
                        unit = "مترمربع",
                        unitPrice = 1_200_000L,
                        calculatedTotal = 16_800_000L,
                        description = "همراه با آهن‌کشی قوطی ۴۰*۴۰"
                    ),
                    InvoiceItemEntity(
                        id = "3",
                        invoiceId = "inv1",
                        serviceType = "نصب و راه‌اندازی",
                        itemDescription = "نصب در ارتفاع طبقه اول با بالابر",
                        quantity = 1.0,
                        unit = "پروژه",
                        unitPrice = 4_500_000L,
                        calculatedTotal = 4_500_000L,
                        description = ""
                    )
                ),
                invoiceExpenses = listOf(
                    ExpenseEntity(
                        id = "e1",
                        projectId = "p1",
                        invoiceId = "inv1",
                        category = "کرایه بالابر و وانت حمل",
                        amount = 1_500_000L,
                        expenseType = "INVOICE",
                        date = "1403/08/12"
                    )
                ),
                discount = 2_000_000L,
                currencyUnit = "تومان",
                onBack = {},
                onPrintPdf = {},
                onAddItem = {},
                onEditItem = {},
                onDeleteItem = {}
            )
        }
    }
}

@Preview(name = "Invoice Item Card Preview", showBackground = true)
@Composable
fun InvoiceItemCardPreview() {
    MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            InvoiceItemCard(
                item = InvoiceItemEntity(
                    id = "1",
                    invoiceId = "inv1",
                    serviceType = "حروف چلنیوم لبه طلایی",
                    itemDescription = "پلکسی دوبل تایوانی و SMD ضدآب",
                    quantity = 18.5,
                    unit = "متر محیط",
                    unitPrice = 1_850_000L,
                    calculatedTotal = 34_225_000L,
                    description = "گارانتی ۲ ساله ترانس و روشنایی"
                ),
                currencyUnit = "تومان",
                onEdit = {},
                onDelete = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Invoice Summary Card Preview", showBackground = true)
@Composable
fun InvoiceSummaryCardPreview() {
    MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            InvoiceSummaryCard(
                itemsSum = 55_525_000L,
                discount = 2_000_000L,
                invoiceExpensesSum = 1_500_000L,
                currencyUnit = "تومان",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

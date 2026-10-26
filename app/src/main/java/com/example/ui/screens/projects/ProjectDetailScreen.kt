package com.example.ui.screens.projects

import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entities.ContractEntity
import com.example.data.local.entities.CustomerEntity
import com.example.ui.components.common.*
import com.example.ui.theme.MoneyNegative
import com.example.ui.theme.MoneyPositive
import com.example.ui.viewmodels.SignageViewModel
import com.example.util.FormatUtils
import com.example.util.JalaliDate
import com.example.util.PdfGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String,
    viewModel: SignageViewModel,
    onBack: () -> Unit,
    onNavigateToContractEditor: (String) -> Unit,
    onNavigateToSpecs: (String) -> Unit,
    onNavigateToInvoice: (String) -> Unit
) {
    val context = LocalContext.current
    val project by viewModel.repository.getProjectByIdFlow(projectId).collectAsState(initial = null)
    val stages by viewModel.stages.collectAsState()
    val specs by viewModel.getSpecsForProject(projectId).collectAsState(initial = emptyList())
    val invoice by viewModel.getInvoiceForProject(projectId).collectAsState(initial = null)
    val contract by viewModel.getContractForProject(projectId).collectAsState(initial = null)
    val currencyUnit by viewModel.currencyUnit.collectAsState()
    val contractorInfo by viewModel.contractorInfo.collectAsState()

    var customer by remember { mutableStateOf<CustomerEntity?>(null) }
    LaunchedEffect(project?.customerId) {
        project?.customerId?.let { cid ->
            customer = viewModel.repository.getCustomerById(cid)
        }
    }

    val invoiceItems = remember(invoice) {
        invoice?.let { inv -> viewModel.getInvoiceItems(inv.id) }
    }?.collectAsState(initial = emptyList())?.value ?: emptyList()

    val projectExpenses = remember(invoice) {
        invoice?.let { inv -> viewModel.repository.getExpensesForInvoice(inv.id) }
    }?.collectAsState(initial = emptyList())?.value ?: emptyList()

    var showProjectEditDialog by remember { mutableStateOf(false) }
    val customers by viewModel.customers.collectAsState()

    // Status dropdown state
    var statusMenuExpanded by remember { mutableStateOf(false) }
    val statusOptions = listOf(
        "ACTIVE" to "جاری",
        "COMPLETED" to "تکمیل‌شده",
        "CANCELLED" to "لغوشده",
        "PENDING" to "معلق"
    )

    Scaffold(
        topBar = {
            AppTopBar(
                title = "پرونده پروژه",
                subtitle = project?.title,
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { showProjectEditDialog = true },
                        modifier = Modifier.testTag("edit_project_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "ویرایش پروژه")
                    }
                }
            )
        }
    ) { padding ->
        if (project == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val proj = project!!
            val completedStageList = remember(proj.completedStageIds) {
                proj.completedStageIds.split(",").filter { it.isNotBlank() }
            }

            // Calculate progress based on displayOrder of highest completed stage
            val sortedStages = remember(stages) { stages.sortedBy { it.displayOrder } }
            val highestCompletedOrder = remember(sortedStages, completedStageList) {
                val completed = sortedStages.filter { completedStageList.contains(it.id) }
                completed.maxOfOrNull { it.displayOrder } ?: 0
            }
            val progressFraction = if (sortedStages.isNotEmpty()) {
                (highestCompletedOrder.toFloat() / sortedStages.size.toFloat()).coerceIn(0f, 1f)
            } else 0f
            val progressPercent = (progressFraction * 100).toInt()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 1: Project Information Overview
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    AppCard(borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = proj.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "کارفرما: ${customer?.name ?: "نامشخص"} (${customer?.phone ?: "بدون تماس"})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "تاریخ شروع: ${proj.startDate}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusBadge(status = proj.status)
                            }

                            if (proj.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "توضیحات: ${proj.description}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(14.dp))

                            // Action buttons: "Print Contract" & "Clause Settings"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (customer == null) {
                                            Toast.makeText(context, "اطلاعات کارفرما بارگذاری نشده است", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val con = contract ?: ContractEntity(
                                            customerId = proj.customerId,
                                            projectId = proj.id,
                                            contractDate = JalaliDate.today().formatShort(),
                                            durationDays = 15,
                                            totalAmount = invoiceItems.sumOf { it.calculatedTotal },
                                            prepayment = (invoiceItems.sumOf { it.calculatedTotal } * 0.5).toLong(),
                                            balanceAmount = (invoiceItems.sumOf { it.calculatedTotal } * 0.5).toLong()
                                        )
                                        try {
                                            val pdfFile = PdfGenerator.generateContractPdf(
                                                context = context,
                                                contractor = contractorInfo,
                                                customer = customer!!,
                                                project = proj,
                                                specs = specs,
                                                durationDays = con.durationDays,
                                                totalAmount = con.totalAmount,
                                                prepayment = con.prepayment,
                                                balanceAmount = con.balanceAmount,
                                                contractDate = con.contractDate,
                                                arbitratorName = con.arbitratorName
                                            )
                                            PdfGenerator.sharePdf(context, pdfFile, "قرارداد رسمی تابلوسازی - ${proj.title}")
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "خطا در چاپ قرارداد: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("print_contract_btn")
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("چاپ قرارداد", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { onNavigateToContractEditor(proj.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تنظیم بندها")
                                }
                            }
                        }
                    }
                }

                // Section 2: Project Status & Stage Management (Directly below Card 1)
                item {
                    AppCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Status Selector Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "وضعیت پروژه",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Box {
                                    OutlinedButton(
                                        onClick = { statusMenuExpanded = true },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        val currentLabel = statusOptions.firstOrNull { it.first == proj.status }?.second ?: "نامشخص"
                                        Text(currentLabel, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }

                                    DropdownMenu(
                                        expanded = statusMenuExpanded,
                                        onDismissRequest = { statusMenuExpanded = false }
                                    ) {
                                        statusOptions.forEach { (statusCode, statusLabel) ->
                                            DropdownMenuItem(
                                                text = { Text(statusLabel) },
                                                onClick = {
                                                    statusMenuExpanded = false
                                                    viewModel.updateProjectStatus(proj.id, statusCode)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(14.dp))

                            // Stage Tracking Header & Progress
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "مراحل اجرایی کارگاه",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "پیشرفت: $progressPercent٪",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Multi-selectable Stage Chips
                            if (sortedStages.isEmpty()) {
                                Text(
                                    text = "مرحله‌ای تعریف نشده است. از تنظیمات مراحل را بیفزایید.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(sortedStages, key = { it.id }) { stage ->
                                        val isCompleted = completedStageList.contains(stage.id)
                                        val stageColor = try {
                                            Color(AndroidColor.parseColor(stage.colorHex))
                                        } catch (_: Exception) {
                                            MaterialTheme.colorScheme.primary
                                        }

                                        FilterChip(
                                            selected = isCompleted,
                                            onClick = {
                                                val updatedList = completedStageList.toMutableList()
                                                if (isCompleted) {
                                                    updatedList.remove(stage.id)
                                                } else {
                                                    updatedList.add(stage.id)
                                                }
                                                // Update completed stages in database
                                                viewModel.updateProjectStages(
                                                    projectId = proj.id,
                                                    stageId = stage.id,
                                                    completedStageIds = updatedList
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = "${stage.displayOrder}. ${stage.title}",
                                                    fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            leadingIcon = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(stageColor)
                                                )
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 3: Summary Card (Financials and Items)
                item {
                    val itemsSum = invoiceItems.sumOf { it.calculatedTotal }
                    val discount = invoice?.discount ?: 0L
                    val invoiceExpensesSum = projectExpenses.filter { it.expenseType == "INVOICE" }.sumOf { it.amount }
                    val workshopExpensesSum = projectExpenses.filter { it.expenseType == "PROJECT" }.sumOf { it.amount }
                    val finalNet = itemsSum - discount + invoiceExpensesSum

                    AppCard(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "جمع‌بندی مالی و اقلام پروژه",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${specs.size} تابلو • ${invoiceItems.size} خدمت",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("جمع فاکتور خدمات و ساخت:", style = MaterialTheme.typography.bodyMedium)
                                Text(FormatUtils.formatCurrency(itemsSum, currencyUnit), fontWeight = FontWeight.Bold)
                            }

                            if (invoiceExpensesSum > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("هزینه‌های فاکتور شده:", style = MaterialTheme.typography.bodyMedium)
                                    Text("+ ${FormatUtils.formatCurrency(invoiceExpensesSum, currencyUnit)}", fontWeight = FontWeight.Bold, color = MoneyNegative)
                                }
                            }

                            if (workshopExpensesSum > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("هزینه‌های کارگاهی و سود/زیان:", style = MaterialTheme.typography.bodyMedium)
                                    Text(FormatUtils.formatCurrency(workshopExpensesSum, currencyUnit), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("مبلغ نهایی پروژه:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = FormatUtils.formatCurrency(finalNet, currencyUnit),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Section 4: Two prominent full-screen access buttons
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "بخش‌های اختصاصی پرونده",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        // Button 1: Specs Screen
                        Card(
                            onClick = { onNavigateToSpecs(proj.id) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_navigate_specs")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Straighten, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "مشخصات تابلوها",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "ابعاد دقیق سانتی‌متر، متریال، طرح‌ها و تصاویر",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${specs.size}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        // Button 2: Invoice Screen
                        Card(
                            onClick = { onNavigateToInvoice(proj.id) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_navigate_invoice")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "فاکتور",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "ردیف‌های خدمات، متراژ، قیمت واحد و چاپ PDF",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${invoiceItems.size}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }

    val warnUnsaved by viewModel.warnUnsavedChanges.collectAsState()

    if (showProjectEditDialog && project != null) {
        ProjectEditDialog(
            project = project,
            customers = customers,
            warnUnsavedChanges = warnUnsaved,
            onDismiss = { showProjectEditDialog = false },
            onSave = { entity, isNew ->
                viewModel.saveProject(entity, isNew) {
                    showProjectEditDialog = false
                }
            }
        )
    }
}

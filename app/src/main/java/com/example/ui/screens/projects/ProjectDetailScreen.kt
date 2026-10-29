package com.example.ui.screens.projects

import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.*
import com.example.ui.components.common.*
import com.example.ui.theme.MoneyNegative
import com.example.ui.theme.MoneyPositive
import com.example.ui.theme.MyApplicationTheme
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
    var showStagesToggleSheet by remember { mutableStateOf(false) }
    val customers by viewModel.customers.collectAsState()

    val itemsSum = remember(invoiceItems) { invoiceItems.sumOf { it.calculatedTotal } }
    val invoiceExpensesSum = remember(projectExpenses) {
        projectExpenses.filter { it.expenseType == "INVOICE" }.sumOf { it.amount }
    }
    val workshopExpensesSum = remember(projectExpenses) {
        projectExpenses.filter { it.expenseType == "PROJECT" }.sumOf { it.amount }
    }
    val discount = invoice?.discount ?: 0L

    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val proj = project!!
        ProjectDetailContent(
            project = proj,
            customer = customer,
            stages = stages,
            specsCount = specs.size,
            invoiceItemsCount = invoiceItems.size,
            itemsSum = itemsSum,
            invoiceExpensesSum = invoiceExpensesSum,
            workshopExpensesSum = workshopExpensesSum,
            discount = discount,
            currencyUnit = currencyUnit,
            onBack = onBack,
            onEditProject = { showProjectEditDialog = true },
            onOpenStagesSheet = { showStagesToggleSheet = true },
            onPrintContract = {
                if (customer == null) {
                    Toast.makeText(context, "اطلاعات کارفرما بارگذاری نشده است", Toast.LENGTH_SHORT).show()
                    return@ProjectDetailContent
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
            onNavigateToContractEditor = { onNavigateToContractEditor(proj.id) },
            onUpdateStatus = { statusCode -> viewModel.updateProjectStatus(proj.id, statusCode) },
            onNavigateToSpecs = { onNavigateToSpecs(proj.id) },
            onNavigateToInvoice = { onNavigateToInvoice(proj.id) }
        )
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

    if (showStagesToggleSheet && project != null) {
        val completedList = remember(project?.completedStageIds) {
            project?.completedStageIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        }
        ProjectStagesToggleSheet(
            projectTitle = project!!.title,
            stages = stages,
            completedStageIds = completedList,
            warnUnsavedChanges = warnUnsaved,
            onDismiss = { showStagesToggleSheet = false },
            onSave = { updatedList ->
                viewModel.updateProjectStages(
                    projectId = project!!.id,
                    stageId = updatedList.lastOrNull(),
                    completedStageIds = updatedList
                )
                showStagesToggleSheet = false
            }
        )
    }
}

// =========================================================================
// Stateless Full-Screen Content
// =========================================================================

@Composable
fun ProjectDetailContent(
    project: ProjectEntity,
    customer: CustomerEntity?,
    stages: List<ProjectStageEntity>,
    specsCount: Int,
    invoiceItemsCount: Int,
    itemsSum: Long,
    invoiceExpensesSum: Long,
    workshopExpensesSum: Long,
    discount: Long,
    currencyUnit: String,
    onBack: () -> Unit,
    onEditProject: () -> Unit,
    onOpenStagesSheet: () -> Unit,
    onPrintContract: () -> Unit,
    onNavigateToContractEditor: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onNavigateToSpecs: () -> Unit,
    onNavigateToInvoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completedStageList = remember(project.completedStageIds) {
        project.completedStageIds.split(",").filter { it.isNotBlank() }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = "پرونده پروژه",
                subtitle = null, // Header subtitle removed per instruction 3.1
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = onEditProject,
                        modifier = Modifier.testTag("edit_project_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "ویرایش پروژه")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Card 1: Project Overview & Progress
                ProjectOverviewCard(
                    project = project,
                    customer = customer,
                    stages = stages
                )
            }

            item {
                // Card 2: Two-column Project Status & Production Stages Pipeline
                ProjectStagePipelineCard(
                    projectStatus = project.status,
                    completedStageIds = completedStageList,
                    stages = stages,
                    onUpdateStatus = onUpdateStatus,
                    onOpenStagesSheet = onOpenStagesSheet
                )
            }

            item {
                ProjectFinancialSummaryCard(
                    specsCount = specsCount,
                    servicesCount = invoiceItemsCount,
                    itemsSum = itemsSum,
                    invoiceExpensesSum = invoiceExpensesSum,
                    workshopExpensesSum = workshopExpensesSum,
                    discount = discount,
                    currencyUnit = currencyUnit
                )
            }

            item {
                ProjectNavigationButtonsCard(
                    specsCount = specsCount,
                    invoiceItemsCount = invoiceItemsCount,
                    onNavigateToSpecs = onNavigateToSpecs,
                    onNavigateToInvoice = onNavigateToInvoice
                )
            }

            item {
                // Card 5: Separate Contract Action Card moved to bottom per instruction 3.4
                ProjectContractActionCard(
                    onPrintContract = onPrintContract,
                    onNavigateToContractEditor = onNavigateToContractEditor
                )
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
// =========================================================================
// Modular Cards
// =========================================================================

@Composable
fun ProjectOverviewCard(
    project: ProjectEntity,
    customer: CustomerEntity?,
    stages: List<ProjectStageEntity>,
    modifier: Modifier = Modifier
) {
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    val sortedStages = remember(stages) { stages.sortedBy { it.displayOrder } }
    val completedStageList = remember(project.completedStageIds) {
        project.completedStageIds.split(",").filter { it.isNotBlank() }
    }
    val highestCompletedOrder = remember(sortedStages, completedStageList) {
        val completed = sortedStages.filter { completedStageList.contains(it.id) }
        completed.maxOfOrNull { it.displayOrder } ?: 0
    }
    val progressFraction = if (sortedStages.isNotEmpty()) {
        (highestCompletedOrder.toFloat() / sortedStages.size.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val progressPercent = (progressFraction * 100).toInt()

    val chipDisplay = remember(project, stages) {
        ProjectStatusHelper.getProjectChip(project, stages)
    }

    val defaultPrimaryColor = MaterialTheme.colorScheme.primary
    val chipColor = remember(chipDisplay.colorHex, defaultPrimaryColor) {
        try {
            if (chipDisplay.colorHex != null) {
                Color(AndroidColor.parseColor(chipDisplay.colorHex))
            } else {
                defaultPrimaryColor
            }
        } catch (_: Exception) {
            defaultPrimaryColor
        }
    }

    val orderCode = remember(project.id) {
        "PRJ-${project.id.takeLast(4).uppercase()}"
    }

    AppCard(
        modifier = modifier,
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Icon + Title + Order Code on right, Status Chip on left
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Right side: Icon + Title + Code
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PrecisionManufacturing,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = project.title,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "کد سفارش: $orderCode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Left side: Status badge chip
                Surface(
                    color = chipColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, chipColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = chipDisplay.label,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Bold,
                        color = chipColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metadata Row: Customer on right, Start Date on left
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "کارفرما: ${customer?.name ?: "نامشخص"}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تاریخ شروع: ${project.startDate}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Thick Progress Bar
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-Progress Row: View Description toggle on right, Total Progress on left
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isDescriptionExpanded = !isDescriptionExpanded }
                ) {
                    Text(
                        text = if (isDescriptionExpanded) "بستن توضیحات" else "مشاهده توضیحات",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isDescriptionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "پیشرفت کل: $progressPercent٪",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expandable Description Box
            AnimatedVisibility(visible = isDescriptionExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (project.description.isNotBlank()) project.description else "توضیحاتی برای این پروژه ثبت نشده است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectStagePipelineCard(
    projectStatus: String,
    completedStageIds: List<String>,
    stages: List<ProjectStageEntity>,
    onUpdateStatus: (String) -> Unit,
    onOpenStagesSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    var statusMenuExpanded by remember { mutableStateOf(false) }
    val statusOptions = listOf(
        "ACTIVE" to "جاری",
        "COMPLETED" to "تکمیل‌شده",
        "CANCELLED" to "لغوشده",
        "PENDING" to "معلق"
    )

    val currentStatusLabel = statusOptions.firstOrNull { it.first == projectStatus }?.second
        ?: if (projectStatus == "در حال ساخت" || projectStatus == "جاری") "جاری"
        else if (projectStatus == "تکمیل شده") "تکمیل‌شده"
        else projectStatus

    val sortedStages = remember(stages) { stages.sortedBy { it.displayOrder } }

    // Find the currently active (first uncompleted) stage
    val currentActiveStage = remember(sortedStages, completedStageIds) {
        sortedStages.firstOrNull { !completedStageIds.contains(it.id) }
    }

    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Right Column: Vertical Production Step Progress Bar
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .padding(end = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "مراحل اجرایی کارگاه",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (sortedStages.isEmpty()) {
                    Text(
                        text = "مرحله‌ای تعریف نشده است.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    VerticalStepProgressBar(
                        stages = sortedStages,
                        completedStageIds = completedStageIds,
                        activeStageId = currentActiveStage?.id
                    )
                }
            }

            // Vertical separator divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(210.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            )

            // Left Column: Project Status Selector & Edit Stages Button
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .padding(start = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "وضعیت پروژه",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Status Dropdown Button matching reference design (RTL: status on right, arrow on left)
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { statusMenuExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Status dot + label on the RIGHT in RTL
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val dotColor = when (projectStatus) {
                                    "ACTIVE", "جاری", "در حال ساخت" -> Color(0xFF16A34A)
                                    "COMPLETED", "تکمیل شده", "تکمیل‌شده" -> Color(0xFF0284C7)
                                    "PENDING", "معلق" -> Color(0xFFD97706)
                                    else -> Color(0xFFDC2626)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = currentStatusLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Arrow on the LEFT in RTL
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = statusMenuExpanded,
                        onDismissRequest = { statusMenuExpanded = false }
                    ) {
                        statusOptions.forEach { (statusCode, statusLabel) ->
                            DropdownMenuItem(
                                text = { Text(statusLabel, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    statusMenuExpanded = false
                                    onUpdateStatus(statusCode)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // "Edit Stages" Button matching reference design
                Button(
                    onClick = onOpenStagesSheet,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ویرایش مراحل",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
fun VerticalStepProgressBar(
    stages: List<ProjectStageEntity>,
    completedStageIds: List<String>,
    activeStageId: String?
) {
    // Pulse animation for active stage
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.45f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(850, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(850, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        stages.forEachIndexed { index, stage ->
            val isCompleted = completedStageIds.contains(stage.id)
            val isActive = !isCompleted && stage.id == activeStageId
            val isFuture = !isCompleted && !isActive
            val isLast = index == stages.size - 1

            // Display title: per instruction, use stage.description exactly
            val displayTitle = if (stage.description.isNotBlank()) stage.description else stage.name

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Timeline node + vertical connecting line (Right side in RTL)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isCompleted -> {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF16A34A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            isActive -> {
                                // Pulsing red ring
                                Box(
                                    modifier = Modifier
                                        .size(18.dp * pulseScale)
                                        .clip(CircleShape)
                                        .background(Color(0xFFDC2626).copy(alpha = pulseAlpha))
                                )
                                // Solid center ring
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFDC2626)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                            else -> {
                                // Future stage node (grey with cross or ring)
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (!isLast) {
                        val lineColor = if (isCompleted) Color(0xFF16A34A) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        Box(
                            modifier = Modifier
                                .width(2.5.dp)
                                .height(20.dp)
                                .background(lineColor)
                        )
                    }
                }

                // Step title (to the left of the node in RTL)
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isActive || isCompleted) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isActive -> Color(0xFFDC2626) // Red color for active stage per user instruction
                        isCompleted -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp, top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun ProjectContractActionCard(
    onPrintContract: () -> Unit,
    onNavigateToContractEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier,
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "قرارداد رسمی تابلوسازی",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "تنظیم شرایط حقوقی، تعهدات ساخت و چاپ مستقیم فایل PDF قرارداد رسمی تابلوسازی",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPrintContract,
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
                    onClick = onNavigateToContractEditor,
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

@Composable
fun ProjectFinancialSummaryCard(
    specsCount: Int,
    servicesCount: Int,
    itemsSum: Long,
    invoiceExpensesSum: Long,
    workshopExpensesSum: Long,
    discount: Long,
    currencyUnit: String,
    modifier: Modifier = Modifier
) {
    val finalNet = itemsSum - discount + invoiceExpensesSum

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
        modifier = modifier.fillMaxWidth()
    ) {
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
                        text = "$specsCount تابلو • $servicesCount خدمت",
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
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "هزینه‌های فاکتور شده:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFDC2626)
                    )
                    Text(
                        text = "+ ${FormatUtils.formatCurrency(invoiceExpensesSum, currencyUnit)}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }
            }

            if (workshopExpensesSum > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("هزینه‌های کارگاهی و سود/زیان:", style = MaterialTheme.typography.bodyMedium)
                    Text(FormatUtils.formatCurrency(workshopExpensesSum, currencyUnit), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

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

@Composable
fun ProjectNavigationButtonsCard(
    specsCount: Int,
    invoiceItemsCount: Int,
    onNavigateToSpecs: () -> Unit,
    onNavigateToInvoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "بخش‌های اختصاصی پرونده",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        // Button 1: Specs Screen
        Card(
            onClick = onNavigateToSpecs,
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
                            text = "$specsCount",
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
            onClick = onNavigateToInvoice,
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
                            text = "$invoiceItemsCount",
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

// =========================================================================
// Previews (Full Screen + Individual Cards)
// =========================================================================

@Preview(
    name = "1. Full Phone Project Detail",
    showBackground = true,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun ProjectDetailScreenPreview() {
    val sampleProject = ProjectEntity(
        id = "sample_p1",
        customerId = "c1",
        title = "تابلو چلنیوم با کامپوزیت طلا و جواهر رحمانی",
        startDate = "1405/10/08",
        status = "ACTIVE",
        completedStageIds = "s1,s2",
        description = "نصب در ارتفاع ۴ متری، زیرسازی کامپوزیت طوسی مات"
    )
    val sampleCustomer = CustomerEntity(
        id = "c1",
        name = "صنایع نفت خلیج",
        phone = "09121112233",
        nationalCode = "0014556789"
    )
    val sampleStages = listOf(
        ProjectStageEntity(id = "s1", name = "مذاکره", description = "مذاکره و تاییدیه", displayOrder = 1, colorHex = "#10B981"),
        ProjectStageEntity(id = "s2", name = "طراحی", description = "طراحی و شبیه‌سازی", displayOrder = 2, colorHex = "#0284C7"),
        ProjectStageEntity(id = "s3", name = "ساخت", description = "ساخت و مونتاژ کارگاه", displayOrder = 3, colorHex = "#E11D48"),
        ProjectStageEntity(id = "s4", name = "نصب", description = "نصب و راه‌اندازی", displayOrder = 4, colorHex = "#7C3AED"),
        ProjectStageEntity(id = "s5", name = "تسویه", description = "تسویه حساب و تحویل", displayOrder = 5, colorHex = "#0D9488")
    )

    MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(color = MaterialTheme.colorScheme.background) {
                ProjectDetailContent(
                    project = sampleProject,
                    customer = sampleCustomer,
                    stages = sampleStages,
                    specsCount = 2,
                    invoiceItemsCount = 3,
                    itemsSum = 68000000L,
                    invoiceExpensesSum = 4500000L,
                    workshopExpensesSum = 12000000L,
                    discount = 3000000L,
                    currencyUnit = "TOMAN",
                    onBack = {},
                    onEditProject = {},
                    onOpenStagesSheet = {},
                    onPrintContract = {},
                    onNavigateToContractEditor = {},
                    onUpdateStatus = {},
                    onNavigateToSpecs = {},
                    onNavigateToInvoice = {}
                )
            }
        }
    }
}

@Preview(name = "2. Project Overview Card", showBackground = true)
@Composable
fun ProjectOverviewCardPreview() {
    val sampleStages = listOf(
        ProjectStageEntity(id = "s1", name = "مذاکره", description = "مذاکره و تاییدیه", displayOrder = 1, colorHex = "#10B981"),
        ProjectStageEntity(id = "s2", name = "طراحی", description = "طراحی و شبیه‌سازی", displayOrder = 2, colorHex = "#0284C7"),
        ProjectStageEntity(id = "s3", name = "ساخت", description = "ساخت و مونتاژ کارگاه", displayOrder = 3, colorHex = "#E11D48")
    )
    MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.padding(16.dp)) {
                ProjectOverviewCard(
                    project = ProjectEntity(
                        id = "sample_p1",
                        customerId = "c1",
                        title = "تابلو چلنیوم با کامپوزیت",
                        startDate = "1405/10/08",
                        status = "ACTIVE",
                        completedStageIds = "s1,s2",
                        description = "نصب در ارتفاع ۴ متری، زیرسازی کامپوزیت طوسی مات"
                    ),
                    customer = CustomerEntity(
                        id = "c1",
                        name = "صنایع نفت خلیج",
                        phone = "09121112233"
                    ),
                    stages = sampleStages
                )
            }
        }
    }
}

@Preview(name = "3. Stage Pipeline Card", showBackground = true)
@Composable
fun ProjectStagePipelineCardPreview() {
    val sampleStages = listOf(
        ProjectStageEntity(id = "s1", name = "مذاکره", description = "مذاکره و تاییدیه", displayOrder = 1, colorHex = "#10B981"),
        ProjectStageEntity(id = "s2", name = "طراحی", description = "طراحی و شبیه‌سازی", displayOrder = 2, colorHex = "#0284C7"),
        ProjectStageEntity(id = "s3", name = "ساخت", description = "ساخت و مونتاژ کارگاه", displayOrder = 3, colorHex = "#E11D48"),
        ProjectStageEntity(id = "s4", name = "نصب", description = "نصب و راه‌اندازی", displayOrder = 4, colorHex = "#7C3AED"),
        ProjectStageEntity(id = "s5", name = "تسویه", description = "تسویه حساب و تحویل", displayOrder = 5, colorHex = "#0D9488")
    )

    MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.padding(16.dp)) {
                ProjectStagePipelineCard(
                    projectStatus = "ACTIVE",
                    completedStageIds = listOf("s1", "s2"),
                    stages = sampleStages,
                    onUpdateStatus = {},
                    onOpenStagesSheet = {}
                )
            }
        }
    }
}

@Preview(name = "4. Contract Action Card", showBackground = true)
@Composable
fun ProjectContractActionCardPreview() {
    MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.padding(16.dp)) {
                ProjectContractActionCard(
                    onPrintContract = {},
                    onNavigateToContractEditor = {}
                )
            }
        }
    }
}

@Preview(name = "5. Financial Summary Card", showBackground = true)
@Composable
fun ProjectFinancialSummaryCardPreview() {
    MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.padding(16.dp)) {
                ProjectFinancialSummaryCard(
                    specsCount = 2,
                    servicesCount = 3,
                    itemsSum = 68000000L,
                    invoiceExpensesSum = 4500000L,
                    workshopExpensesSum = 12000000L,
                    discount = 3000000L,
                    currencyUnit = "TOMAN"
                )
            }
        }
    }
}

@Preview(name = "6. Navigation Buttons Card", showBackground = true)
@Composable
fun ProjectNavigationButtonsCardPreview() {
    MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.padding(16.dp)) {
                ProjectNavigationButtonsCard(
                    specsCount = 2,
                    invoiceItemsCount = 3,
                    onNavigateToSpecs = {},
                    onNavigateToInvoice = {}
                )
            }
        }
    }
}

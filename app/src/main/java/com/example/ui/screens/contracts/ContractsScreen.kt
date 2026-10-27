package com.example.ui.screens.contracts

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entities.ContractEntity
import com.example.ui.components.common.*
import com.example.ui.viewmodels.SignageViewModel
import com.example.util.FormatUtils
import com.example.util.PdfGenerator
import java.io.File

@Composable
fun ContractsScreen(
    viewModel: SignageViewModel,
    onNavigateToContractEditor: (String) -> Unit
) {
    val context = LocalContext.current
    val contracts by viewModel.contracts.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val currencyUnit by viewModel.currencyUnit.collectAsState()
    val contractorInfo by viewModel.contractorInfo.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var contractToDelete by remember { mutableStateOf<ContractEntity?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val filteredContracts = remember(contracts, searchQuery, projects, customers) {
        if (searchQuery.isBlank()) contracts
        else {
            val q = searchQuery.trim()
            contracts.filter { con ->
                val proj = projects.firstOrNull { it.id == con.projectId }
                val cust = customers.firstOrNull { it.id == con.customerId }
                (proj?.title?.contains(q, ignoreCase = true) == true) ||
                        (cust?.name?.contains(q, ignoreCase = true) == true) ||
                        con.contractDate.contains(q)
            }
        }
    }

    // List generated documents
    val generatedFiles = remember(selectedTab) {
        val dir = File(context.filesDir, "documents")
        if (dir.exists()) {
            dir.listFiles { f -> f.extension.equals("pdf", ignoreCase = true) }?.sortedByDescending { it.lastModified() } ?: emptyList()
        } else emptyList()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = "اسناد و قراردادها",
                subtitle = "${contracts.size} قرارداد ثبت شده"
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("قراردادهای جاری (${contracts.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("فایل‌های PDF صادر شده (${generatedFiles.size})", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedTab == 0) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PersianTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = "جستجوی قرارداد بر اساس پروژه یا کارفرما...",
                        leadingIcon = Icons.Default.Search,
                        testTag = "contracts_search_input"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (filteredContracts.isEmpty()) {
                        EmptyStateView(
                            title = "قراردادی یافت نشد",
                            message = "از صفحه پروژه‌ها، با زدن دکمه «چاپ قرارداد» یا «تنظیم بندها» قرارداد صادر کنید.",
                            icon = Icons.Default.Description
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredContracts, key = { it.id }) { con ->
                                val project = projects.firstOrNull { it.id == con.projectId }
                                val customer = customers.firstOrNull { it.id == con.customerId }

                                AppCard {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "پروژه: ${project?.title ?: "پروژه بدون عنوان"}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "کارفرما: ${customer?.name ?: "نامشخص"} • تاریخ: ${con.contractDate}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = FormatUtils.formatCurrency(con.totalAmount - con.discount, currencyUnit),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceAround
                                            ) {
                                                Text("پیش‌پرداخت: ${FormatUtils.formatCurrency(con.prepayment, currencyUnit)}", style = MaterialTheme.typography.labelSmall)
                                                Text("مانده: ${FormatUtils.formatCurrency(con.balanceAmount, currencyUnit)}", style = MaterialTheme.typography.labelSmall)
                                                Text("مدت: ${con.durationDays} روز", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(onClick = {
                                                if (project != null && customer != null) {
                                                    try {
                                                        val pdf = PdfGenerator.generateContractPdf(
                                                            context = context,
                                                            contractor = contractorInfo,
                                                            customer = customer,
                                                            project = project,
                                                            specs = emptyList(),
                                                            durationDays = con.durationDays,
                                                            totalAmount = con.totalAmount,
                                                            prepayment = con.prepayment,
                                                            balanceAmount = con.balanceAmount,
                                                            contractDate = con.contractDate,
                                                            arbitratorName = con.arbitratorName
                                                        )
                                                        PdfGenerator.sharePdf(context, pdf, "قرارداد تابلوسازی - ${project.title}")
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "خطا در چاپ PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }) {
                                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("چاپ قرارداد")
                                            }

                                            IconButton(onClick = { onNavigateToContractEditor(con.projectId) }) {
                                                Icon(Icons.Default.Edit, contentDescription = "ویرایش بندها", tint = MaterialTheme.colorScheme.primary)
                                            }

                                            IconButton(onClick = { contractToDelete = con }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Generated PDF Documents List
                if (generatedFiles.isEmpty()) {
                    EmptyStateView(
                        title = "فایل PDF صادر نشده است",
                        message = "با صدور فاکتور یا قرارداد، فایل‌های PDF ذخیره‌شده در اینجا جهت مشاهده و اشتراک‌گذاری سریع نمایش می‌یابند.",
                        icon = Icons.Default.FolderOpen
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(generatedFiles) { file ->
                            AppCard {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Column {
                                            Text(file.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            Text("${file.length() / 1024} کیلوبایت", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Row {
                                        IconButton(onClick = {
                                            try {
                                                PdfGenerator.viewPdf(context, file)
                                            } catch (e: Exception) {
                                                PdfGenerator.sharePdf(context, file)
                                            }
                                        }) {
                                            Icon(Icons.Default.Visibility, contentDescription = "مشاهده سند")
                                        }
                                        IconButton(onClick = {
                                            PdfGenerator.sharePdf(context, file)
                                        }) {
                                            Icon(Icons.Default.Share, contentDescription = "اشتراک‌گذاری", tint = MaterialTheme.colorScheme.primary)
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

    DeleteConfirmationDialog(
        show = contractToDelete != null,
        title = "حذف قرارداد",
        message = "آیا از حذف این قرارداد رسمی اطمینان دارید؟",
        onConfirm = {
            contractToDelete?.let { con ->
                viewModel.deleteContract(con) {
                    contractToDelete = null
                }
            }
        },
        onDismiss = { contractToDelete = null }
    )
}

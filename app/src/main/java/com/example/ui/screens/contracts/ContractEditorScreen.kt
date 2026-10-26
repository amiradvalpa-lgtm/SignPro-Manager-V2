package com.example.ui.screens.contracts

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.data.local.entities.CustomerEntity
import com.example.ui.components.common.*
import com.example.ui.viewmodels.SignageViewModel
import com.example.util.FormatUtils
import com.example.util.JalaliDate
import com.example.util.PdfGenerator
import java.util.UUID

@Composable
fun ContractEditorScreen(
    projectId: String,
    viewModel: SignageViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val project by viewModel.repository.getProjectByIdFlow(projectId).collectAsState(initial = null)
    val existingContract by viewModel.getContractForProject(projectId).collectAsState(initial = null)
    val specs by viewModel.getSpecsForProject(projectId).collectAsState(initial = emptyList())
    val invoice by viewModel.getInvoiceForProject(projectId).collectAsState(initial = null)
    val contractorInfo by viewModel.contractorInfo.collectAsState()
    val currencyUnit by viewModel.currencyUnit.collectAsState()

    var customer by remember { mutableStateOf<CustomerEntity?>(null) }
    LaunchedEffect(project?.customerId) {
        project?.customerId?.let { cid ->
            customer = viewModel.repository.getCustomerById(cid)
        }
    }

    val invoiceItems = remember(invoice) {
        invoice?.let { inv -> viewModel.getInvoiceItems(inv.id) }
    }?.collectAsState(initial = emptyList())?.value ?: emptyList()

    // Financial calculations
    val calculatedTotalFromInvoice = remember(invoiceItems, invoice) {
        val sum = invoiceItems.sumOf { it.calculatedTotal }
        val discount = invoice?.discount ?: 0L
        (sum - discount).coerceAtLeast(0L)
    }

    var contractDate by remember { mutableStateOf(JalaliDate.today().formatShort()) }
    var durationDaysStr by remember { mutableStateOf("15") }
    var totalAmount by remember { mutableStateOf(0L) }
    var discount by remember { mutableStateOf(0L) }
    var prepayment by remember { mutableStateOf(0L) }
    var arbitratorName by remember { mutableStateOf("مرضی‌الطرفین") }
    var description by remember { mutableStateOf("") }

    var customClauses by remember { mutableStateOf(listOf<String>()) }
    var newClauseText by remember { mutableStateOf("") }
    var showAddClauseDialog by remember { mutableStateOf(false) }

    // Synchronize initial values from existing contract or invoice
    LaunchedEffect(existingContract, calculatedTotalFromInvoice) {
        if (existingContract != null) {
            val c = existingContract!!
            contractDate = c.contractDate
            durationDaysStr = c.durationDays.toString()
            totalAmount = c.totalAmount
            discount = c.discount
            prepayment = c.prepayment
            arbitratorName = c.arbitratorName
            description = c.description
            if (c.termsJson.isNotEmpty()) {
                customClauses = c.termsJson.split("###").filter { it.isNotBlank() }
            }
        } else if (calculatedTotalFromInvoice > 0) {
            totalAmount = calculatedTotalFromInvoice
            prepayment = (calculatedTotalFromInvoice * 0.5).toLong()
        }
    }

    val balanceAmount = (totalAmount - discount - prepayment).coerceAtLeast(0L)

    Scaffold(
        topBar = {
            AppTopBar(
                title = "تنظیم و صدور قرارداد تابلوسازی",
                subtitle = project?.title,
                onBack = onBack
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
                AppCard(borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "به نام خدا - قرارداد رسمی تابلوسازی",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "تنظیم شده بر اساس ماده ۱۰ قانون مدنی جمهوری اسلامی ایران",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Contractor & Customer Header
            item {
                AppCard {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("بند ۱: طرفین قرارداد", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• پیمانکار: ${contractorInfo.name} (${contractorInfo.businessName}) - تلفن: ${contractorInfo.phone}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• کارفرما: ${customer?.name ?: "نامشخص"} - تلفن: ${customer?.phone ?: ""} - کد ملی: ${customer?.nationalCode?.ifEmpty { "ثبت نشده" }}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (customer?.address?.isNotEmpty() == true) {
                            Text("نشانی کارفرما: ${customer?.address}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Project & Specs
            item {
                AppCard {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("بند ۲: موضوع و مشخصات فنی تابلو", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        if (specs.isEmpty()) {
                            Text(
                                text = "هنوز مشخصات تابلویی در این پروژه ثبت نشده است. مشخصات از فرم جزئیات پروژه قابل ثبت است.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            specs.forEach { s ->
                                Text(
                                    text = "• ${s.subTitle} (نوع: ${s.signType}) - ابعاد: طول ${s.lengthCm} cm × عرض ${s.widthCm} cm × عمق ${s.depthCm} cm",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // Financial & Payment Form
            item {
                AppCard {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("بند ۳: مبلغ قرارداد و نحوه پرداخت", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                        PersianDateField(
                            dateString = contractDate,
                            onDateSelected = { contractDate = it },
                            label = "تاریخ تنظیم قرارداد *"
                        )

                        PersianTextField(
                            value = durationDaysStr,
                            onValueChange = { durationDaysStr = it },
                            label = "مدت زمان اجرای پروژه (روز کاری) *"
                        )

                        CurrencyInputField(
                            amount = totalAmount,
                            onAmountChange = { totalAmount = it },
                            label = "مبلغ کل قرارداد",
                            currencyUnit = currencyUnit,
                            testTag = "contract_total_amount_input"
                        )

                        CurrencyInputField(
                            amount = discount,
                            onAmountChange = { discount = it },
                            label = "تخفیف توافق‌شده",
                            currencyUnit = currencyUnit,
                            testTag = "contract_discount_input"
                        )

                        CurrencyInputField(
                            amount = prepayment,
                            onAmountChange = { prepayment = it },
                            label = "مبلغ پیش‌پرداخت نقدی / کارت به کارت",
                            currencyUnit = currencyUnit,
                            testTag = "contract_prepay_input"
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("مانده قابل پرداخت پس از تحویل:", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = FormatUtils.formatCurrency(balanceAmount, currencyUnit),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "واریز به: حساب ${contractorInfo.accountNumber} نزد ${contractorInfo.bankName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        PersianTextField(
                            value = arbitratorName,
                            onValueChange = { arbitratorName = it },
                            label = "داور مرضی‌الطرفین قرارداد"
                        )

                        PersianTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = "توضیحات و توافقات خاص",
                            singleLine = false
                        )
                    }
                }
            }

            // Custom Clauses Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("بندهای الحاقی و سفارشی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showAddClauseDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("افزودن بند سفارشی")
                    }
                }
            }

            if (customClauses.isEmpty()) {
                item {
                    Text(
                        text = "بندهای استاندارد قانونی (تعهدات ۵، ۶ و ۷ شامل ضمانت یک ساله و اسقاط خیارات) به‌طور خودکار در متن PDF لحاظ می‌شوند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                itemsIndexed(customClauses) { index, clause ->
                    AppCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "بند الحاقی ${index + 1}: $clause",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                customClauses = customClauses.toMutableList().apply { removeAt(index) }
                            }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Bottom Actions: Save & Print PDF
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val duration = durationDaysStr.toIntOrNull() ?: 15
                            val entity = existingContract?.copy(
                                contractDate = contractDate,
                                durationDays = duration,
                                totalAmount = totalAmount,
                                discount = discount,
                                prepayment = prepayment,
                                balanceAmount = balanceAmount,
                                termsJson = customClauses.joinToString("###"),
                                arbitratorName = arbitratorName.trim(),
                                description = description.trim()
                            ) ?: ContractEntity(
                                id = UUID.randomUUID().toString(),
                                customerId = project?.customerId ?: "",
                                projectId = projectId,
                                contractDate = contractDate,
                                durationDays = duration,
                                totalAmount = totalAmount,
                                discount = discount,
                                prepayment = prepayment,
                                balanceAmount = balanceAmount,
                                termsJson = customClauses.joinToString("###"),
                                arbitratorName = arbitratorName.trim(),
                                description = description.trim()
                            )
                            viewModel.saveContract(entity, isNew = existingContract == null) {
                                Toast.makeText(context, "اطلاعات قرارداد با موفقیت ذخیره شد.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("save_contract_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ذخیره قرارداد", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (customer == null || project == null) {
                                Toast.makeText(context, "اطلاعات کارفرما یا پروژه کامل نیست.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            try {
                                val duration = durationDaysStr.toIntOrNull() ?: 15
                                val pdf = PdfGenerator.generateContractPdf(
                                    context = context,
                                    contractor = contractorInfo,
                                    customer = customer!!,
                                    project = project!!,
                                    specs = specs,
                                    durationDays = duration,
                                    totalAmount = totalAmount,
                                    prepayment = prepayment,
                                    balanceAmount = balanceAmount,
                                    contractDate = contractDate,
                                    arbitratorName = arbitratorName,
                                    customClauses = customClauses
                                )
                                PdfGenerator.sharePdf(context, pdf, "قرارداد تابلوسازی - ${project?.title}")
                            } catch (e: Exception) {
                                Toast.makeText(context, "خطا در تولید PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("generate_contract_pdf_btn")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("چاپ و ارسال PDF", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    if (showAddClauseDialog) {
        AlertDialog(
            onDismissRequest = { showAddClauseDialog = false },
            title = { Text("افزودن بند سفارشی به قرارداد", fontWeight = FontWeight.Bold) },
            text = {
                PersianTextField(
                    value = newClauseText,
                    onValueChange = { newClauseText = it },
                    label = "متن بند الحاقی",
                    placeholder = "مثال: هزینه جرثقیل تا سقف ۲ میلیون تومان بر عهده مجری می‌باشد.",
                    singleLine = false
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newClauseText.trim().isNotEmpty()) {
                            customClauses = customClauses + newClauseText.trim()
                            newClauseText = ""
                            showAddClauseDialog = false
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("افزودن به قرارداد")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddClauseDialog = false }, shape = RoundedCornerShape(10.dp)) {
                    Text("انصراف")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

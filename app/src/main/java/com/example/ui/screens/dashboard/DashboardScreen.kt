package com.example.ui.screens.dashboard

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.UserEntity
import com.example.data.preferences.ContractorInfo
import com.example.ui.components.common.AppHeaderDivider
import com.example.ui.navigation.Screen
import com.example.ui.theme.*
import com.example.ui.viewmodels.SignageViewModel
import com.example.util.FormatUtils
import com.example.util.JalaliDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: SignageViewModel,
    onNavigateTo: (String) -> Unit,
    onOpenCustomerDialog: () -> Unit,
    onOpenProjectDialog: () -> Unit,
    onOpenPaymentDialog: () -> Unit,
    onOpenCheckDialog: () -> Unit
) {
    val customers by viewModel.customers.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val checks by viewModel.checks.collectAsState()
    val contracts by viewModel.contracts.collectAsState()
    val currencyUnit by viewModel.currencyUnit.collectAsState()
    val contractorInfo by viewModel.contractorInfo.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    // Calculated metrics
    val activeProjectsCount = remember(projects) {
        projects.count { it.status != "تکمیل شده" && it.status != "تسویه شده" }
    }

    val totalContractsAmount = remember(contracts) {
        contracts.sumOf { it.totalAmount - it.discount }
    }

    val totalReceipts = remember(payments) {
        payments.filter { it.type == "RECEIPT" }.sumOf { it.amount }
    }

    val pendingChecks = remember(checks) {
        checks.filter { it.status == "PENDING" }
    }
    val pendingChecksAmount = remember(pendingChecks) {
        pendingChecks.sumOf { it.amount }
    }

    val todayStr = remember { JalaliDate.today().formatShort() }
    val overdueChecksCount = remember(checks, todayStr) {
        checks.count { it.status == "PENDING" && it.dueDate <= todayStr }
    }

    // Customer balance & debtor calculation
    val totalCustomerDebt = remember(contracts, payments, checks) {
        val billed = contracts.sumOf { it.totalAmount - it.discount }
        val paid = payments.filter { it.type == "RECEIPT" }.sumOf { it.amount }
        val clearedChecks = checks.filter { it.status == "CLEARED" }.sumOf { it.amount }
        (billed - paid - clearedChecks).coerceAtLeast(0L)
    }

    val debtorCustomersCount = remember(customers, contracts, payments, checks) {
        customers.count { customer ->
            val customerContracts = contracts.filter { it.customerId == customer.id }
            val customerBilled = customerContracts.sumOf { it.totalAmount - it.discount }
            val customerPayments = payments.filter { it.customerId == customer.id && it.type == "RECEIPT" }.sumOf { it.amount }
            val customerClearedChecks = checks.filter { it.customerId == customer.id && it.status == "CLEARED" }.sumOf { it.amount }
            (customerBilled - customerPayments - customerClearedChecks) > 0
        }
    }

    var showUserMenu by remember { mutableStateOf(false) }
    val todayJalali = remember { JalaliDate.today().formatPersian() }

    DashboardContent(
        todayJalali = todayJalali,
        currentUser = currentUser,
        contractorInfo = contractorInfo,
        showUserMenu = showUserMenu,
        onShowUserMenuChange = { showUserMenu = it },
        onNavigateTo = onNavigateTo,
        onOpenCustomerDialog = onOpenCustomerDialog,
        onOpenProjectDialog = onOpenProjectDialog,
        onOpenPaymentDialog = onOpenPaymentDialog,
        onOpenCheckDialog = onOpenCheckDialog,
        activeProjectsCount = activeProjectsCount,
        totalProjectsCount = projects.size,
        debtorCustomersCount = debtorCustomersCount,
        totalCustomersCount = customers.size,
        totalContractsAmount = totalContractsAmount,
        contractsCount = contracts.size,
        totalReceipts = totalReceipts,
        totalCustomerDebt = totalCustomerDebt,
        pendingChecksAmount = pendingChecksAmount,
        pendingChecksCount = pendingChecks.size,
        overdueChecksCount = overdueChecksCount,
        currencyUnit = FormatUtils.getCurrencyLabel(currencyUnit)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    todayJalali: String,
    currentUser: UserEntity?,
    contractorInfo: ContractorInfo,
    showUserMenu: Boolean,
    onShowUserMenuChange: (Boolean) -> Unit,
    onNavigateTo: (String) -> Unit,
    onOpenCustomerDialog: () -> Unit,
    onOpenProjectDialog: () -> Unit,
    onOpenPaymentDialog: () -> Unit,
    onOpenCheckDialog: () -> Unit,
    activeProjectsCount: Int,
    totalProjectsCount: Int,
    debtorCustomersCount: Int,
    totalCustomersCount: Int,
    totalContractsAmount: Long,
    contractsCount: Int,
    totalReceipts: Long,
    totalCustomerDebt: Long,
    pendingChecksAmount: Long,
    pendingChecksCount: Int,
    overdueChecksCount: Int,
    currencyUnit: String,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "امروز",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = todayJalali,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { /* اعلان‌ها */ }) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "اعلان‌ها و پیام‌ها",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box {
                                IconButton(
                                    onClick = { onShowUserMenuChange(true) },
                                    modifier = Modifier.testTag("dashboard_user_icon_btn")
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "پروفایل کاربر",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }

                                DropdownMenu(
                                    expanded = showUserMenu,
                                    onDismissRequest = { onShowUserMenuChange(false) }
                                ) {
                                    val userName = currentUser?.fullName?.ifBlank { contractorInfo.name } ?: contractorInfo.name
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = userName,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = "مدیر کارگاه تابلوسازی",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {},
                                        enabled = false
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("ویرایش مشخصات کاربر") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                        },
                                        onClick = {
                                            onShowUserMenuChange(false)
                                            onNavigateTo(Screen.Settings.route)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("تنظیمات برنامه") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                                        },
                                        onClick = {
                                            onShowUserMenuChange(false)
                                            onNavigateTo(Screen.Settings.route)
                                        }
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    AppHeaderDivider()
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = 12.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: دسترسی سریع
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "دسترسی سریع",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4 Quick action buttons across screen width in RTL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. مشتری جدید (Rightmost in RTL)
                        QuickActionItem(
                            modifier = Modifier.weight(1f),
                            title = "مشتری جدید",
                            icon = Icons.Default.PersonAdd,
                            accentColor = Color(0xFF2563EB),
                            iconColor = Color(0xFF2563EB),
                            iconBoxColor = Color(0xFFDBEAFE),
                            iconBoxDarkColor = Color(0xFF1E3A8A).copy(alpha = 0.45f),
                            cardBgColor = Color(0xFFF0F7FF),
                            cardBgDarkColor = MaterialTheme.colorScheme.surface,
                            borderColor = Color(0xFFBFDBFE),
                            borderDarkColor = Color(0xFF3B82F6).copy(alpha = 0.35f),
                            textColor = Color(0xFF1E3A8A),
                            textDarkColor = Color(0xFF93C5FD),
                            onClick = onOpenCustomerDialog
                        )

                        // 2. پروژه جدید
                        QuickActionItem(
                            modifier = Modifier.weight(1f),
                            title = "پروژه جدید",
                            icon = Icons.Default.Apartment,
                            accentColor = Color(0xFFD97706),
                            iconColor = Color(0xFFD97706),
                            iconBoxColor = Color(0xFFFEF3C7),
                            iconBoxDarkColor = Color(0xFF78350F).copy(alpha = 0.45f),
                            cardBgColor = Color(0xFFFFFBEB),
                            cardBgDarkColor = MaterialTheme.colorScheme.surface,
                            borderColor = Color(0xFFFED7AA),
                            borderDarkColor = Color(0xFFF59E0B).copy(alpha = 0.35f),
                            textColor = Color(0xFF92400E),
                            textDarkColor = Color(0xFFFDE68A),
                            onClick = onOpenProjectDialog
                        )

                        // 3. ثبت دریافتی
                        QuickActionItem(
                            modifier = Modifier.weight(1f),
                            title = "ثبت دریافتی",
                            icon = Icons.Default.Payments,
                            accentColor = Color(0xFF059669),
                            iconColor = Color(0xFF059669),
                            iconBoxColor = Color(0xFFD1FAE5),
                            iconBoxDarkColor = Color(0xFF064E3B).copy(alpha = 0.45f),
                            cardBgColor = Color(0xFFF0FDF4),
                            cardBgDarkColor = MaterialTheme.colorScheme.surface,
                            borderColor = Color(0xFFA7F3D0),
                            borderDarkColor = Color(0xFF10B981).copy(alpha = 0.35f),
                            textColor = Color(0xFF065F46),
                            textDarkColor = Color(0xFFA7F3D0),
                            onClick = onOpenPaymentDialog
                        )

                        // 4. ثبت چک (Leftmost in RTL)
                        QuickActionItem(
                            modifier = Modifier.weight(1f),
                            title = "ثبت چک",
                            icon = Icons.Default.ReceiptLong,
                            accentColor = Color(0xFF7C3AED),
                            iconColor = Color(0xFF7C3AED),
                            iconBoxColor = Color(0xFFF3E8FF),
                            iconBoxDarkColor = Color(0xFF4C1D95).copy(alpha = 0.45f),
                            cardBgColor = Color(0xFFFAF5FF),
                            cardBgDarkColor = MaterialTheme.colorScheme.surface,
                            borderColor = Color(0xFFE9D5FF),
                            borderDarkColor = Color(0xFF8B5CF6).copy(alpha = 0.35f),
                            textColor = Color(0xFF5B21B6),
                            textDarkColor = Color(0xFFDDD6FE),
                            onClick = onOpenCheckDialog
                        )
                    }
                }

                // Section 2: خلاصه وضعیت کارگاه و مالی
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "خلاصه وضعیت کارگاه و مالی",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 1: پروژه‌های فعال (راست) | مشتریان بدهکار (چپ)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DashboardSummaryCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stat_card_active_projects"),
                            title = "پروژه‌های فعال",
                            icon = Icons.Default.TrendingUp,
                            iconColor = Color(0xFFF59E0B),
                            value = "${FormatUtils.toPersianDigits(activeProjectsCount.toString())} پروژه",
                            valueColor = MaterialTheme.colorScheme.onSurface,
                            subtitle = "از مجموع ${FormatUtils.toPersianDigits(totalProjectsCount.toString())} سفارش",
                            indicatorColor = Color(0xFFF59E0B),
                            onClick = { onNavigateTo(Screen.Projects.route) }
                        )

                        DashboardSummaryCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stat_card_debtor_customers"),
                            title = "مشتریان بدهکار",
                            icon = Icons.Default.PersonRemove,
                            iconColor = Color(0xFFF43F5E),
                            value = "${FormatUtils.toPersianDigits(debtorCustomersCount.toString())} نفر",
                            valueColor = Color(0xFFEF4444),
                            subtitle = "از مجموع ${FormatUtils.toPersianDigits(totalCustomersCount.toString())} مشتری",
                            indicatorColor = Color(0xFFEF4444),
                            onClick = { onNavigateTo(Screen.Customers.route) }
                        )
                    }
                }

                // Row 2: مبالغ قراردادها (راست) | دریافتی نقدی (چپ)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DashboardSummaryCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stat_card_contracts"),
                            title = "مبالغ قراردادها",
                            icon = Icons.Default.Description,
                            iconColor = Color(0xFF6366F1),
                            value = FormatUtils.toPersianDigits(FormatUtils.formatNumber(totalContractsAmount)),
                            valueColor = Color(0xFF6366F1),
                            unit = currencyUnit,
                            subtitle = "${FormatUtils.toPersianDigits(contractsCount.toString())} قرارداد رسمی",
                            indicatorColor = Color(0xFF6366F1),
                            onClick = { onNavigateTo(Screen.Contracts.route) }
                        )

                        DashboardSummaryCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stat_card_receipts"),
                            title = "دریافتی نقدی",
                            icon = Icons.Default.AccountBalanceWallet,
                            iconColor = Color(0xFF10B981),
                            value = FormatUtils.toPersianDigits(FormatUtils.formatNumber(totalReceipts)),
                            valueColor = Color(0xFF059669),
                            unit = currencyUnit,
                            subtitle = "تسویه‌شده از کارفرما",
                            indicatorColor = Color(0xFF10B981),
                            onClick = { onNavigateTo(Screen.Finance.route) }
                        )
                    }
                }

                // Row 3: مانده طلب (بدهی) (راست) | چک‌های در جریان (چپ)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DashboardSummaryCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stat_card_debt"),
                            title = "مانده طلب (بدهی)",
                            icon = Icons.Default.TrendingUp,
                            iconColor = Color(0xFFEF4444),
                            value = FormatUtils.toPersianDigits(FormatUtils.formatNumber(totalCustomerDebt)),
                            valueColor = Color(0xFFEF4444),
                            unit = currencyUnit,
                            subtitle = "بدهی تسویه‌نشده",
                            indicatorColor = Color(0xFFEF4444),
                            onClick = { onNavigateTo(Screen.Finance.route) }
                        )

                        val checksSubtitle = if (overdueChecksCount > 0) {
                            "${FormatUtils.toPersianDigits(overdueChecksCount.toString())} چک سررسیدشده!"
                        } else {
                            "${FormatUtils.toPersianDigits(pendingChecksCount.toString())} برگ چک وصول‌نشده"
                        }

                        DashboardSummaryCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stat_card_checks"),
                            title = "چک‌های در جریان",
                            icon = Icons.Default.CreditCard,
                            iconColor = Color(0xFF06B6D4),
                            value = FormatUtils.toPersianDigits(FormatUtils.formatNumber(pendingChecksAmount)),
                            valueColor = Color(0xFF0284C7),
                            unit = currencyUnit,
                            subtitle = checksSubtitle,
                            indicatorColor = Color(0xFF06B6D4),
                            onClick = { onNavigateTo(Screen.Finance.route) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * دکمه دسترسی سریع با گوشه‌های گرد، بردر اختصاصی و کانتینر آیکون مطابق طراحی تصویر
 */
@Composable
private fun QuickActionItem(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    accentColor: Color,
    iconColor: Color,
    iconBoxColor: Color,
    iconBoxDarkColor: Color,
    cardBgColor: Color,
    cardBgDarkColor: Color,
    borderColor: Color,
    borderDarkColor: Color,
    textColor: Color,
    textDarkColor: Color,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) cardBgDarkColor else cardBgColor
    val border = if (isDark) borderDarkColor else borderColor
    val iconBox = if (isDark) iconBoxDarkColor else iconBoxColor
    val txt = if (isDark) textDarkColor else textColor

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = bg,
        border = BorderStroke(1.2.dp, border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconBox, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                fontWeight = FontWeight.Bold,
                color = txt,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

/**
 * کارت آماری دو ستونه خلاصه وضعیت با خط رنگی پایین و استایل هماهنگ با تم برنامه
 */
@Composable
private fun DashboardSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    iconColor: Color,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    unit: String? = null,
    subtitle: String,
    indicatorColor: Color,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f) else Color(0xFFE2E8F0)
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val titleTextColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 14.dp, end = 14.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row: عنوان کارت در سمت راست و آیکون نشانگر در سمت چپ (در چیدمان RTL)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                    fontWeight = FontWeight.Bold,
                    color = titleTextColor,
                    maxLines = 1
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Value + Unit
            if (unit != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = valueColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = valueColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(10.dp))

            // خط نشانگر افقی گرد در پایین کارت
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .height(4.dp)
                    .background(
                        color = indicatorColor,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

// -------------------------------------------------------------------------
// پیش‌نمایش کامپوز (Preview) جهت نمایش دقیق و پایدار در Android Studio
// -------------------------------------------------------------------------

@androidx.compose.ui.tooling.preview.Preview(
    name = "Dashboard - Light Theme",
    showBackground = true,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun DashboardScreenPreviewLight() {
    MyApplicationTheme(themeMode = "LIGHT") {
        Surface(color = MaterialTheme.colorScheme.background) {
            DashboardContent(
                todayJalali = "دوشنبه ۶ آبان ۱۴۰۵",
                currentUser = UserEntity(id = "user_1", fullName = "امیرحسین حسنخانی", username = "admin"),
                contractorInfo = ContractorInfo(name = "کارگاه تابلوسازی آلپا"),
                showUserMenu = false,
                onShowUserMenuChange = {},
                onNavigateTo = {},
                onOpenCustomerDialog = {},
                onOpenProjectDialog = {},
                onOpenPaymentDialog = {},
                onOpenCheckDialog = {},
                activeProjectsCount = 3,
                totalProjectsCount = 4,
                debtorCustomersCount = 5,
                totalCustomersCount = 24,
                totalContractsAmount = 151850000L,
                contractsCount = 2,
                totalReceipts = 94516765L,
                totalCustomerDebt = 47333235L,
                pendingChecksAmount = 78850000L,
                pendingChecksCount = 2,
                overdueChecksCount = 0,
                currencyUnit = "تومان"
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Dashboard - Dark Theme",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun DashboardScreenPreviewDark() {
    MyApplicationTheme(themeMode = "DARK") {
        Surface(color = MaterialTheme.colorScheme.background) {
            DashboardContent(
                todayJalali = "دوشنبه ۶ آبان ۱۴۰۵",
                currentUser = UserEntity(id = "user_1", fullName = "امیرحسین حسنخانی", username = "admin"),
                contractorInfo = ContractorInfo(name = "کارگاه تابلوسازی آلپا"),
                showUserMenu = false,
                onShowUserMenuChange = {},
                onNavigateTo = {},
                onOpenCustomerDialog = {},
                onOpenProjectDialog = {},
                onOpenPaymentDialog = {},
                onOpenCheckDialog = {},
                activeProjectsCount = 3,
                totalProjectsCount = 4,
                debtorCustomersCount = 5,
                totalCustomersCount = 24,
                totalContractsAmount = 151850000L,
                contractsCount = 2,
                totalReceipts = 94516765L,
                totalCustomerDebt = 47333235L,
                pendingChecksAmount = 78850000L,
                pendingChecksCount = 2,
                overdueChecksCount = 0,
                currencyUnit = "تومان"
            )
        }
    }
}

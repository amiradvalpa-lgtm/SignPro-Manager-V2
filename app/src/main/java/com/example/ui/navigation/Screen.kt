package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null
) {
    object Dashboard : Screen("dashboard", "داشبورد", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    object Customers : Screen("customers", "مشتریان", Icons.Filled.People, Icons.Outlined.People)
    object Projects : Screen("projects", "پروژه‌ها", Icons.Filled.Work, Icons.Outlined.WorkOutline)
    object Finance : Screen("finance", "امور مالی", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet)
    object Contracts : Screen("contracts", "قراردادها", Icons.Filled.Description, Icons.Outlined.Description)
    object Settings : Screen("settings", "تنظیمات", Icons.Filled.Settings, Icons.Outlined.Settings)

    // Sub-screens
    object CustomerDetail : Screen("customer_detail/{customerId}", "جزئیات مشتری") {
        fun createRoute(customerId: String) = "customer_detail/$customerId"
    }

    object ProjectDetail : Screen("project_detail/{projectId}", "پرونده پروژه") {
        fun createRoute(projectId: String) = "project_detail/$projectId"
    }

    object ProjectSpecs : Screen("project_specs/{projectId}", "مشخصات تابلوها") {
        fun createRoute(projectId: String) = "project_specs/$projectId"
    }

    object ProjectInvoice : Screen("project_invoice/{projectId}", "فاکتور") {
        fun createRoute(projectId: String) = "project_invoice/$projectId"
    }

    object ContractEditor : Screen("contract_editor/{projectId}", "تنظیم قرارداد") {
        fun createRoute(projectId: String) = "contract_editor/$projectId"
    }

    object ProjectStages : Screen("project_stages", "مدیریت مراحل پروژه")

    object Trash : Screen("trash", "سطل زباله")

    object AuditLogs : Screen("audit_logs", "سابقه فعالیت‌ها و تغییرات")

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(
                Dashboard,
                Customers,
                Projects,
                Finance,
                Contracts
            )
    }
}

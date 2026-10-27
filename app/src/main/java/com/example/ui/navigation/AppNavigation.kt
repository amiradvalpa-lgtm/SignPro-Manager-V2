package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.data.local.entities.CheckEntity
import com.example.data.local.entities.CustomerEntity
import com.example.data.local.entities.PaymentEntity
import com.example.data.local.entities.ProjectEntity
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.contracts.ContractEditorScreen
import com.example.ui.screens.contracts.ContractsScreen
import com.example.ui.screens.customers.CustomerDetailScreen
import com.example.ui.screens.customers.CustomerEditDialog
import com.example.ui.screens.customers.CustomersScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.finance.CheckEditDialog
import com.example.ui.screens.finance.FinanceScreen
import com.example.ui.screens.finance.PaymentEditDialog
import com.example.ui.screens.projects.ProjectDetailScreen
import com.example.ui.screens.projects.ProjectEditDialog
import com.example.ui.screens.projects.ProjectInvoiceScreen
import com.example.ui.screens.projects.ProjectSpecsScreen
import com.example.ui.screens.projects.ProjectsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.viewmodels.SignageViewModel

@Composable
fun AppNavigation(viewModel: SignageViewModel) {
    val navController = rememberNavController()
    val isAuthEnabled by viewModel.isAuthEnabled.collectAsState()
    var isAuthenticated by remember(isAuthEnabled) { mutableStateOf(!isAuthEnabled) }

    val customers by viewModel.customers.collectAsState()
    val currencyUnit by viewModel.currencyUnit.collectAsState()
    val warnUnsaved by viewModel.warnUnsavedChanges.collectAsState()

    // Global quick dialog triggers from Dashboard
    var showQuickCustomerDialog by remember { mutableStateOf(false) }
    var showQuickProjectDialog by remember { mutableStateOf(false) }
    var showQuickPaymentDialog by remember { mutableStateOf(false) }
    var showQuickCheckDialog by remember { mutableStateOf(false) }

    if (isAuthEnabled && !isAuthenticated) {
        LoginScreen(
            viewModel = viewModel,
            onLoginSuccess = { isAuthenticated = true }
        )
    } else {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val currentRoute = currentDestination?.route

        // Determine if bottom bar should be shown
        val showBottomBar = currentRoute in listOf(
            Screen.Dashboard.route,
            Screen.Customers.route,
            Screen.Projects.route,
            Screen.Finance.route,
            Screen.Contracts.route
        )

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                        NavigationBar(
                            modifier = Modifier.testTag("app_bottom_nav_bar"),
                            containerColor = MaterialTheme.colorScheme.surface,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            Screen.bottomNavItems.filterNotNull().forEach { screen ->
                                val selected = currentRoute == screen.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        if (currentRoute != screen.route) {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = {
                                        val iconVector = if (selected) screen.icon else (screen.unselectedIcon ?: screen.icon)
                                        iconVector?.let {
                                            Icon(imageVector = it, contentDescription = screen.title)
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = screen.title,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.testTag("nav_item_${screen.route}")
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 1. Dashboard
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateTo = { route -> navController.navigate(route) },
                        onOpenCustomerDialog = { showQuickCustomerDialog = true },
                        onOpenProjectDialog = { showQuickProjectDialog = true },
                        onOpenPaymentDialog = { showQuickPaymentDialog = true },
                        onOpenCheckDialog = { showQuickCheckDialog = true }
                    )
                }

                // 2. Customers
                composable(Screen.Customers.route) {
                    CustomersScreen(
                        viewModel = viewModel,
                        onNavigateToCustomerDetail = { customerId ->
                            navController.navigate(Screen.CustomerDetail.createRoute(customerId))
                        }
                    )
                }

                // 3. Projects
                composable(Screen.Projects.route) {
                    ProjectsScreen(
                        viewModel = viewModel,
                        onNavigateToProjectDetail = { projectId ->
                            navController.navigate(Screen.ProjectDetail.createRoute(projectId))
                        }
                    )
                }

                // 4. Finance
                composable(Screen.Finance.route) {
                    FinanceScreen(viewModel = viewModel)
                }

                // 5. Contracts
                composable(Screen.Contracts.route) {
                    ContractsScreen(
                        viewModel = viewModel,
                        onNavigateToContractEditor = { projectId ->
                            navController.navigate(Screen.ContractEditor.createRoute(projectId))
                        }
                    )
                }

                // Settings
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                // Customer Detail
                composable(
                    route = Screen.CustomerDetail.route,
                    arguments = listOf(navArgument("customerId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
                    CustomerDetailScreen(
                        customerId = customerId,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToProjectDetail = { projectId ->
                            navController.navigate(Screen.ProjectDetail.createRoute(projectId))
                        }
                    )
                }

                // Project Detail
                composable(
                    route = Screen.ProjectDetail.route,
                    arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                    ProjectDetailScreen(
                        projectId = projectId,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToContractEditor = { pId ->
                            navController.navigate(Screen.ContractEditor.createRoute(pId))
                        },
                        onNavigateToSpecs = { pId ->
                            navController.navigate(Screen.ProjectSpecs.createRoute(pId))
                        },
                        onNavigateToInvoice = { pId ->
                            navController.navigate(Screen.ProjectInvoice.createRoute(pId))
                        }
                    )
                }

                // Project Specs (Full Screen)
                composable(
                    route = Screen.ProjectSpecs.route,
                    arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                    ProjectSpecsScreen(
                        projectId = projectId,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                // Project Invoice (Full Screen)
                composable(
                    route = Screen.ProjectInvoice.route,
                    arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                    ProjectInvoiceScreen(
                        projectId = projectId,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                // Contract Editor
                composable(
                    route = Screen.ContractEditor.route,
                    arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                    ContractEditorScreen(
                        projectId = projectId,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    // Quick Dialogs from Dashboard
    if (showQuickCustomerDialog) {
        CustomerEditDialog(
            customer = null,
            warnUnsavedChanges = warnUnsaved,
            onDismiss = { showQuickCustomerDialog = false },
            onSave = { entity, isNew ->
                viewModel.saveCustomer(entity, isNew) {
                    showQuickCustomerDialog = false
                }
            }
        )
    }

    if (showQuickProjectDialog) {
        ProjectEditDialog(
            project = null,
            customers = customers,
            warnUnsavedChanges = warnUnsaved,
            onDismiss = { showQuickProjectDialog = false },
            onSave = { entity, isNew ->
                viewModel.saveProject(entity, isNew) {
                    showQuickProjectDialog = false
                }
            }
        )
    }

    if (showQuickPaymentDialog) {
        PaymentEditDialog(
            payment = null,
            customers = customers,
            currencyUnit = currencyUnit,
            warnUnsavedChanges = warnUnsaved,
            onDismiss = { showQuickPaymentDialog = false },
            onSave = { entity, isNew ->
                viewModel.savePayment(entity, isNew) {
                    showQuickPaymentDialog = false
                }
            }
        )
    }

    if (showQuickCheckDialog) {
        CheckEditDialog(
            check = null,
            customers = customers,
            currencyUnit = currencyUnit,
            warnUnsavedChanges = warnUnsaved,
            onDismiss = { showQuickCheckDialog = false },
            onSave = { entity, isNew ->
                viewModel.saveCheck(entity, isNew) {
                    showQuickCheckDialog = false
                }
            }
        )
    }
}

package com.example.ui.screens.projects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.data.local.entities.ProjectEntity
import com.example.ui.components.common.*
import com.example.ui.viewmodels.SignageViewModel

@Composable
fun ProjectsScreen(
    viewModel: SignageViewModel,
    onNavigateToProjectDetail: (String) -> Unit
) {
    val projects by viewModel.projects.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val stages by viewModel.stages.collectAsState()
    val warnUnsavedChanges by viewModel.warnUnsavedChanges.collectAsState()

    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("همه") }
    var selectedProjectForEdit by remember { mutableStateOf<ProjectEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }

    val filterOptions = remember(stages) {
        val sortedStages = stages.sortedBy { it.displayOrder }
        val list = mutableListOf("همه")
        sortedStages.forEach { list.add("در مرحله ${it.name}") }
        list.add("تکمیل‌شده")
        list.add("معلق")
        list.add("لغوشده")
        list
    }

    val filteredProjects = remember(projects, stages, searchQuery, selectedStatusFilter) {
        projects.filter { proj ->
            val matchesSearch = searchQuery.isBlank() || proj.title.contains(searchQuery.trim(), ignoreCase = true)
            val matchesFilter = ProjectStatusHelper.matchesFilter(proj, stages, selectedStatusFilter)
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = "پروژه‌های تابلوسازی",
                actions = {
                    IconButton(
                        onClick = { isSearchOpen = !isSearchOpen },
                        modifier = Modifier.testTag("projects_search_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isSearchOpen) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "جستجو",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("add_project_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "پروژه جدید")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            AnimatedVisibility(
                visible = isSearchOpen,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(4.dp))
                    AppSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "جستجوی عنوان پروژه...",
                        testTag = "projects_search_input"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterOptions) { filter ->
                    val isSelected = selectedStatusFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedStatusFilter = filter },
                        label = { Text(filter, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredProjects.isEmpty()) {
                EmptyStateView(
                    title = if (searchQuery.isNotEmpty() || selectedStatusFilter != "همه") "پروژه‌ای یافت نشد" else "هنوز پروژه‌ای ثبت نشده است",
                    message = "برای ثبت و مدیریت اولین سفارش تابلوسازی روی دکمه (+) کلیک کنید.",
                    icon = Icons.Default.BusinessCenter,
                    actionButtonText = if (searchQuery.isEmpty() && selectedStatusFilter == "همه") "تعریف پروژه جدید" else null,
                    onActionClick = { showCreateDialog = true }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredProjects, key = { it.id }) { project ->
                        val customer = customers.firstOrNull { it.id == project.customerId }
                        val chipDisplay = ProjectStatusHelper.getProjectChip(project, stages)
                        ProjectListItem(
                            project = project,
                            customerName = customer?.name ?: "مشتری نامشخص",
                            chipDisplay = chipDisplay,
                            onClick = { onNavigateToProjectDetail(project.id) },
                            onEdit = { selectedProjectForEdit = project },
                            onDelete = { projectToDelete = project }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        ProjectEditDialog(
            project = null,
            customers = customers,
            warnUnsavedChanges = warnUnsavedChanges,
            onDismiss = { showCreateDialog = false },
            onSave = { entity, isNew ->
                viewModel.saveProject(entity, isNew) {
                    showCreateDialog = false
                }
            }
        )
    }

    selectedProjectForEdit?.let { proj ->
        ProjectEditDialog(
            project = proj,
            customers = customers,
            warnUnsavedChanges = warnUnsavedChanges,
            onDismiss = { selectedProjectForEdit = null },
            onSave = { entity, isNew ->
                viewModel.saveProject(entity, isNew) {
                    selectedProjectForEdit = null
                }
            }
        )
    }

    DeleteConfirmationDialog(
        show = projectToDelete != null,
        title = "حذف پروژه",
        message = "آیا از حذف این پروژه اطمینان دارید؟ تمام مشخصات و صورت‌حساب‌های متصل به آن ردیابی می‌شوند.",
        itemDetail = projectToDelete?.title,
        onConfirm = {
            projectToDelete?.let { p ->
                viewModel.deleteProject(p) {
                    projectToDelete = null
                }
            }
        },
        onDismiss = { projectToDelete = null }
    )
}

@Composable
fun ProjectListItem(
    project: ProjectEntity,
    customerName: String,
    chipDisplay: ProjectChipDisplay,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    AppCard(
        onClick = onClick,
        modifier = Modifier.testTag("project_item_${project.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Title on right, Status Chip + 3-Dots Menu on left
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val defaultPrimaryColor = MaterialTheme.colorScheme.primary
                    val chipColor = remember(chipDisplay.colorHex, defaultPrimaryColor) {
                        try {
                            if (chipDisplay.colorHex != null) {
                                androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(chipDisplay.colorHex))
                            } else {
                                defaultPrimaryColor
                            }
                        } catch (_: Exception) {
                            defaultPrimaryColor
                        }
                    }

                    Surface(
                        color = chipColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = chipDisplay.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = chipColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // 3-Dots Overflow Menu without background
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "عملیات پروژه",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("ویرایش پروژه") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "حذف پروژه",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Two-column aligned metadata rows (Customer and Start Date)
            ProjectDetailRow(label = "کارفرما:", value = customerName)
            Spacer(modifier = Modifier.height(4.dp))
            ProjectDetailRow(label = "تاریخ شروع:", value = project.startDate)
        }
    }
}

@Composable
fun ProjectDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Projects Screen Preview",
    showBackground = true,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun ProjectsScreenPreview() {
    com.example.ui.theme.MyApplicationTheme(themeMode = "LIGHT") {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
        ) {
            Surface(color = MaterialTheme.colorScheme.background) {
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        AppTopBar(
                            title = "پروژه‌های تابلوسازی",
                            actions = {
                                IconButton(onClick = {}) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "جستجو",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {},
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "پروژه جدید")
                        }
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(listOf("همه", "در حال مذاکره", "طراحی", "در حال ساخت", "نصب", "تکمیل شده")) { filter ->
                                val isSelected = filter == "همه"
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {},
                                    label = { Text(filter, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        ProjectListItem(
                            project = ProjectEntity(
                                id = "1",
                                title = "تابلو چلنیوم داروخانه دکتر کریمی",
                                customerId = "c1",
                                startDate = "1403/08/10",
                                status = "ACTIVE",
                                description = "طول ۵ متر با نور SMD آفتابی"
                            ),
                            customerName = "دکتر کریمی",
                            chipDisplay = ProjectChipDisplay(label = "در مرحله ساخت", colorHex = "#E11D48"),
                            onClick = {},
                            onEdit = {},
                            onDelete = {}
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        ProjectListItem(
                            project = ProjectEntity(
                                id = "2",
                                title = "تابلو کامپوزیت و حروف برجسته کافه لاته",
                                customerId = "c2",
                                startDate = "1403/08/15",
                                status = "ACTIVE",
                                description = "طراحی مدرن با نور مخفی نئون"
                            ),
                            customerName = "آقای فرهمند",
                            chipDisplay = ProjectChipDisplay(label = "در مرحله طراحی", colorHex = "#0284C7"),
                            onClick = {},
                            onEdit = {},
                            onDelete = {}
                        )
                    }
                }
            }
        }
    }
}

package com.example.ui.screens.projects

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
    val warnUnsavedChanges by viewModel.warnUnsavedChanges.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("همه") }
    var selectedProjectForEdit by remember { mutableStateOf<ProjectEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }

    val filterOptions = listOf("همه", "در حال مذاکره", "طراحی", "در حال ساخت", "نصب", "تکمیل شده", "تسویه شده")

    val filteredProjects = remember(projects, searchQuery, selectedStatusFilter) {
        projects.filter { proj ->
            val matchesSearch = searchQuery.isBlank() || proj.title.contains(searchQuery.trim(), ignoreCase = true)
            val matchesStatus = selectedStatusFilter == "همه" || proj.status == selectedStatusFilter
            matchesSearch && matchesStatus
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = "پروژه‌های تابلوسازی",
                subtitle = "${projects.size} پروژه ثبت شده"
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
            Spacer(modifier = Modifier.height(8.dp))

            PersianTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "جستجوی پروژه...",
                leadingIcon = Icons.Default.Search,
                testTag = "projects_search_input"
            )

            Spacer(modifier = Modifier.height(10.dp))

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
                        ProjectListItem(
                            project = project,
                            customerName = customer?.name ?: "مشتری نامشخص",
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
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard(
        onClick = onClick,
        modifier = Modifier.testTag("project_item_${project.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "کارفرما: $customerName • تاریخ شروع: ${project.startDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(status = project.status)
            }

            if (project.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = project.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "ویرایش", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Project Card Preview", showBackground = true)
@Composable
fun ProjectCardPreview() {
    com.example.ui.theme.MyApplicationTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ProjectListItem(
                project = ProjectEntity(
                    id = "1",
                    title = "تابلو چلنیوم داروخانه دکتر کریمی",
                    customerId = "c1",
                    startDate = "1403/08/10",
                    status = "در حال ساخت",
                    description = "طول ۵ متر با نور SMD آفتابی"
                ),
                customerName = "دکتر کریمی",
                onClick = {},
                onEdit = {},
                onDelete = {}
            )
        }
    }
}

package com.example.ui.screens.projects

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.data.local.entities.ProjectSpecEntity
import com.example.ui.components.common.AppCard
import com.example.ui.components.common.AppTopBar
import com.example.ui.components.common.CardOverflowMenu
import com.example.ui.components.common.DeleteConfirmationDialog
import com.example.ui.components.common.EmptyStateView
import com.example.ui.viewmodels.SignageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSpecsScreen(
    projectId: String,
    viewModel: SignageViewModel,
    onBack: () -> Unit
) {
    val project by viewModel.repository.getProjectByIdFlow(projectId).collectAsState(initial = null)
    val specs by viewModel.getSpecsForProject(projectId).collectAsState(initial = emptyList())
    val warnUnsaved by viewModel.warnUnsavedChanges.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedSpecForEdit by remember { mutableStateOf<ProjectSpecEntity?>(null) }
    var specToDelete by remember { mutableStateOf<ProjectSpecEntity?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "مشخصات تابلوها",
                subtitle = project?.title,
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.testTag("add_spec_top_bar_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "افزودن مشخصات تابلو")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("افزودن مشخصات تابلو") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_spec")
            )
        }
    ) { padding ->
        if (specs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                EmptyStateView(
                    title = "مشخصاتی ثبت نشده است",
                    message = "برای صدور دقیق قرارداد و محاسبات متراژ، ابعاد و نوع تابلو را ثبت کنید.",
                    icon = Icons.Default.Straighten,
                    actionButtonText = "افزودن اولین مشخصات تابلو",
                    onActionClick = { showCreateDialog = true }
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
                        text = "مجموع زیرمجموعه‌های تابلو: ${specs.size} مورد",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(specs, key = { it.id }) { spec ->
                    AppCard {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            // Overflow menu on top-left (per design rule)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                            ) {
                                CardOverflowMenu(
                                    onEdit = { selectedSpecForEdit = spec },
                                    onDelete = { specToDelete = spec }
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                                    .padding(end = 28.dp) // space for menu icon
                            ) {
                                Text(
                                    text = spec.subTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = spec.signType,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Dimension display in centimeters
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "طول: ${spec.lengthCm} سانتی‌متر",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "عرض: ${spec.widthCm} سانتی‌متر",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (spec.depthCm > 0) {
                                        Text(
                                            text = "عمق: ${spec.depthCm} سانتی‌متر",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (spec.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "توضیحات: ${spec.description}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Attachments preview
                                val uris = spec.attachmentUris.split(",").filter { it.isNotBlank() }
                                if (uris.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(uris) { uriStr ->
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                            ) {
                                                Image(
                                                    painter = rememberAsyncImagePainter(uriStr),
                                                    contentDescription = "طرح ضمیمه",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    if (showCreateDialog) {
        SpecEditDialog(
            projectId = projectId,
            spec = null,
            warnUnsavedChanges = warnUnsaved,
            onDismiss = { showCreateDialog = false },
            onSave = { entity, isNew ->
                viewModel.saveSpec(entity, isNew) {
                    showCreateDialog = false
                }
            }
        )
    }

    selectedSpecForEdit?.let { sp ->
        SpecEditDialog(
            projectId = projectId,
            spec = sp,
            warnUnsavedChanges = warnUnsaved,
            onDismiss = { selectedSpecForEdit = null },
            onSave = { entity, isNew ->
                viewModel.saveSpec(entity, isNew) {
                    selectedSpecForEdit = null
                }
            }
        )
    }

    DeleteConfirmationDialog(
        show = specToDelete != null,
        title = "حذف مشخصات تابلو",
        message = "آیا از حذف این ردیف مشخصات تابلو اطمینان دارید؟",
        itemDetail = specToDelete?.subTitle,
        onConfirm = {
            specToDelete?.let { sp ->
                viewModel.deleteSpec(sp) {
                    specToDelete = null
                }
            }
        },
        onDismiss = { specToDelete = null }
    )
}

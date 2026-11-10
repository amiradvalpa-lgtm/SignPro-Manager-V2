package com.example.ui.screens.projects

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.theme.MyApplicationTheme

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
    var viewingImageUriAndTitle by remember { mutableStateOf<Pair<String, String>?>(null) }

    ProjectSpecsContent(
        projectTitle = project?.title,
        specs = specs,
        onBack = onBack,
        onAddSpec = { showCreateDialog = true },
        onEditSpec = { spec -> selectedSpecForEdit = spec },
        onDeleteSpec = { spec -> specToDelete = spec },
        onImageClick = { uri, title -> viewingImageUriAndTitle = Pair(uri, title) }
    )

    viewingImageUriAndTitle?.let { (uri, title) ->
        SpecImageViewerDialog(
            imageUri = uri,
            title = title,
            onDismiss = { viewingImageUriAndTitle = null }
        )
    }

    if (showCreateDialog) {
        val lastSignType = specs.lastOrNull()?.signType
        SpecEditDialog(
            projectId = projectId,
            spec = null,
            defaultSignType = lastSignType,
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
        itemDetail = specToDelete?.signType,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSpecsContent(
    projectTitle: String?,
    specs: List<ProjectSpecEntity>,
    onBack: () -> Unit,
    onAddSpec: () -> Unit,
    onEditSpec: (ProjectSpecEntity) -> Unit,
    onDeleteSpec: (ProjectSpecEntity) -> Unit,
    onImageClick: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var revealedSpecId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = "مشخصات تابلوها",
                subtitle = projectTitle,
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = onAddSpec,
                        modifier = Modifier.testTag("add_spec_top_bar_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "افزودن مشخصات تابلو")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddSpec,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("افزودن مشخصات تابلو", fontWeight = FontWeight.Bold) },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
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
                    onActionClick = onAddSpec
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
                    ProjectSpecItemCard(
                        spec = spec,
                        isRevealed = (revealedSpecId == spec.id),
                        onExpanded = { revealedSpecId = spec.id },
                        onCollapsed = { if (revealedSpecId == spec.id) revealedSpecId = null },
                        onEdit = { onEditSpec(spec) },
                        onDelete = { onDeleteSpec(spec) },
                        onImageClick = onImageClick
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

private fun formatDimension(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        value.toString()
    }
}

@Composable
private fun DimensionRow(
    label: String,
    value: String,
    unit: String = "سانتی‌متر"
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Column 1: Label (fixed width for column alignment)
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(46.dp)
        )
        // Column 2: Value (minimum width so unit starts at aligned position)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.widthIn(min = 44.dp)
        )
        // Column 3: Unit
        Text(
            text = unit,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
        )
    }
}

@Composable
fun ProjectSpecItemCard(
    spec: ProjectSpecEntity,
    isRevealed: Boolean = false,
    onExpanded: (() -> Unit)? = null,
    onCollapsed: (() -> Unit)? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onImageClick: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    com.example.ui.components.common.SwipeToRevealCard(
        isRevealed = isRevealed,
        onExpanded = onExpanded,
        onCollapsed = onCollapsed,
        onEdit = onEdit,
        onDelete = onDelete,
        modifier = modifier.testTag("spec_item_${spec.id}")
    ) {
        AppCard(onClick = onEdit) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Main Upper Row: Right side (material name & tabular dimensions) | Left side (proportional image)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Right Column (Start in RTL):
                    // Top: Material name without chip in bold font
                    // Bottom: Dimensions, aligned strictly into columns (عناوین، اعداد، سانتی‌متر)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = spec.signType,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Length row
                        DimensionRow(label = "طول:", value = formatDimension(spec.lengthCm))

                        // Height row (ارتفاع)
                        DimensionRow(label = "ارتفاع:", value = formatDimension(spec.widthCm))

                        // Depth row (عمق)
                        if (spec.depthCm > 0) {
                            DimensionRow(label = "عمق:", value = formatDimension(spec.depthCm))
                        }
                    }

                    // Left Column (End in RTL): Attached image thumbnail matching upper card height in both width & height
                    val uris = remember(spec.attachmentUris) {
                        spec.attachmentUris.split(",").filter { it.isNotBlank() }
                    }
                    if (uris.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .then(
                                    if (onImageClick != null) {
                                        Modifier.clickable { onImageClick(uris.first(), spec.signType) }
                                    } else Modifier
                                )
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(uris.first()),
                                contentDescription = "طرح ضمیمه",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (uris.size > 1) {
                                Surface(
                                    color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(topStart = 8.dp),
                                    modifier = Modifier.align(Alignment.BottomEnd)
                                ) {
                                    Text(
                                        text = "+${uris.size - 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Last line: Description (اگر توضیحات بود توضیحات باشه)
                if (spec.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "توضیحات:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = spec.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// PREVIEWS
// -------------------------------------------------------------

@Preview(
    name = "Project Specs Screen - Phone",
    showBackground = true,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun ProjectSpecsScreenPreview() {
    MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            ProjectSpecsContent(
                projectTitle = "تابلو چلنیوم و کامپوزیت - نمایندگی سامسونگ",
                specs = listOf(
                    ProjectSpecEntity(
                        id = "1",
                        projectId = "p1",
                        subTitle = "تابلو ورودی اصلی",
                        signType = "چلنیوم کریستالی با نور SMD",
                        lengthCm = 450.0,
                        widthCm = 120.0,
                        depthCm = 12.0,
                        description = "لبه طلایی اسپانیایی با پلکسی تایوانی ۷ رنگ"
                    ),
                    ProjectSpecEntity(
                        id = "2",
                        projectId = "p1",
                        subTitle = "زیرسازی نما",
                        signType = "کامپوزیت پنل ضد حریق",
                        lengthCm = 800.0,
                        widthCm = 240.0,
                        depthCm = 0.0,
                        description = "رنگ نقره‌ای مات با آهن‌کشی شبکه ۴۰*۴۰"
                    )
                ),
                onBack = {},
                onAddSpec = {},
                onEditSpec = {},
                onDeleteSpec = {}
            )
        }
    }
}

@Preview(name = "Project Spec Item Card Preview", showBackground = true)
@Composable
fun ProjectSpecItemCardPreview() {
    MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            ProjectSpecItemCard(
                spec = ProjectSpecEntity(
                    id = "1",
                    projectId = "p1",
                    subTitle = "تابلو ورودی اصلی",
                    signType = "چلنیوم کریستالی با نور SMD",
                    lengthCm = 450.0,
                    widthCm = 120.0,
                    depthCm = 12.0,
                    description = "لبه طلایی اسپانیایی با پلکسی تایوانی ۷ رنگ"
                ),
                onEdit = {},
                onDelete = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

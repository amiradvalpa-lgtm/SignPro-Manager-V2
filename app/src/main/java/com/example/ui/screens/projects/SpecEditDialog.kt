package com.example.ui.screens.projects

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.example.data.local.entities.ProjectSpecEntity
import com.example.ui.components.common.AppFormBottomSheet
import com.example.ui.components.common.PersianTextField
import com.example.ui.components.common.StickyBottomActionBar
import com.example.ui.components.common.UnsavedChangesDialog
import com.example.ui.theme.MyApplicationTheme
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecEditDialog(
    projectId: String,
    spec: ProjectSpecEntity?,
    warnUnsavedChanges: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (ProjectSpecEntity, Boolean) -> Unit
) {
    val context = LocalContext.current
    val isNew = spec == null

    var subTitle by remember { mutableStateOf(spec?.subTitle ?: "") }
    var signType by remember { mutableStateOf(spec?.signType ?: "چلنیوم طلایی") }
    var lengthStr by remember { mutableStateOf(if (spec != null) spec.lengthCm.toString() else "") }
    var widthStr by remember { mutableStateOf(if (spec != null) spec.widthCm.toString() else "") }
    var depthStr by remember { mutableStateOf(if (spec != null) spec.depthCm.toString() else "0") }
    var description by remember { mutableStateOf(spec?.description ?: "") }

    var attachmentUris by remember {
        mutableStateOf(spec?.attachmentUris?.split(",")?.filter { it.isNotBlank() } ?: emptyList())
    }

    var subTitleError by remember { mutableStateOf<String?>(null) }
    var signTypeError by remember { mutableStateOf<String?>(null) }
    var lengthError by remember { mutableStateOf<String?>(null) }
    var widthError by remember { mutableStateOf<String?>(null) }
    var depthError by remember { mutableStateOf<String?>(null) }

    var signTypeDropdownExpanded by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    val signTypePresets = listOf(
        "چلنیوم طلایی (پایه)",
        "چلنیوم الماسی و ستاره‌ای",
        "استیل نقره‌ای و طلایی",
        "حروف پلاستیک (پلکسی ۳ بعدی)",
        "نئون فلکسی سیلیکونی",
        "لایت‌باکس کریستالی / آلومینیومی",
        "کامپوزیت طرح چوب و سنگ",
        "ترمووود فنلاندی",
        "تابلو ال‌ای‌دی ثابت و روان",
        "حروف وکیوم سه بعدی",
        "پیکسل و پوینت لایت فول‌کالر"
    )

    val isDirty by remember {
        derivedStateOf {
            if (spec == null) {
                subTitle.isNotBlank() || lengthStr.isNotBlank() || widthStr.isNotBlank() || attachmentUris.isNotEmpty()
            } else {
                subTitle != spec.subTitle ||
                        signType != spec.signType ||
                        lengthStr != spec.lengthCm.toString() ||
                        widthStr != spec.widthCm.toString() ||
                        depthStr != spec.depthCm.toString() ||
                        description != spec.description ||
                        attachmentUris.joinToString(",") != spec.attachmentUris
            }
        }
    }

    val attemptDismiss = {
        if (warnUnsavedChanges && isDirty) {
            showUnsavedDialog = true
        } else {
            onDismiss()
        }
    }

    // Android zero-permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val newUris = attachmentUris.toMutableList()
            uris.forEach { u ->
                val str = u.toString()
                if (!newUris.contains(str)) newUris.add(str)
            }
            attachmentUris = newUris
        }
    }

    // Camera capture
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempCameraUri != null) {
            val newUris = attachmentUris.toMutableList()
            newUris.add(tempCameraUri.toString())
            attachmentUris = newUris
        }
    }

    fun launchCamera() {
        try {
            val photoDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
            val photoFile = File(photoDir, "spec_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (_: Exception) {
        }
    }

    val executeSave = {
        var hasError = false
        if (subTitle.trim().isEmpty()) {
            subTitleError = "عنوان زیرمجموعه الزامی است."
            hasError = true
        }
        if (signType.trim().isEmpty()) {
            signTypeError = "نوع تابلو الزامی است."
            hasError = true
        }
        val l = lengthStr.toDoubleOrNull()
        if (l == null || l <= 0) {
            lengthError = "طول نامعتبر است."
            hasError = true
        }
        val w = widthStr.toDoubleOrNull()
        if (w == null || w <= 0) {
            widthError = "عرض نامعتبر است."
            hasError = true
        }
        val d = depthStr.toDoubleOrNull() ?: 0.0

        if (!hasError) {
            val entity = spec?.copy(
                subTitle = subTitle.trim(),
                signType = signType.trim(),
                lengthCm = l ?: 0.0,
                widthCm = w ?: 0.0,
                depthCm = d,
                attachmentUris = attachmentUris.joinToString(","),
                description = description.trim()
            ) ?: ProjectSpecEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                subTitle = subTitle.trim(),
                signType = signType.trim(),
                lengthCm = l ?: 0.0,
                widthCm = w ?: 0.0,
                depthCm = d,
                attachmentUris = attachmentUris.joinToString(","),
                description = description.trim()
            )
            onSave(entity, isNew)
        }
    }

    AppFormBottomSheet(
        title = if (isNew) "افزودن مشخصات فنی تابلو" else "ویرایش مشخصات تابلو",
        subtitle = "تعیین متریال، ابعاد، پیوست‌های طرح و جزئیات ساخت",
        onDismissRequest = onDismiss,
        isDirty = isDirty,
        warnUnsavedChanges = warnUnsavedChanges,
        onAttemptDismiss = attemptDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PersianTextField(
                value = subTitle,
                onValueChange = {
                    subTitle = it
                    subTitleError = null
                },
                label = "عنوان بخش یا زیرمجموعه تابلو",
                placeholder = "مثال: حروف اصلی سردر، لوگوی گرد، لایت‌باکس ورودی",
                isError = subTitleError != null,
                errorMessage = subTitleError,
                testTag = "spec_subtitle_input"
            )

            // Sign Type Dropdown
            ExposedDropdownMenuBox(
                expanded = signTypeDropdownExpanded,
                onExpandedChange = { signTypeDropdownExpanded = !signTypeDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = signType,
                    onValueChange = {
                        signType = it
                        signTypeError = null
                    },
                    label = { Text("نوع تابلو و متریال") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = signTypeDropdownExpanded) },
                    isError = signTypeError != null,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable)
                )
                ExposedDropdownMenu(
                    expanded = signTypeDropdownExpanded,
                    onDismissRequest = { signTypeDropdownExpanded = false }
                ) {
                    signTypePresets.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(preset) },
                            onClick = {
                                signType = preset
                                signTypeError = null
                                signTypeDropdownExpanded = false
                            }
                        )
                    }
                }
            }
            if (signTypeError != null) {
                Text(text = signTypeError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            // Dimensions in 3 separate rows with fixed "سانتی‌متر" suffix
            Text(
                text = "ابعاد تابلو",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Row 1: Length
            OutlinedTextField(
                value = lengthStr,
                onValueChange = {
                    lengthStr = it
                    lengthError = null
                },
                label = { Text("طول") },
                placeholder = { Text("مثال: ۳۵۰") },
                trailingIcon = {
                    Text(
                        text = "سانتی‌متر",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = lengthError != null,
                supportingText = if (lengthError != null) {
                    { Text(lengthError ?: "", color = MaterialTheme.colorScheme.error) }
                } else null,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("spec_length_input")
            )

            // Row 2: Width
            OutlinedTextField(
                value = widthStr,
                onValueChange = {
                    widthStr = it
                    widthError = null
                },
                label = { Text("عرض") },
                placeholder = { Text("مثال: ۱۲۰") },
                trailingIcon = {
                    Text(
                        text = "سانتی‌متر",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = widthError != null,
                supportingText = if (widthError != null) {
                    { Text(widthError ?: "", color = MaterialTheme.colorScheme.error) }
                } else null,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("spec_width_input")
            )

            // Row 3: Depth
            OutlinedTextField(
                value = depthStr,
                onValueChange = {
                    depthStr = it
                    depthError = null
                },
                label = { Text("عمق") },
                placeholder = { Text("مثال: ۸") },
                trailingIcon = {
                    Text(
                        text = "سانتی‌متر",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = depthError != null,
                supportingText = if (depthError != null) {
                    { Text(depthError ?: "", color = MaterialTheme.colorScheme.error) }
                } else null,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("spec_depth_input")
            )

            PersianTextField(
                value = description,
                onValueChange = { description = it },
                label = "توضیحات فنی، رنگ نور و جنس لبه",
                placeholder = "مثال: پلکسی یرلانگ تایوان، SMD بلوکی اینجکشن لنزدار با ترانس ۳۳ آمپر",
                singleLine = false,
                testTag = "spec_description_input"
            )

            // Attachments section: Gallery and Camera
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "فایل‌ها و تصاویر طرح",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("گالری گوشی", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = { launchCamera() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("دوربین مستقیم", style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (attachmentUris.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(attachmentUris) { uriStr ->
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(uriStr),
                                    contentDescription = "طرح ضمیمه",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = {
                                        attachmentUris = attachmentUris.filter { it != uriStr }
                                    },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.TopEnd)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.error,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "حذف عکس",
                                            tint = MaterialTheme.colorScheme.onError,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sticky Bottom Action Bar with 2:1 ratio and Dirty state
        StickyBottomActionBar(
            entityName = "مشخصات تابلو",
            saveButtonText = "ذخیره مشخصات تابلو",
            onSave = executeSave,
            onCancel = onDismiss,
            isDirty = isDirty,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showUnsavedDialog) {
        UnsavedChangesDialog(
            show = true,
            onSaveAndExit = {
                showUnsavedDialog = false
                executeSave()
            },
            onDiscardAndExit = {
                showUnsavedDialog = false
                onDismiss()
            },
            onDismiss = { showUnsavedDialog = false }
        )
    }
}

@Preview(name = "Spec Form Preview", showBackground = true)
@Composable
fun SpecEditDialogPreview() {
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
    ) {
        MyApplicationTheme {
            Surface(color = MaterialTheme.colorScheme.background) {
                SpecEditDialog(
                    projectId = "p1",
                    spec = ProjectSpecEntity(
                        projectId = "p1",
                        subTitle = "حروف برجسته چلنیوم سردر",
                        signType = "چلنیوم طلایی",
                        lengthCm = 420.0,
                        widthCm = 95.0,
                        depthCm = 9.0,
                        description = "پلکسی دوبل با نوردهی آفتابی"
                    ),
                    onDismiss = {},
                    onSave = { _, _ -> }
                )
            }
        }
    }
}

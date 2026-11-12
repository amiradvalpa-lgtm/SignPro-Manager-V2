package com.example.ui.screens.projects

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.example.data.local.entities.ProjectSpecEntity
import com.example.ui.components.common.AppDropdownField
import com.example.ui.components.common.AppFormBottomSheet
import com.example.ui.components.common.LocalShowPlaceholders
import com.example.ui.components.common.PersianTextField
import com.example.ui.components.common.StickyBottomActionBar
import com.example.ui.components.common.UnsavedChangesDialog
import com.example.ui.components.common.appOutlinedTextFieldColors
import com.example.ui.theme.MyApplicationTheme
import java.io.File
import java.util.UUID

private fun formatDimension(value: Double?): String {
    if (value == null) return ""
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        value.toString()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecEditDialog(
    projectId: String,
    projectTitle: String? = null,
    customerName: String? = null,
    spec: ProjectSpecEntity?,
    defaultSignType: String? = null,
    warnUnsavedChanges: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (ProjectSpecEntity, Boolean) -> Unit
) {
    val context = LocalContext.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val isNew = spec == null

    var signType by remember {
        mutableStateOf(spec?.signType ?: defaultSignType ?: "چلنیوم طلایی (پایه)")
    }
    
    // Dimension inputs as TextFieldValue to support select-all on focus
    var lengthField by remember {
        mutableStateOf(
            TextFieldValue(
                text = if (spec != null) formatDimension(spec.lengthCm) else ""
            )
        )
    }

    fun selectAll(field: TextFieldValue, update: (TextFieldValue) -> Unit) {
        if (field.text.isNotEmpty()) {
            coroutineScope.launch {
                delay(60)
                update(field.copy(selection = TextRange(0, field.text.length)))
            }
        }
    }
    var widthField by remember {
        mutableStateOf(
            androidx.compose.ui.text.input.TextFieldValue(
                text = if (spec != null) formatDimension(spec.widthCm) else ""
            )
        )
    }
    var depthField by remember {
        mutableStateOf(
            androidx.compose.ui.text.input.TextFieldValue(
                text = if (spec != null && spec.depthCm > 0) formatDimension(spec.depthCm) else ""
            )
        )
    }
    var description by remember { mutableStateOf(spec?.description ?: "") }

    var attachmentUris by remember {
        mutableStateOf(spec?.attachmentUris?.split(",")?.filter { it.isNotBlank() } ?: emptyList())
    }

    var signTypeError by remember { mutableStateOf<String?>(null) }
    var lengthError by remember { mutableStateOf<String?>(null) }
    var widthError by remember { mutableStateOf<String?>(null) }
    var depthError by remember { mutableStateOf<String?>(null) }
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
                lengthField.text.isNotBlank() ||
                        widthField.text.isNotBlank() ||
                        depthField.text.isNotBlank() ||
                        description.isNotBlank() ||
                        attachmentUris.isNotEmpty() ||
                        (defaultSignType != null && signType != defaultSignType)
            } else {
                signType != spec.signType ||
                        lengthField.text != formatDimension(spec.lengthCm) ||
                        widthField.text != formatDimension(spec.widthCm) ||
                        depthField.text != (if (spec.depthCm > 0) formatDimension(spec.depthCm) else "") ||
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

    // Android zero-permission Photo Picker - immediately copies to app internal storage
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val copiedUris = mutableListOf<String>()
                uris.forEach { u ->
                    val internalUri = com.example.util.FileStorageHelper.saveImageToInternalStorage(
                        context = context,
                        sourceUri = u,
                        projectId = projectId,
                        projectTitle = projectTitle ?: "پروژه",
                        customerName = customerName ?: "کارفرما",
                        signType = signType
                    )
                    if (internalUri != null) {
                        copiedUris.add(internalUri.toString())
                    }
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (copiedUris.isNotEmpty()) {
                        val newUris = attachmentUris.toMutableList()
                        copiedUris.forEach { str ->
                            if (!newUris.contains(str)) newUris.add(str)
                        }
                        attachmentUris = newUris
                    }
                }
            }
        }
    }

    // Camera capture directly into app internal storage
    var tempCameraFile by remember { mutableStateOf<File?>(null) }
    var tempCameraUriString by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempCameraFile != null) {
            val fileUri = Uri.fromFile(tempCameraFile)
            val newUris = attachmentUris.toMutableList()
            newUris.add(fileUri.toString())
            attachmentUris = newUris
        }
    }

    val launchCameraInternal = {
        try {
            val photoFile = com.example.util.FileStorageHelper.createInternalCameraFile(
                context = context,
                projectId = projectId,
                projectTitle = projectTitle ?: "پروژه",
                customerName = customerName ?: "کارفرما",
                signType = signType
            )
            tempCameraFile = photoFile
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            tempCameraUriString = uri.toString()
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                context,
                "خطا در باز کردن دوربین: ${e.localizedMessage ?: "دوربین در دسترس نیست"}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCameraInternal()
        } else {
            android.widget.Toast.makeText(
                context,
                "برای عکاسی مستقیم از تابلو، تأیید دسترسی به دوربین الزامی است.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun launchCamera() {
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            launchCameraInternal()
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    val executeSave = {
        var hasError = false
        if (signType.trim().isEmpty()) {
            signTypeError = "نوع تابلو الزامی است."
            hasError = true
        }
        val l = lengthField.text.toDoubleOrNull()
        if (l == null || l <= 0) {
            lengthError = "طول نامعتبر است."
            hasError = true
        }
        val w = widthField.text.toDoubleOrNull()
        if (w == null || w <= 0) {
            widthError = "ارتفاع نامعتبر است."
            hasError = true
        }
        val d = depthField.text.toDoubleOrNull() ?: 0.0

        if (!hasError) {
            val subTitleValue = spec?.subTitle.takeIf { !it.isNullOrBlank() } ?: signType.trim()
            val entity = spec?.copy(
                subTitle = subTitleValue,
                signType = signType.trim(),
                lengthCm = l ?: 0.0,
                widthCm = w ?: 0.0,
                depthCm = d,
                attachmentUris = attachmentUris.joinToString(","),
                description = description.trim()
            ) ?: ProjectSpecEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                subTitle = subTitleValue,
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
            // Row 1: Sign Type Dropdown (First row as requested)
            AppDropdownField(
                value = signType,
                onValueChange = {
                    signType = it
                    signTypeError = null
                },
                label = "نوع تابلو و متریال",
                options = signTypePresets,
                onOptionSelected = { preset ->
                    signType = preset
                    signTypeError = null
                },
                isEditable = true,
                isError = signTypeError != null,
                errorMessage = signTypeError,
                testTag = "spec_signtype_dropdown"
            )

            // Row 2: Dimensions in 1 single row (طول، ارتفاع، عمق) with cm prefix on the right
            Text(
                text = "ابعاد تابلو",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            val showPlaceholders = LocalShowPlaceholders.current

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Length (طول)
                OutlinedTextField(
                    value = lengthField,
                    onValueChange = { newVal ->
                        val raw = newVal.text
                        val filtered = com.example.util.FormatUtils.toEnglishDigits(raw).filter { ch -> ch.isDigit() || ch == '.' }
                        val clean = if (filtered.count { it == '.' } > 1) {
                            val firstDot = filtered.indexOf('.')
                            filtered.filterIndexed { index, c -> c != '.' || index == firstDot }
                        } else filtered

                        lengthField = if (clean != raw) {
                            val diff = clean.length - raw.length
                            val newPos = (newVal.selection.end + diff).coerceIn(0, clean.length)
                            androidx.compose.ui.text.input.TextFieldValue(clean, androidx.compose.ui.text.TextRange(newPos))
                        } else {
                            newVal
                        }
                        lengthError = null
                    },
                    label = { Text("طول") },
                    placeholder = if (showPlaceholders) {
                        { Text("۳۵۰") }
                    } else null,
                    prefix = {
                        Text(
                            text = "cm ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    isError = lengthError != null,
                    supportingText = if (lengthError != null) {
                        { Text(lengthError ?: "", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    shape = RoundedCornerShape(14.dp),
                    colors = appOutlinedTextFieldColors(),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                selectAll(lengthField) { lengthField = it }
                            }
                        }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(pass = PointerEventPass.Initial)
                                val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                                if (up != null) {
                                    selectAll(lengthField) { lengthField = it }
                                }
                            }
                        }
                        .testTag("spec_length_input")
                )

                // Height/Width (ارتفاع)
                OutlinedTextField(
                    value = widthField,
                    onValueChange = { newVal ->
                        val raw = newVal.text
                        val filtered = com.example.util.FormatUtils.toEnglishDigits(raw).filter { ch -> ch.isDigit() || ch == '.' }
                        val clean = if (filtered.count { it == '.' } > 1) {
                            val firstDot = filtered.indexOf('.')
                            filtered.filterIndexed { index, c -> c != '.' || index == firstDot }
                        } else filtered

                        widthField = if (clean != raw) {
                            val diff = clean.length - raw.length
                            val newPos = (newVal.selection.end + diff).coerceIn(0, clean.length)
                            TextFieldValue(clean, TextRange(newPos))
                        } else {
                            newVal
                        }
                        widthError = null
                    },
                    label = { Text("ارتفاع") },
                    placeholder = if (showPlaceholders) {
                        { Text("۱۲۰") }
                    } else null,
                    prefix = {
                        Text(
                            text = "cm ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    isError = widthError != null,
                    supportingText = if (widthError != null) {
                        { Text(widthError ?: "", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    shape = RoundedCornerShape(14.dp),
                    colors = appOutlinedTextFieldColors(),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                selectAll(widthField) { widthField = it }
                            }
                        }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(pass = PointerEventPass.Initial)
                                val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                                if (up != null) {
                                    selectAll(widthField) { widthField = it }
                                }
                            }
                        }
                        .testTag("spec_width_input")
                )

                // Depth (عمق)
                OutlinedTextField(
                    value = depthField,
                    onValueChange = { newVal ->
                        val raw = newVal.text
                        val filtered = com.example.util.FormatUtils.toEnglishDigits(raw).filter { ch -> ch.isDigit() || ch == '.' }
                        val clean = if (filtered.count { it == '.' } > 1) {
                            val firstDot = filtered.indexOf('.')
                            filtered.filterIndexed { index, c -> c != '.' || index == firstDot }
                        } else filtered

                        depthField = if (clean != raw) {
                            val diff = clean.length - raw.length
                            val newPos = (newVal.selection.end + diff).coerceIn(0, clean.length)
                            TextFieldValue(clean, TextRange(newPos))
                        } else {
                            newVal
                        }
                        depthError = null
                    },
                    label = { Text("عمق") },
                    placeholder = if (showPlaceholders) {
                        { Text("۸") }
                    } else null,
                    prefix = {
                        Text(
                            text = "cm ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    isError = depthError != null,
                    supportingText = if (depthError != null) {
                        { Text(depthError ?: "", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    shape = RoundedCornerShape(14.dp),
                    colors = appOutlinedTextFieldColors(),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                selectAll(depthField) { depthField = it }
                            }
                        }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(pass = PointerEventPass.Initial)
                                val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                                if (up != null) {
                                    selectAll(depthField) { depthField = it }
                                }
                            }
                        }
                        .testTag("spec_depth_input")
                )
            }

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
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(uriStr),
                                    contentDescription = "طرح ضمیمه",
                                    contentScale = ContentScale.Fit,
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

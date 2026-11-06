package com.example.ui.components.common

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.local.entities.CustomerEntity
import com.example.ui.components.pickers.PersianDatePickerModal
import com.example.util.FormatUtils
import com.example.util.JalaliDate
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.draw.rotate

val LocalShowPlaceholders = compositionLocalOf { true }

@Composable
fun appOutlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
)

@Composable
fun AppHeaderDivider(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    showDivider: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("top_bar_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        if (showDivider) {
            AppHeaderDivider()
        }
    }
}

@Composable
fun AppSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "جستجو...",
    modifier: Modifier = Modifier,
    testTag: String = "app_search_input"
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isFocused) 0.85f else 0.65f),
        border = BorderStroke(
            width = if (isFocused) 1.5.dp else 1.dp,
            color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            val showPlaceholders = LocalShowPlaceholders.current
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = if (showPlaceholders && placeholder.isNotEmpty()) {
                    {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                } else null,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag(testTag)
                    .onFocusChanged { isFocused = it.isFocused }
            )
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "پاک کردن",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

class PhoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val isLandline = raw.startsWith("0") && !raw.startsWith("09") && raw.length >= 3
        val formatted = StringBuilder()
        val firstSplit = if (isLandline) 3 else 4
        val secondSplit = if (isLandline) 7 else 7

        for (i in raw.indices) {
            if (i == firstSplit || i == secondSplit) {
                formatted.append(' ')
            }
            formatted.append(raw[i])
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, raw.length)
                var extraSpaces = 0
                if (clamped > firstSplit) extraSpaces++
                if (clamped > secondSplit) extraSpaces++
                return (clamped + extraSpaces).coerceIn(0, formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, formatted.length)
                var spacesBefore = 0
                val firstSpacePos = firstSplit
                val secondSpacePos = secondSplit + 1
                if (clamped > firstSpacePos) spacesBefore++
                if (clamped > secondSpacePos) spacesBefore++
                return (clamped - spacesBefore).coerceIn(0, raw.length)
            }
        }

        return TransformedText(AnnotatedString(formatted.toString()), offsetMapping)
    }
}

@Composable
fun PhoneInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "شماره تماس",
    modifier: Modifier = Modifier,
    placeholder: String = "۰۹۱۲ ۳۴۵ ۶۷۸۹",
    isError: Boolean = false,
    errorMessage: String? = null,
    testTag: String = "phone_input_field"
) {
    // Keep raw numeric digits in state (clean without spaces or hidden markers)
    val rawDigits = remember(value) {
        FormatUtils.toEnglishDigits(value).filter { it.isDigit() }.take(11)
    }

    val showPlaceholders = LocalShowPlaceholders.current

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = rawDigits,
            onValueChange = { input ->
                // Support pasted numbers (+98, spaces, dashes) vs live typing
                val digits = if (input.contains("+") || input.contains(" ") || input.contains("-") || input.length > 11) {
                    FormatUtils.cleanAndNormalizePhone(input)
                } else {
                    FormatUtils.toEnglishDigits(input).filter { it.isDigit() }.take(11)
                }
                onValueChange(digits)
            },
            label = { Text(label) },
            placeholder = if (showPlaceholders && placeholder.isNotEmpty()) {
                {
                    Text(
                        text = placeholder,
                        style = LocalTextStyle.current.copy(
                            textDirection = TextDirection.Ltr,
                            textAlign = TextAlign.Right,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                    )
                }
            } else null,
            visualTransformation = remember { PhoneVisualTransformation() },
            leadingIcon = {
                Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = if (rawDigits.isNotEmpty()) {
                {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "پاک کردن", modifier = Modifier.size(18.dp))
                    }
                }
            } else null,
            textStyle = LocalTextStyle.current.copy(
                textDirection = TextDirection.Ltr,
                textAlign = TextAlign.Right
            ),
            isError = isError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            colors = appOutlinedTextFieldColors()
        )
        if (isError && !errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(0.75.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = content
    )
}

@Composable
fun SwipeToRevealCard(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isRevealed: Boolean = false,
    onExpanded: (() -> Unit)? = null,
    onCollapsed: (() -> Unit)? = null,
    editContentDescription: String = "ویرایش",
    deleteContentDescription: String = "حذف",
    customRightSwipeActions: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val maxRevealPx = remember(density) { with(density) { 130.dp.toPx() } }
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Collapse card automatically when another card is revealed (isRevealed becomes false)
    LaunchedEffect(isRevealed) {
        if (!isRevealed && offsetX.value != 0f) {
            offsetX.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        // Background Actions: Buttons revealed when swiped
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Revealed on swiping towards LEFT of the screen (Start side in RTL layout)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = {
                        coroutineScope.launch {
                            offsetX.animateTo(0f)
                            onCollapsed?.invoke()
                        }
                        onEdit()
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFFFFC107),
                        contentColor = Color(0xFF3A3A3A)
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = editContentDescription,
                        modifier = Modifier.size(20.dp)
                    )
                }

                FilledIconButton(
                    onClick = {
                        coroutineScope.launch {
                            offsetX.animateTo(0f)
                            onCollapsed?.invoke()
                        }
                        onDelete()
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFFF44336),
                        contentColor = Color(0xFFFFFFFF)
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = deleteContentDescription,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Future extensibility: Revealed on swiping towards RIGHT of the screen (End side in RTL layout)
            if (customRightSwipeActions != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    customRightSwipeActions()
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }
        }

        // Allow dragging right only if customRightSwipeActions is present
        val minOffset = if (layoutDirection == LayoutDirection.Rtl) {
            if (customRightSwipeActions != null) -maxRevealPx else 0f
        } else {
            -maxRevealPx
        }
        val maxOffset = if (layoutDirection == LayoutDirection.Rtl) {
            maxRevealPx
        } else {
            if (customRightSwipeActions != null) maxRevealPx else 0f
        }

        // Foreground Card Layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(minOffset, maxOffset) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            // In RTL, Modifier.offset { IntOffset(x, 0) } uses placeRelativeWithLayer where positive x
                            // moves towards End (Left in RTL). Reversing raw dragAmount in RTL makes the card move
                            // exactly 1:1 in the direction the user moves their finger.
                            val dragDelta = if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                            val newOffset = (offsetX.value + dragDelta).coerceIn(minOffset, maxOffset)
                            coroutineScope.launch {
                                offsetX.snapTo(newOffset)
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                val current = offsetX.value
                                val target = when {
                                    current > maxRevealPx * 0.35f && maxOffset > 0f -> maxOffset
                                    current < -maxRevealPx * 0.35f && minOffset < 0f -> minOffset
                                    else -> 0f
                                }
                                if (target != 0f) {
                                    onExpanded?.invoke()
                                } else {
                                    onCollapsed?.invoke()
                                }
                                offsetX.animateTo(
                                    targetValue = target,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(0f)
                                onCollapsed?.invoke()
                            }
                        }
                    )
                }
                .then(
                    if (abs(offsetX.value) > 1f) {
                        Modifier.clickable {
                            coroutineScope.launch {
                                offsetX.animateTo(0f)
                                onCollapsed?.invoke()
                            }
                        }
                    } else Modifier
                )
        ) {
            content()
        }
    }
}

@Composable
fun PersianTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    testTag: String = "persian_text_field"
) {
    val showPlaceholders = LocalShowPlaceholders.current
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = if (showPlaceholders && placeholder.isNotEmpty()) {
                {
                    Text(
                        text = placeholder,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    )
                }
            } else null,
            leadingIcon = if (leadingIcon != null) {
                { Icon(leadingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            } else null,
            trailingIcon = if (value.isNotEmpty()) {
                {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "پاک کردن", modifier = Modifier.size(18.dp))
                    }
                }
            } else null,
            isError = isError,
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(14.dp),
            colors = appOutlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
        AnimatedVisibility(visible = isError && errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }
}

class ThousandsSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        val reversed = raw.reversed()
        val formatted = reversed.chunked(3).joinToString(",").reversed()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, raw.length)
                if (clamped == 0) return 0
                val totalCommas = (raw.length - 1) / 3
                val digitsRight = raw.length - clamped
                val commasRight = if (digitsRight == 0) 0 else (digitsRight - 1) / 3
                val commasBefore = totalCommas - commasRight
                return (clamped + commasBefore).coerceIn(0, formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, formatted.length)
                val commasBefore = formatted.take(clamped).count { it == ',' }
                return (clamped - commasBefore).coerceIn(0, raw.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

@Composable
fun CurrencyInputField(
    amount: Long,
    onAmountChange: (Long) -> Unit,
    label: String,
    currencyUnit: String = "TOMAN",
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    testTag: String = "currency_input_field"
) {
    var rawDigits by remember(amount) {
        mutableStateOf(if (amount > 0) amount.toString() else "")
    }

    val unitLabel = if (currencyUnit == "RIAL") "ریال" else "تومان"

    val showPlaceholders = LocalShowPlaceholders.current
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = rawDigits,
            onValueChange = { input ->
                val digits = FormatUtils.toEnglishDigits(input).filter { it.isDigit() }.take(15)
                rawDigits = digits
                val parsed = digits.toLongOrNull() ?: 0L
                onAmountChange(parsed)
            },
            label = { Text(label) },
            placeholder = if (showPlaceholders) {
                { Text("0", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) }
            } else null,
            visualTransformation = remember { ThousandsSeparatorVisualTransformation() },
            textStyle = LocalTextStyle.current.copy(
                textDirection = TextDirection.Ltr,
                textAlign = TextAlign.Right
            ),
            trailingIcon = {
                Text(
                    text = unitLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 12.dp)
                )
            },
            isError = isError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            colors = appOutlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
        if (amount > 0) {
            Text(
                text = FormatUtils.numberToPersianWords(amount) + " " + unitLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        AnimatedVisibility(visible = isError && errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun PersianDateField(
    dateString: String,
    onDateSelected: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    testTag: String = "persian_date_field"
) {
    var showPicker by remember { mutableStateOf(false) }

    val initialJalali = remember(dateString) {
        JalaliDate.parse(dateString) ?: JalaliDate.today()
    }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = dateString.ifEmpty { initialJalali.formatShort() },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = "انتخاب تاریخ",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "باز کردن تقویم",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            shape = RoundedCornerShape(14.dp),
            colors = appOutlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
        // Click overlay to trigger modal
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showPicker = true }
        )
    }

    if (showPicker) {
        PersianDatePickerModal(
            initialDate = initialJalali,
            onDateSelected = { jDate ->
                onDateSelected(jDate.formatShort())
                showPicker = false
            },
            onDismissRequest = { showPicker = false }
        )
    }
}

@Composable
fun DeleteConfirmationDialog(
    show: Boolean,
    title: String = "تأیید حذف رکورد",
    message: String,
    itemDetail: String? = null,
    confirmButtonText: String = "بله، حذف شود",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    if (!itemDetail.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = itemDetail,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(confirmButtonText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                    Text("انصراف")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (status) {
        "در حال مذاکره" -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        "طراحی" -> Color(0xFF38BDF8).copy(alpha = 0.2f) to Color(0xFF0284C7)
        "در حال ساخت" -> Color(0xFFFBBF24).copy(alpha = 0.2f) to Color(0xFFD97706)
        "نصب" -> Color(0xFFA78BFA).copy(alpha = 0.2f) to Color(0xFF7C3AED)
        "تکمیل شده", "وصول شده" -> Color(0xFF34D399).copy(alpha = 0.2f) to Color(0xFF059669)
        "تسویه شده" -> Color(0xFF10B981).copy(alpha = 0.25f) to Color(0xFF047857)
        "برگشت خورده" -> Color(0xFFF87171).copy(alpha = 0.2f) to Color(0xFFDC2626)
        "در جریان وصول" -> Color(0xFF60A5FA).copy(alpha = 0.2f) to Color(0xFF2563EB)
        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) to MaterialTheme.colorScheme.primary
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Surface(
                color = iconColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    title: String,
    message: String,
    icon: ImageVector,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionButtonText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onActionClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(actionButtonText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * استاندارد ۳.۱ سند توافقنامه: نوار ثابت دکمه‌های پایینی (Sticky Bottom Action Bar)
 * - دکمه اصلی در سمت راست با عنوان «ذخیره + نام موجودیت» با عرض دو برابر دکمه انصراف
 * - دکمه انصراف در سمت چپ بدون پس‌زمینه برجسته با نصف عرض دکمه ذخیره
 * - فعال شدن دکمه ذخیره فقط در صورت ایجاد تغییر واقعی در داده‌ها (Dirty State)
 */
@Composable
fun StickyBottomActionBar(
    entityName: String,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    isDirty: Boolean = true,
    isValid: Boolean = true,
    saveButtonText: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            // Save button: Right side, 2/3 width, primary color, disabled if !isDirty or !isValid
            Button(
                onClick = onSave,
                enabled = isDirty && isValid,
                modifier = Modifier
                    .weight(2f)
                    .height(48.dp)
                    .testTag("action_bar_save_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            ) {
                Text(
                    text = saveButtonText ?: "ذخیره $entityName",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Cancel button: Left side, 1/3 width, flat without elevated background
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("action_bar_cancel_btn"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("انصراف", style = MaterialTheme.typography.labelLarge)
            }

        }
    }
}

/**
 * استاندارد ۳.۲ سند توافقنامه: هشدار خروج با تغییرات ذخیره‌نشده (Unsaved Changes Dialog)
 */
@Composable
fun UnsavedChangesDialog(
    show: Boolean = true,
    onSaveAndExit: (() -> Unit)? = null,
    onDiscardAndExit: () -> Unit = {},
    onConfirmDiscard: () -> Unit = onDiscardAndExit,
    onDismiss: () -> Unit
) {
    if (show) {
        val discardAction = {
            onConfirmDiscard()
            onDiscardAndExit()
        }
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "تغییرات ذخیره‌نشده",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    "تغییراتی در فرم اعمال کرده‌اید. آیا بدون ذخیره خارج می‌شوید؟",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // دکمه ادامه ویرایش: به رنگ سبز و نوشته سفید
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("unsaved_continue_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF16A34A),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "ادامه ویرایش",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // دکمه خروج: به رنگ قرمز و نوشته سفید
                    Button(
                        onClick = discardAction,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("unsaved_discard_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "خروج",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = null,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

/**
 * استاندارد ۵.۲ سند اختصاصی: منوی سه‌نقطه عمودی در کارت‌ها (Overflow Menu)
 */
@Composable
fun CardOverflowMenu(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "عملیات بیشتر",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("ویرایش") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                onClick = {
                    expanded = false
                    onEdit()
                }
            )
            DropdownMenuItem(
                text = { Text("حذف", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                onClick = {
                    expanded = false
                    onDelete()
                }
            )
        }
    }
}

/**
 * کامپوننت مشترک و ماژولار دراپ‌داون در سراسر برنامه (Single Source of Truth)
 * پشتیبانی از گزینه‌های جنریک، جستجوی زنده، حالت تایپ مستقیم و استایل یکپارچه متریال ۳
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AppDropdownField(
    value: String,
    onValueChange: (String) -> Unit = {},
    label: String,
    options: List<T>,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    optionLabel: (T) -> String = { it.toString() },
    optionIcon: ((T) -> ImageVector?)? = null,
    isSearchable: Boolean = false,
    searchPlaceholder: String = "جستجو...",
    searchMatcher: ((T, String) -> Boolean)? = null,
    isEditable: Boolean = false,
    leadingIcon: ImageVector? = null,
    placeholder: String = "",
    isError: Boolean = false,
    errorMessage: String? = null,
    testTag: String = "app_dropdown_field"
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val showPlaceholders = LocalShowPlaceholders.current
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val filteredOptions = remember(options, searchQuery, isSearchable) {
        if (!isSearchable || searchQuery.isBlank()) {
            options
        } else {
            val q = searchQuery.trim().lowercase()
            options.filter { opt ->
                if (searchMatcher != null) {
                    searchMatcher(opt, q)
                } else {
                    optionLabel(opt).lowercase().contains(q)
                }
            }
        }
    }

    LaunchedEffect(expanded) {
        if (expanded && isSearchable) {
            kotlinx.coroutines.delay(120)
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                readOnly = !isEditable,
                label = { Text(label) },
                placeholder = if (showPlaceholders && placeholder.isNotEmpty()) {
                    { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)) }
                } else null,
                leadingIcon = if (leadingIcon != null) {
                    { Icon(leadingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                } else null,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                isError = isError,
                shape = RoundedCornerShape(14.dp),
                colors = appOutlinedTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(if (isEditable) MenuAnchorType.PrimaryEditable else MenuAnchorType.PrimaryNotEditable)
                    .testTag(testTag)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                    searchQuery = ""
                },
                focusable = isSearchable,
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                if (isSearchable) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                searchPlaceholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "پاک کردن",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else null,
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(searchFocusRequester)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                }

                if (filteredOptions.isEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "موردی یافت نشد",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {},
                        enabled = false
                    )
                } else {
                    filteredOptions.forEach { opt ->
                        val itemIcon = optionIcon?.invoke(opt)
                        DropdownMenuItem(
                            text = { Text(optionLabel(opt), style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = if (itemIcon != null) {
                                {
                                    Icon(
                                        imageVector = itemIcon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else null,
                            onClick = {
                                onOptionSelected(opt)
                                expanded = false
                                searchQuery = ""
                            }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }
}

/**
 * نسخه اختصاصی AppDropdownField برای لیست‌های ساده متنی (String) با پشتیبانی از آیکون
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDropdownField(
    value: String,
    onValueChange: (String) -> Unit = {},
    label: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    optionIcon: ((String) -> ImageVector?)? = null,
    isSearchable: Boolean = false,
    searchPlaceholder: String = "جستجو...",
    isEditable: Boolean = false,
    leadingIcon: ImageVector? = null,
    placeholder: String = "",
    isError: Boolean = false,
    errorMessage: String? = null,
    testTag: String = "app_dropdown_field"
) {
    AppDropdownField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        options = options,
        optionLabel = { it },
        optionIcon = optionIcon,
        onOptionSelected = onOptionSelected,
        modifier = modifier,
        isSearchable = isSearchable,
        searchPlaceholder = searchPlaceholder,
        searchMatcher = null,
        isEditable = isEditable,
        leadingIcon = leadingIcon,
        placeholder = placeholder,
        isError = isError,
        errorMessage = errorMessage,
        testTag = testTag
    )
}







/**
 * موقعیت‌دهی Popup دقیقاً زیر فیلد (یا بالای آن در صورت کمبود جا).
 * جهت باز شدن فقط یک بار در لحظهٔ باز شدن تعیین می‌شود تا هنگام تایپ/باز شدن کیبورد جابه‌جا نشود.
 */
private class BelowAnchorPositionProvider(
    private val gapPx: Int,
    private val maxHeightPx: Int
) : PopupPositionProvider {
    private var showAbove: Boolean? = null

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val spaceBelow = windowSize.height - anchorBounds.bottom
        val spaceAbove = anchorBounds.top
        val above = showAbove ?: (spaceBelow < maxHeightPx && spaceAbove > spaceBelow)
            .also { showAbove = it }

        val y = if (above) anchorBounds.top - popupContentSize.height - gapPx
        else anchorBounds.bottom + gapPx
        val x = anchorBounds.left.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        return IntOffset(x, y.coerceAtLeast(0))
    }
}

/**
 * کنترل اختصاصی انتخاب مشتری (Popup مستقل زیر فیلد، بدون ExposedDropdownMenu)
 * فیلد اصلی فقط یک دکمه است؛ جستجو، لیست و «افزودن مشتری جدید» داخل Popup هستند.
 */
@Composable
fun CustomerDropdownField(
    selectedCustomerId: String,
    customers: List<CustomerEntity>,
    onCustomerSelected: (CustomerEntity) -> Unit,
    label: String = "مشتری یا کارفرمای مرتبط",
    placeholder: String = "انتخاب مشتری",
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
    testTag: String = "customer_dropdown",
    onAddNewCustomer: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var anchorWidthPx by remember { mutableIntStateOf(0) }
    val showPlaceholders = LocalShowPlaceholders.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current

    val maxPopupHeight = 340.dp

    val selectedCustomer = remember(customers, selectedCustomerId) {
        customers.firstOrNull { it.id == selectedCustomerId }
    }

    val displayValue = remember(selectedCustomer) {
        if (selectedCustomer == null) ""
        else if (selectedCustomer.companyName.isNotBlank()) "${selectedCustomer.name} (${selectedCustomer.companyName})"
        else selectedCustomer.name
    }

    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) {
            customers
        } else {
            val q = searchQuery.trim().lowercase()
            customers.filter { cust ->
                cust.name.lowercase().contains(q) ||
                        cust.companyName.lowercase().contains(q) ||
                        cust.phone.contains(q)
            }
        }
    }

    fun close() {
        expanded = false
        searchQuery = ""
        keyboardController?.hide()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { anchorWidthPx = it.width }
                .testTag(testTag)
        ) {
            OutlinedTextField(
                value = displayValue,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = { Text(label) },
                placeholder = if (showPlaceholders && placeholder.isNotEmpty()) {
                    { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)) }
                } else null,
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    val rotation by animateFloatAsState(
                        targetValue = if (expanded) 180f else 0f,
                        label = "dropdown_arrow_rotation"
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(rotation)
                    )
                },
                isError = isError,
                shape = RoundedCornerShape(14.dp),
                colors = appOutlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            // لایهٔ شفاف کلیک روی فیلد (فیلد خودش فوکوس/کیبورد نمی‌گیرد)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.DropdownList
                    ) {
                        if (expanded) close() else expanded = true
                    }
            )

            if (expanded) {
                val positionProvider = remember {
                    BelowAnchorPositionProvider(
                        gapPx = with(density) { 4.dp.roundToPx() },
                        maxHeightPx = with(density) { maxPopupHeight.roundToPx() }
                    )
                }

                Popup(
                    popupPositionProvider = positionProvider,
                    onDismissRequest = { close() },
                    properties = PopupProperties(
                        focusable = true,
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true
                    )
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,   // قبلاً surfaceContainer بود
                        tonalElevation = 0.dp,                        // قبلاً 3.dp بود
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .width(with(density) { anchorWidthPx.toDp() })
                            .heightIn(max = maxPopupHeight)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // باکس جستجو (ثابت در بالا)
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        "جستجوی نام، نام مجموعه یا تلفن...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = if (searchQuery.isNotEmpty()) {
                                    {
                                        IconButton(
                                            onClick = { searchQuery = "" },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "پاک کردن",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                } else null,
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("${testTag}_search")
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )

                            // لیست قابل اسکرول
                            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                                if (filteredCustomers.isEmpty()) {
                                    item {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "مشتری با این مشخصات یافت نشد",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
                                            onClick = {},
                                            enabled = false
                                        )
                                    }
                                } else {
                                    items(filteredCustomers, key = { it.id }) { cust ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    // راست (RTL): نام مشتری + نام مجموعه با سایز کوچک‌تر
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text(
                                                            text = cust.name,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Normal,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f, fill = false)
                                                        )
                                                        if (cust.companyName.isNotBlank()) {
                                                            Text(
                                                                text = "(${cust.companyName})",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontWeight = FontWeight.Normal,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.weight(1f, fill = false)
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    // چپ (RTL): شماره تلفن با چیدمان LTR
                                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                                        Text(
                                                            text = FormatUtils.formatPhoneNumber(cust.phone),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                onCustomerSelected(cust)
                                                close()
                                            }
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )

                            // افزودن مشتری جدید (فعلاً ظاهری)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "افزودن مشتری جدید",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                onClick = {
                                    close()
                                    onAddNewCustomer?.invoke()
                                },
                                modifier = Modifier.testTag("${testTag}_add_new")
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }
}






/**
 * کنترل انتخاب واحد اقلام فاکتور با استفاده از کامپوننت مشترک AppDropdownField
 */
@Composable
fun UnitSelectorDropdown(
    selectedUnit: String,
    onUnitSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val units = listOf("عدد", "متر", "سانتی‌متر", "متر مربع", "کیلوگرم")
    AppDropdownField(
        value = selectedUnit,
        label = "واحد",
        options = units,
        onOptionSelected = onUnitSelected,
        modifier = modifier,
        testTag = "unit_selector_dropdown"
    )
}


/**
 * شیت فرم استاندارد برنامه. امضا و رفتار (isDirty / onAttemptDismiss / ...) مثل قبل است.
 *
 * بلور، تیرگی و انیمیشن از SheetEffectConfig می‌آیند و نیاز به تنظیم در هر فرم نیست.
 * روش اجرا (ModalBottomSheet یا شیت سفارشی) از SheetEffectConfig.impl خوانده می‌شود
 * و لحظه ساخته شدن شیت ثابت می‌ماند.
 *
 * پیش‌نیاز: SheetEffectHost باید یک‌بار در ریشه برنامه (MainActivity) قرار گرفته باشد.
 * اگر نبود، شیت با Scrim پیش‌فرض M3 (بدون بلور) کار می‌کند.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFormBottomSheet(
    title: String,
    subtitle: String? = null,
    headerAction: (@Composable () -> Unit)? = null,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    isDirty: Boolean = false,
    warnUnsavedChanges: Boolean = true,
    onAttemptDismiss: () -> Unit = onDismissRequest,
    content: @Composable ColumnScope.() -> Unit
) {
    val isInspection = LocalInspectionMode.current
    val controller = LocalSheetEffect.current
    val impl = remember { SheetEffectConfig.impl }

    when {
        isInspection -> PreviewSheet(title, subtitle, headerAction, modifier, content)

        controller != null && impl == SheetImpl.CUSTOM -> CustomSheetPortal(
            controller = controller,
            title = title,
            subtitle = subtitle,
            headerAction = headerAction,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            isDirty = isDirty,
            warnUnsavedChanges = warnUnsavedChanges,
            onAttemptDismiss = onAttemptDismiss,
            content = content
        )

        else -> DialogSheet(
            controller = controller,
            title = title,
            subtitle = subtitle,
            headerAction = headerAction,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            isDirty = isDirty,
            warnUnsavedChanges = warnUnsavedChanges,
            onAttemptDismiss = onAttemptDismiss,
            content = content
        )
    }
}

// =============================================================================
// اجزای مشترک
// =============================================================================

@Composable
internal fun SheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp),
            shape = RoundedCornerShape(2.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        ) {}
    }
}

@Composable
internal fun SheetHeader(
    title: String,
    subtitle: String?,
    headerAction: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (headerAction != null) {
            headerAction()
        }
    }
}

@Composable
internal fun SheetBodyColumn(
    title: String,
    subtitle: String?,
    headerAction: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        SheetHeader(title, subtitle, headerAction)
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

// =============================================================================
// Preview (Layoutlib): رندر مستقیم بدون پنجره
// =============================================================================

@Composable
private fun PreviewSheet(
    title: String,
    subtitle: String?,
    headerAction: (@Composable () -> Unit)?,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            SheetHandle()
            SheetBodyColumn(title, subtitle, headerAction, content = content)
        }
    }
}

// =============================================================================
// روش ۲: شیت سفارشی (محتوا به SheetEffectHost منتقل و آنجا رسم می‌شود)
// =============================================================================

@Composable
private fun CustomSheetPortal(
    controller: SheetEffectController,
    title: String,
    subtitle: String?,
    headerAction: (@Composable () -> Unit)?,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    isDirty: Boolean,
    warnUnsavedChanges: Boolean,
    onAttemptDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val entry = remember { CustomSheetEntry() }

    // به‌روزرسانی اطلاعات در هر recomposition (قبل از DisposableEffect اجرا می‌شود)
    SideEffect {
        entry.title = title
        entry.subtitle = subtitle
        entry.headerAction = headerAction
        entry.modifier = modifier
        entry.isDirty = isDirty
        entry.warnUnsaved = warnUnsavedChanges
        entry.onAttemptDismiss = onAttemptDismiss
        entry.onDismissRequest = onDismissRequest
        entry.content = content
    }

    // Back: همان قفل isDirty (اگر لازم بود onAttemptDismiss صدا زده می‌شود)
    BackHandler(enabled = !entry.closing) {
        controller.requestDismiss(entry)
    }

    DisposableEffect(entry) {
        controller.showCustom(entry)
        onDispose { controller.releaseCustom(entry) }
    }
}

// =============================================================================
// روش ۱: ModalBottomSheet (با بلور و تیرگی همگام از روی offset واقعی شیت)
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogSheet(
    controller: SheetEffectController?,
    title: String,
    subtitle: String?,
    headerAction: (@Composable () -> Unit)?,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    isDirty: Boolean,
    warnUnsavedChanges: Boolean,
    onAttemptDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val currentIsDirty by rememberUpdatedState(isDirty)
    val currentWarnUnsaved by rememberUpdatedState(warnUnsavedChanges)
    val currentOnAttemptDismiss by rememberUpdatedState(onAttemptDismiss)
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { targetValue ->
            if (targetValue == SheetValue.Hidden) {
                if (currentIsDirty && currentWarnUnsaved) {
                    currentOnAttemptDismiss()
                    false // رد تغییر حالت به Hidden جهت جلوگیری از بسته شدن خودکار فرم
                } else {
                    true
                }
            } else {
                true
            }
        }
    )

    var hiddenPx by remember { mutableFloatStateOf(Float.NaN) }
    var expandedPx by remember { mutableFloatStateOf(Float.NaN) }
    var contentHeightPx by remember { mutableFloatStateOf(0f) }
    var handleHeightPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val navBottomPx = WindowInsets.navigationBars.getBottom(density).toFloat()

    if (controller != null) {
        // progress ۰..۱ از روی موقعیت واقعی شیت
        val compute: () -> Float = compute@{
            if (sheetState.currentValue == SheetValue.Expanded &&
                sheetState.targetValue == SheetValue.Expanded
            ) return@compute 1f

            val offset = runCatching { sheetState.requireOffset() }.getOrDefault(Float.NaN)
            if (offset.isNaN()) return@compute 0f

            val hidden = if (hiddenPx.isNaN()) controller.containerPx else hiddenPx
            // تا قبل از رسیدن شیت به حالت باز، موقعیت باز تخمین زده می‌شود
            val expanded = if (!expandedPx.isNaN()) expandedPx
            else hidden - (contentHeightPx + handleHeightPx + navBottomPx)
            val range = hidden - expanded
            if (range <= 1f) 0f else ((hidden - offset) / range).coerceIn(0f, 1f)
        }
        val computeState = rememberUpdatedState(compute)

        // کالیبراسیون: حالت بسته = بیشترین offset دیده‌شده ، حالت باز = offset وقتی Expanded است
        LaunchedEffect(sheetState) {
            launch {
                snapshotFlow {
                    runCatching { sheetState.requireOffset() }.getOrDefault(Float.NaN)
                }.collect { offset ->
                    if (!offset.isNaN() && (hiddenPx.isNaN() || offset > hiddenPx)) hiddenPx = offset
                }
            }
            snapshotFlow {
                if (sheetState.currentValue == SheetValue.Expanded) {
                    runCatching { sheetState.requireOffset() }.getOrDefault(Float.NaN)
                } else {
                    Float.NaN
                }
            }.collect { offset ->
                if (!offset.isNaN()) expandedPx = offset
            }
        }

        DisposableEffect(controller) {
            val token = Any()
            controller.attachDialog(token) { computeState.value() }
            onDispose { controller.releaseDialog(token, computeState.value()) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (currentIsDirty && currentWarnUnsaved) {
                currentOnAttemptDismiss()
            } else {
                currentOnDismissRequest()
            }
        },
        sheetState = sheetState,
        properties = ModalBottomSheetDefaults.properties(
            shouldDismissOnBackPress = !(currentIsDirty && currentWarnUnsaved)
        ),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.background,
        // وقتی SheetEffectHost هست، تیرگی را میزبان از روی offset شیت می‌کشد
        scrimColor = if (controller != null) Color.Transparent else BottomSheetDefaults.ScrimColor,
        dragHandle = {
            SheetHandle(Modifier.onSizeChanged { handleHeightPx = it.height.toFloat() })
        },
        modifier = modifier.fillMaxWidth()
    ) {
        BackHandler(enabled = currentIsDirty && currentWarnUnsaved) {
            currentOnAttemptDismiss()
        }

        SheetBodyColumn(
            title = title,
            subtitle = subtitle,
            headerAction = headerAction,
            modifier = Modifier
                .onSizeChanged { contentHeightPx = it.height.toFloat() }
                .fillMaxWidth()
                .imePadding(),
            content = content
        )
    }
}

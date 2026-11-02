package com.example.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.common.AppCard
import com.example.ui.components.common.AppTopBar
import com.example.ui.theme.MyApplicationTheme

/**
 * متدها و الگوهای پیاده‌سازی تیرگی و بلور
 */
enum class BlurDarknessMethod(val title: String, val description: String) {
    CUSTOM_SCRIM(
        title = "روش ۱: Scrim سفارشی (لایه ریشه تمام‌صفحه)",
        description = "افکت Fade In/Out یکپارچه از طریق لایه Overlay مستقل روی کل صفحه از لبه Status Bar تا پایین"
    ),
    DIRECT_DIM(
        title = "روش ۲: Dim مستقیم (روی کل کانتینر صفحه)",
        description = "ترسیم مستقیم تیرگی روی کل صفحه همراه با فیلتر بلور بدون لایه Overlay مجزا"
    )
}

/**
 * صفحه آزمایشی مستقل جهت بررسی و مقایسه جلوه بلور (Blur) و تیرگی همزمان (Fade In / Fade Out)
 * به دور از هرگونه حرکت جهتی از پایین به بالا.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlurTestSettingsScreen(
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    // وضعیت نمایش شیت
    var showBottomSheet by remember { mutableStateOf(false) }

    // متغیرهای تنظیماتی پایدار (با باز و بسته شدن مودال حفظ می‌شوند)
    var blurRadius by remember { mutableFloatStateOf(6f) } // 0 تا 10 دی‌پی
    var scrimAlpha by remember { mutableFloatStateOf(0.30f) } // 0.0 تا 1.0 (0 تا 100٪)
    var animationDurationMs by remember { mutableIntStateOf(300) } // 100 تا 800 میلی‌ثانیه
    var selectedMethod by remember { mutableStateOf(BlurDarknessMethod.CUSTOM_SCRIM) }

    // متغیرهای فرم فرضی زیرین جهت آزمایش جلوه شیشه‌ای روی انواع کامپوننت‌ها
    var customerName by remember { mutableStateOf("حاج رضا کریمی (طلافروشی درخشان)") }
    var orderTitle by remember { mutableStateOf("تابلو حروف برجسته چلنیوم کریستال پانچی") }
    var orderAmount by remember { mutableStateOf("85,000,000") }
    var orderDescription by remember {
        mutableStateOf("لبه چلنیوم مشکی طلایی ۰.۴، پلکسی دوبل شفاف ۳ میل، نورپردازی اس‌ام‌دی بلوکی با ریموت کنترل دیجیتال.")
    }
    var sendSmsEnabled by remember { mutableStateOf(true) }
    var officialInvoice by remember { mutableStateOf(false) }
    var warrantyCertificate by remember { mutableStateOf(true) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // انیمیشن ترنزیشن واحد و مشترک برای بلور و تیرگی (محو شدن و نمایان شدن ایستا بدون جهت)
    val transitionProgress by animateFloatAsState(
        targetValue = if (showBottomSheet) 1f else 0f,
        animationSpec = tween(
            durationMillis = animationDurationMs,
            easing = FastOutSlowInEasing
        ),
        label = "blur_and_darkness_fade"
    )

    val currentBlur = (blurRadius * transitionProgress).dp
    val currentDarkness = (scrimAlpha * transitionProgress).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // =========================================================================
        // لایه کانتینر اصلی تمام‌صفحه (پوشش ۱۰۰٪ از بالای Status Bar تا پایین صفحه)
        // =========================================================================
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            modifier = Modifier
                .fillMaxSize()
                // اعمال بلور سراسری همزمان روی کل صفحه
                .then(
                    if (currentBlur > 0.dp) {
                        Modifier.blur(currentBlur)
                    } else {
                        Modifier
                    }
                )
                // در روش ۲: اعمال مستقیم تیرگی (Dim) روی کل کانتینر صفحه
                .then(
                    if (selectedMethod == BlurDarknessMethod.DIRECT_DIM && currentDarkness > 0f) {
                        Modifier.drawWithContent {
                            drawContent()
                            drawRect(Color.Black.copy(alpha = currentDarkness))
                        }
                    } else {
                        Modifier
                    }
                )
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // هدر تمام‌عرض در بالاترین نقطه صفحه (زیر نوار وضعیت)
                AppTopBar(
                    title = "تست افکت بلور و تیرگی یکپارچه",
                    onBack = onBack
                )

                // بدنه اسکرول‌پذیر فرم فرضی
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // کارت راهنما و وضعیت زنده
                    AppCard {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.BlurOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "فرم کارگاهی (لایه زیرین تمام‌صفحه)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "بلور: ${blurRadius.toInt()}dp | تیرگی: ${(scrimAlpha * 100).toInt()}% | زمان: ${animationDurationMs}ms",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Text(
                                text = "روش فعال: ${selectedMethod.title}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            Text(
                                text = "کل صفحه از بالاترین نقطه نوار وضعیت تا انتهای صفحه، بدون هیچ موج یا حرکت بالا به پایین، همزمان Fade In/Out می‌شود.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // دکمه باز کردن مودال تست
                    Button(
                        onClick = { showBottomSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("open_blur_modal_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "باز کردن مودال تنظیمات و تست افکت Fade",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // کنترل‌های ورودی فرضی
                    Text(
                        text = "اطلاعات سفارش و کارفرما",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    AppCard {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { customerName = it },
                                label = { Text("نام مشتری / کارفرما") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = orderTitle,
                                onValueChange = { orderTitle = it },
                                label = { Text("عنوان تابلو و سفارش") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = orderAmount,
                                onValueChange = { orderAmount = it },
                                label = { Text("مبلغ قرارداد (تومان)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = orderDescription,
                                onValueChange = { orderDescription = it },
                                label = { Text("توضیحات و مشخصات متریال") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )
                        }
                    }

                    // گزینه‌های اجرایی
                    Text(
                        text = "تنظیمات اجرایی و کارگاهی",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    AppCard {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("ارسال پیامک اطلاع‌رسانی وضعیت", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("پیامک پیشرفت مراحل به شماره همراه مشتری", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = sendSmsEnabled,
                                    onCheckedChange = { sendSmsEnabled = it }
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("صدور برگه گارانتی ۳۶ ماهه", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("تضمین قطعات الکترونیکی اس‌ام‌دی و ترانس", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = warrantyCertificate,
                                    onCheckedChange = { warrantyCertificate = it }
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("صدور فاکتور رسمی مالیاتی", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("محاسبه ارزش افزوده به مبلغ نهایی فاکتور", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = officialInvoice,
                                    onCheckedChange = { officialInvoice = it }
                                )
                            }
                        }
                    }

                    // کارت خلاصه مالی
                    AppCard {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "خلاصه حساب پروژه نمونه",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("مبلغ کل فاکتور", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("۸۵,۰۰۰,۰۰۰ تومان", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("مانده بدهی مشتری", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("۴۰,۰۰۰,۰۰۰ تومان", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }

        // =========================================================================
        // در روش ۱: لایه ریشه Scrim سفارشی تمام‌صفحه (Fade In / Fade Out کامل)
        // =========================================================================
        if (selectedMethod == BlurDarknessMethod.CUSTOM_SCRIM && currentDarkness > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = currentDarkness))
            )
        }
    }

    // =========================================================================
    // کامپوننت ModalBottomSheet با ارتفاع حداکثر ۵۰٪ صفحه
    // بدون Scrim پیش‌فرض (scrimColor = Color.Transparent) جهت رفع گسترش جهت‌دار
    // =========================================================================
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            // کلیدی: غیرفعال‌سازی Scrim حرکتی داخلی شیت
            scrimColor = Color.Transparent,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val maxSheetHeight = maxHeight * 0.50f
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxSheetHeight)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // هدر فشرده مودال
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = "تنظیمات تیرگی و بلور سراسری",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { showBottomSheet = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "بستن", modifier = Modifier.size(18.dp))
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // بخش رادیوباتن: انتخاب روش پیاده‌سازی
                    Text(
                        text = "انتخاب روش تیرگی و بلور:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            BlurDarknessMethod.values().forEach { method ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = (selectedMethod == method),
                                            onClick = { selectedMethod = method },
                                            role = Role.RadioButton
                                        )
                                        .padding(vertical = 4.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (selectedMethod == method),
                                        onClick = null, // کنترل توسط ردیف کلیک‌پذیر
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = method.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (selectedMethod == method) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Text(
                                            text = method.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // اسلایدر ۱: شعاع تاری (حداکثر ۱۰ دی‌پی)
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "شعاع بلور کل صفحه (حداکثر ۱۰):",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${blurRadius.toInt()} dp",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Slider(
                            value = blurRadius,
                            onValueChange = { blurRadius = it },
                            valueRange = 0f..10f,
                            steps = 10,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .testTag("slider_blur_radius")
                        )
                    }

                    // اسلایدر ۲: تیرگی سراسری صفحه (بازه تا ۱۰۰٪)
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تیرگی سراسری کل صفحه (بازه ۰ تا ۱۰۰٪):",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${(scrimAlpha * 100).toInt()}%",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        Slider(
                            value = scrimAlpha,
                            onValueChange = { scrimAlpha = it },
                            valueRange = 0f..1f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .testTag("slider_scrim_alpha")
                        )
                    }

                    // اسلایدر ۳: مدت زمان انیمیشن Fade (100 تا 800 میلی‌ثانیه)
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مدت زمان انیمیشن Fade:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "$animationDurationMs ms",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }

                        Slider(
                            value = animationDurationMs.toFloat(),
                            onValueChange = { animationDurationMs = it.toInt() },
                            valueRange = 100f..800f,
                            steps = 13,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .testTag("slider_animation_duration")
                        )
                    }

                    // پیش‌تنظیم‌های سریع (بلور ۴، ۶، ۸)
                    Text(
                        text = "پیش‌تنظیم‌های سریع:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = blurRadius == 4f && ((scrimAlpha * 100).toInt() == 20),
                            onClick = {
                                blurRadius = 4f
                                scrimAlpha = 0.20f
                                animationDurationMs = 250
                            },
                            label = { Text("بلور ۴ (۲۰٪)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = blurRadius == 6f && ((scrimAlpha * 100).toInt() == 30),
                            onClick = {
                                blurRadius = 6f
                                scrimAlpha = 0.30f
                                animationDurationMs = 300
                            },
                            label = { Text("بلور ۶ (۳۰٪)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = blurRadius == 8f && ((scrimAlpha * 100).toInt() == 45),
                            onClick = {
                                blurRadius = 8f
                                scrimAlpha = 0.45f
                                animationDurationMs = 400
                            },
                            label = { Text("بلور ۸ (۴۵٪)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // دکمه تأیید و بستن
                    Button(
                        onClick = { showBottomSheet = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("بستن مودال و تست Fade Out", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Preview(name = "صفحه تست بلور و تیرگی - حالت فرم عادی", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
fun BlurTestSettingsScreenPreview() {
    MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            BlurTestSettingsScreen(onBack = {})
        }
    }
}

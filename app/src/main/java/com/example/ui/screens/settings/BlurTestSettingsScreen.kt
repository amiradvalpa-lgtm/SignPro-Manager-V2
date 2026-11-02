package com.example.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.common.AppCard
import com.example.ui.components.common.AppTopBar
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

/**
 * صفحه آزمایشی استاندارد جهت ارزیابی جلوه شیشه‌ای (بلور) و تیرگی (Scrim) بومی متریال ۳.
 * ویژگی‌های کلیدی این نسخه استاندارد:
 * ۱. استفاده از Scrim استاندارد و بهینه‌شده متریال ۳ بدون لایه‌های دستی و پیچیده.
 * ۲. پوشش ۱۰۰٪ تمام‌صفحه حتی Status Bar با تنظیم windowInsets = WindowInsets(0, 0, 0, 0).
 * ۳. انیمیشن کاملاً همزمان و موازی در باز و بسته شدن (حذف کامل تاخیر ۲ مرحله‌ای).
 * ۴. قابلیت قفل کشیدن به پایین (Swipe Down Lock) بدون اخلال در دکمه Back و لمس Scrim بیرون.
 * ۵. ماندگاری پایدار کلیه تنظیمات هنگام باز و بسته شدن مودال.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlurTestSettingsScreen(
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // وضعیت نمایش شیت و وضعیت فعال بودن افکت بلور
    var showBottomSheet by remember { mutableStateOf(false) }
    var isSheetActive by remember { mutableStateOf(false) }
    var allowDismiss by remember { mutableStateOf(false) }

    // تنظیمات پایدار مودال (در State صفحه ذخیره می‌شوند)
    var blurRadius by remember { mutableFloatStateOf(6f) } // شعاع تاری 0 تا 10
    var scrimAlpha by remember { mutableFloatStateOf(0.32f) } // تیرگی Scrim متریال ۳
    var animationDurationMs by remember { mutableIntStateOf(300) } // زمان انیمیشن همزمان
    var disableDragDismiss by remember { mutableStateOf(true) } // قفل بستن با کشیدن به پایین

    // متغیرهای فرم فرضی زیرین
    var customerName by remember { mutableStateOf("حاج رضا کریمی (طلافروشی درخشان)") }
    var orderTitle by remember { mutableStateOf("تابلو حروف برجسته چلنیوم کریستال پانچی") }
    var orderAmount by remember { mutableStateOf("85,000,000") }
    var orderDescription by remember {
        mutableStateOf("لبه چلنیوم مشکی طلایی ۰.۴، پلکسی دوبل شفاف ۳ میل، نورپردازی اس‌ام‌دی بلوکی با ریموت کنترل دیجیتال.")
    }
    var sendSmsEnabled by remember { mutableStateOf(true) }
    var officialInvoice by remember { mutableStateOf(false) }
    var warrantyCertificate by remember { mutableStateOf(true) }

    // وضعیت شیت همراه با قفل اختیاری درگ به پایین
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { targetValue ->
            if (targetValue == SheetValue.Hidden) {
                if (disableDragDismiss) {
                    // در حالت فعال بودن قفل، فقط اگر دستور خروج مجاز باشد اجازه بسته شدن داده می‌شود
                    allowDismiss
                } else {
                    true
                }
            } else {
                true
            }
        }
    )

    // تابع همزمان برای بستن نرم شیت و فید اوت همزمان بلور (بدون تاخیر ۲ مرحله‌ای)
    val dismissSheet: () -> Unit = {
        isSheetActive = false // بلافاصله در همان میلی‌ثانیه اول افکت بلور شروع به محو شدن می‌کند
        coroutineScope.launch {
            allowDismiss = true
            sheetState.hide()
        }.invokeOnCompletion {
            showBottomSheet = false
            allowDismiss = false
        }
    }

    // تابع باز کردن شیت
    val openSheet: () -> Unit = {
        showBottomSheet = true
        isSheetActive = true
    }

    // مدیریت دکمه Back فیزیکی اندروید
    BackHandler(enabled = showBottomSheet) {
        dismissSheet()
    }
    BackHandler(enabled = !showBottomSheet, onBack = onBack)

    // انیمیشن بلور همگام با مدت زمان انتخابی
    val animatedBlurProgress by animateFloatAsState(
        targetValue = if (isSheetActive) 1f else 0f,
        animationSpec = tween(
            durationMillis = animationDurationMs,
            easing = FastOutSlowInEasing
        ),
        label = "blur_synchronized_animation"
    )

    val currentBlur = (blurRadius * animatedBlurProgress).dp

    // =========================================================================
    // لایه اصلی فرم زیرین (تمام‌صفحه با اعمال بلور)
    // =========================================================================
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (currentBlur > 0.dp) {
                    Modifier.blur(currentBlur)
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
            // نوار بالای صفحه (زیر نوار وضعیت)
            AppTopBar(
                title = "تست هماهنگ بلور و Scrim متریال ۳",
                onBack = onBack
            )

            // محتوای فرم نمونه کارگاهی
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // کارت وضعیت و راهنمای مکانیزم
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
                                    text = "بلور: ${blurRadius.toInt()}dp | Scrim: ${(scrimAlpha * 100).toInt()}% | زمان: ${animationDurationMs}ms",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            text = "قفل کشیدن شیت: ${if (disableDragDismiss) "فعال (فقط دکمه و لمس خارج)" else "غیرفعال (بستن با درگ مجاز است)"}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (disableDragDismiss) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )

                        Text(
                            text = "لایه Scrim مستقیماً توسط متریال ۳ از بالای Status Bar تا پایین صفحه کشیده شده و بلور همزمان با آن باز و بسته می‌شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // دکمه باز کردن مودال
                Button(
                    onClick = openSheet,
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
                        text = "باز کردن مودال تنظیمات و تست انیمیشن همزمان",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // فیلدهای ورودی فرضی
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

                // گزینه‌های اجرایی فرضی
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
    // کامپوننت ModalBottomSheet استاندارد متریال ۳
    // با پوشش سراسری Status Bar و انیمیشن همزمان Scrim و Blur
    // =========================================================================
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = dismissSheet,
            sheetState = sheetState,
            properties = ModalBottomSheetDefaults.properties(
                shouldDismissOnBackPress = false
            ),
            // استفاده از Scrim استاندارد و بهینه متریال ۳
            scrimColor = Color.Black.copy(alpha = scrimAlpha.coerceIn(0f, 1f)),
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
                                text = "تنظیمات تیرگی، بلور و رفتار شیت",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = dismissSheet,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "بستن", modifier = Modifier.size(18.dp))
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    // تنظیم درخواستی جدید: تاگل قفل بستن با کشیدن به پایین
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "قفل بستن با کشیدن به پایین",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "بستن فقط با لمس بیرون، دکمه بستن یا کلید Back",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = disableDragDismiss,
                                onCheckedChange = { disableDragDismiss = it },
                                modifier = Modifier.testTag("switch_disable_drag_dismiss")
                            )
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
                                text = "شعاع بلور فرم زیرین (حداکثر ۱۰):",
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

                    // اسلایدر ۲: تیرگی لایه Scrim متریال ۳ (بازه تا ۱۰۰٪)
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تیرگی Scrim متریال ۳ (پوشش کامل):",
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

                    // اسلایدر ۳: مدت زمان انیمیشن همگام (100 تا 800 میلی‌ثانیه)
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مدت زمان انیمیشن هماهنگ:",
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
                            selected = blurRadius == 6f && ((scrimAlpha * 100).toInt() == 32),
                            onClick = {
                                blurRadius = 6f
                                scrimAlpha = 0.32f
                                animationDurationMs = 300
                            },
                            label = { Text("بلور ۶ (۳۲٪)", fontSize = 11.sp) },
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
                        onClick = dismissSheet,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("بستن شیت (با انیمیشن همزمان)", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Preview(name = "صفحه تست بلور و تیرگی متریال ۳ - حالت فرم عادی", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
fun BlurTestSettingsScreenPreview() {
    MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            BlurTestSettingsScreen(onBack = {})
        }
    }
}

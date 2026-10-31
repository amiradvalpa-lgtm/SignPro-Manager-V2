package com.example.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
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

/**
 * صفحه آزمایشی مستقل جهت بررسی و مقایسه جلوه بلور (Blur) روی بخش‌های مختلف فرم زیرین و تنظیم تاری Scrim.
 * لایه‌بندی سه‌گانه:
 * لایه ۱: خود ModalBottomSheet (ارتفاع حداکثر ۵۰٪ صفحه جهت دیده‌شدن فرم زیرین)
 * لایه ۲: لایه نیمه‌شفاف تیره Scrim با بازه ۰ تا ۱۰۰٪
 * لایه ۳: فرم زیرین با تاگل‌های تفکیک‌شده تاری هدر، بدنه و ناحیه نوار وضعیت (Status Bar)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlurTestSettingsScreen(
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var showBottomSheet by remember { mutableStateOf(false) }
    // مقادیر اولیه منطبق بر پیش‌تنظیم استاندارد درخواستی (بلور ۶، تیرگی ۳۰٪)
    var blurRadius by remember { mutableFloatStateOf(6f) } // بازه 0 تا 10
    var scrimAlpha by remember { mutableFloatStateOf(0.30f) } // بازه 0.0 تا 1.0 (0 تا 100٪)

    // تاگل‌سوییچ‌های مجزا برای کنترل هر بخش از فرم زیرین
    var blurHeaderEnabled by remember { mutableStateOf(true) } // تاری هدر
    var blurBodyEnabled by remember { mutableStateOf(true) } // تاری بدنه
    var blurStatusBarEnabled by remember { mutableStateOf(true) } // تاری ناحیه استاتوس‌بار

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

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // انیمیشن نرم برای شعاع تاری لایه زیرین
    val animatedBlurRadius by animateDpAsState(
        targetValue = if (showBottomSheet) blurRadius.dp else 0.dp,
        animationSpec = tween(durationMillis = 250),
        label = "blur_animation"
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ==========================================
            // لایه ۳: هدر و محدوده Status Bar (با کنترل مستقل بلور)
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (animatedBlurRadius > 0.dp && blurHeaderEnabled) {
                            Modifier.blur(animatedBlurRadius)
                        } else {
                            Modifier
                        }
                    )
            ) {
                AppTopBar(
                    title = "تست افکت بلور (Glassmorphism)",
                    onBack = onBack
                )
            }

            // ==========================================
            // لایه ۳: بدنه محتوای فرم (با کنترل مستقل بلور)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (animatedBlurRadius > 0.dp && blurBodyEnabled) {
                            Modifier.blur(animatedBlurRadius)
                        } else {
                            Modifier
                        }
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // کارت وضعیت و راهنما
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
                                    text = "فرم فرضی کارگاهی (لایه ۳ زیرین)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "بلور: ${blurRadius.toInt()}dp | تیرگی: ${(scrimAlpha * 100).toInt()}% | هدر: ${if (blurHeaderEnabled) "تار" else "واضح"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            text = "مودال زیر حداکثر ۵۰٪ صفحه را می‌گیرد تا نیمی از فرم زیرین برای ارزیابی تاری کاملاً قابل مشاهده باشد.",
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
                        text = "باز کردن مودال نیمه‌صفحه و تنظیمات بلور",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // کنترل‌های فرضی ورودی اطلاعات
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

                // تاگل سوییچ‌ها و گزینه‌های فرضی
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
    // لایه ۱ و لایه ۲: ModalBottomSheet با ارتفاع حداکثر ۵۰٪ صفحه و چیدمان فشرده
    // =========================================================================
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            // لایه ۲: لایه نیمه‌شفاف تیره Scrim با بازه کامل ۰ تا ۱۰۰٪
            scrimColor = Color.Black.copy(alpha = scrimAlpha.coerceIn(0f, 1f)),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = Modifier.fillMaxHeight(0.50f) // ارتفاع حداکثر نصف ارتفاع گوشی
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            text = "تنظیمات بلور و شفافیت (حداکثر ۵۰٪ ارتفاع)",
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

                // تاگل‌های ۳گانه فشرده در ردیف‌های افقی
                Text(
                    text = "بخش‌های تحت تأثیر بلور:",
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
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // تاگل ۱: هدر فرم
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("تار شدن هدر فرم (TopAppBar)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = blurHeaderEnabled,
                                onCheckedChange = { blurHeaderEnabled = it },
                                modifier = Modifier
                                    .scale(0.8f)
                                    .testTag("switch_blur_header")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        // تاگل ۲: بدنه فرم
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("تار شدن بدنه و کادرهای فرم", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = blurBodyEnabled,
                                onCheckedChange = { blurBodyEnabled = it },
                                modifier = Modifier
                                    .scale(0.8f)
                                    .testTag("switch_blur_body")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        // تاگل ۳: پوشش نوار وضعیت
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("امتداد به نوار وضعیت (Status Bar)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = blurStatusBarEnabled,
                                onCheckedChange = { blurStatusBarEnabled = it },
                                modifier = Modifier
                                    .scale(0.8f)
                                    .testTag("switch_blur_statusbar")
                            )
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
                            text = "شعاع بلور لایه زیرین (حداکثر ۱۰):",
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

                // اسلایدر ۲: تیرگی لایه Scrim (بازه تا ۱۰۰٪)
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تیرگی لایه Scrim (بازه ۰ تا ۱۰۰٪):",
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

                // پیش‌تنظیم‌های مقادیر ۴، ۶، ۸
                Text(
                    text = "پیش‌تنظیم‌های سریع (بلور ۴، ۶، ۸):",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = blurRadius == 4f,
                        onClick = {
                            blurRadius = 4f
                            scrimAlpha = 0.20f
                        },
                        label = { Text("بلور ۴ (۲۰٪)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = blurRadius == 6f && ((scrimAlpha * 100).toInt() == 30),
                        onClick = {
                            blurRadius = 6f
                            scrimAlpha = 0.30f // دقیقاً طبق دستور: بلور ۶ و تیرگی ۳۰
                        },
                        label = { Text("بلور ۶ (۳۰٪)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = blurRadius == 8f,
                        onClick = {
                            blurRadius = 8f
                            scrimAlpha = 0.45f
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
                    Text("بستن مودال", fontSize = 13.sp)
                }
            }
        }
    }
}

// تابع کمکی مقیاس‌دهی سوییچ‌ها
@Composable
private fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.size((scale * 51).dp, (scale * 31).dp)
)

@Preview(name = "صفحه تست بلور - حالت فرم عادی", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
fun BlurTestSettingsScreenPreview() {
    MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            BlurTestSettingsScreen(onBack = {})
        }
    }
}

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
 * لایه ۱: خود ModalBottomSheet (بدون تغییر)
 * لایه ۲: لایه نیمه‌شفاف تیره Scrim
 * لایه ۳: فرم زیرین با تاگل‌های تفکیک‌شده تاری هدر، بدنه و ناحیه نوار وضعیت (Status Bar)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlurTestSettingsScreen(
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var showBottomSheet by remember { mutableStateOf(false) }
    var blurRadius by remember { mutableFloatStateOf(16f) } // شعاع تاری به dp (پیش‌فرض 16)
    var scrimAlpha by remember { mutableFloatStateOf(0.40f) } // درصد تیرگی Scrim (پیش‌فرض 40%)

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
        animationSpec = tween(durationMillis = 280),
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
                        if (animatedBlurRadius > 0.dp && ((blurHeaderEnabled && blurStatusBarEnabled) || blurHeaderEnabled)) {
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
                                    text = "هدر: ${if (blurHeaderEnabled) "تار" else "واضح"} | بدنه: ${if (blurBodyEnabled) "تار" else "واضح"} (${blurRadius.toInt()}dp)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            text = "با باز کردن مودال زیر، می‌توانید با تاگل‌های اختصاصی، تار شدن هدر، بدنه یا کل صفحه را به دلخواه مقایسه کنید.",
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
                        .height(52.dp)
                        .testTag("open_blur_modal_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "باز کردن مودال و تاگل‌های مقایسه بلور",
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
    // لایه ۱ و لایه ۲: ModalBottomSheet به همراه Scrim و تاگل‌های مقایسه زنده
    // =========================================================================
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            // لایه ۲: لایه نیمه‌شفاف تیره Scrim با آلفای قابل تنظیم توسط کاربر
            scrimColor = Color.Black.copy(alpha = scrimAlpha),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // هدر مودال
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "تنظیمات لایه‌ها و تفکیک بلور",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تاگل‌های زیر را جهت مقایسه زنده روشن/خاموش کنید",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = { showBottomSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // بخش ۱: تاگل‌های تفکیک اعمال بلور بر بخش‌های مختلف فرم زیرین
                Text(
                    text = "۱. انتخاب بخش‌های تحت تأثیر بلور (تست زنده):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // تاگل ۱: هدر فرم
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    Icons.Default.ViewHeadline,
                                    contentDescription = null,
                                    tint = if (blurHeaderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text("تار شدن هدر فرم (TopAppBar)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("تار شدن عنوان بالای صفحه، دکمه بازگشت و خط افقی", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = blurHeaderEnabled,
                                onCheckedChange = { blurHeaderEnabled = it },
                                modifier = Modifier.testTag("switch_blur_header")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // تاگل ۲: بدنه فرم
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    Icons.Default.ViewAgenda,
                                    contentDescription = null,
                                    tint = if (blurBodyEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text("تار شدن بدنه و کادرهای فرم", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("تار شدن کارت‌ها، فیلدهای ورودی متن و سوییچ‌ها", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = blurBodyEnabled,
                                onCheckedChange = { blurBodyEnabled = it },
                                modifier = Modifier.testTag("switch_blur_body")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // تاگل ۳: پوشش نوار وضعیت سیستم
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    Icons.Default.VerticalAlignTop,
                                    contentDescription = null,
                                    tint = if (blurStatusBarEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text("امتداد به محدوده نوار وضعیت (Status Bar)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("پوشش یکپارچه لبه بالایی زیر ساعت و باتری گوشی", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = blurStatusBarEnabled,
                                onCheckedChange = { blurStatusBarEnabled = it },
                                modifier = Modifier.testTag("switch_blur_statusbar")
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // بخش ۲: تنظیم شعاع بلور لایه زیرین
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "۲. شعاع تاری لایه زیرین (Blur Radius):",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${blurRadius.toInt()} dp",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Slider(
                        value = blurRadius,
                        onValueChange = { blurRadius = it },
                        valueRange = 0f..30f,
                        steps = 30,
                        modifier = Modifier.fillMaxWidth().testTag("slider_blur_radius")
                    )

                    Text(
                        text = "مقادیر ۱۲ تا ۲۰ دی‌پی حالتی مات، مدرن و شبیه شیشه (Frosted Glass) ایجاد می‌کنند.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // بخش ۳: تنظیم میزان تیرگی لایه Scrim
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "۳. تیرگی لایه میانی (Scrim Opacity):",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${(scrimAlpha * 100).toInt()}%",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Slider(
                        value = scrimAlpha,
                        onValueChange = { scrimAlpha = it },
                        valueRange = 0.05f..0.85f,
                        modifier = Modifier.fillMaxWidth().testTag("slider_scrim_alpha")
                    )

                    Text(
                        text = "تیرگی ۳۵٪ تا ۵۰٪ بهترین تعادل میان تفکیک مودال و زیبایی لایه زیرین را فراهم می‌سازد.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // پیش‌تنظیم‌های ترکیبی سریع
                Text(
                    text = "پیش‌تنظیم‌های ترکیبی:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = blurHeaderEnabled && blurBodyEnabled && blurRadius == 16f,
                        onClick = {
                            blurHeaderEnabled = true
                            blurBodyEnabled = true
                            blurStatusBarEnabled = true
                            blurRadius = 16f
                            scrimAlpha = 0.40f
                        },
                        label = { Text("کل صفحه شیشه‌ای", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = !blurHeaderEnabled && blurBodyEnabled && blurRadius == 16f,
                        onClick = {
                            blurHeaderEnabled = false
                            blurBodyEnabled = true
                            blurStatusBarEnabled = false
                            blurRadius = 16f
                            scrimAlpha = 0.40f
                        },
                        label = { Text("فقط بدنه (هدر واضح)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = blurRadius == 0f,
                        onClick = {
                            blurHeaderEnabled = false
                            blurBodyEnabled = false
                            blurRadius = 0f
                            scrimAlpha = 0.32f
                        },
                        label = { Text("عادی بدون بلور", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // دکمه تأیید و بستن
                Button(
                    onClick = { showBottomSheet = false },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("بستن مودال و بازگشت به فرم")
                }
            }
        }
    }
}

@Preview(name = "صفحه تست بلور - حالت فرم عادی", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
fun BlurTestSettingsScreenPreview() {
    MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            BlurTestSettingsScreen(onBack = {})
        }
    }
}

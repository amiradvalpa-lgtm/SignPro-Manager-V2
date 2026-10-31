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
 * صفحه آزمایشی مستقل جهت بررسی و مقایسه جلوه بلور (Blur) روی فرم زیرین و تنظیم تاری Scrim.
 * ساختار سه‌لایه‌ای:
 * لایه ۱: خود ModalBottomSheet (بدون تغییر)
 * لایه ۲: لایه نیمه‌شفاف تیره Scrim
 * لایه ۳: فرم زیرین (اعمال افکت Modifier.blur)
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
    var selectedChannelType by remember { mutableIntStateOf(1) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // انیمیشن نرم برای شعاع تاری لایه زیرین
    val animatedBlurRadius by animateDpAsState(
        targetValue = if (showBottomSheet) blurRadius.dp else 0.dp,
        animationSpec = tween(durationMillis = 280),
        label = "blur_animation"
    )

    Scaffold(
        topBar = {
            AppTopBar(
                title = "تست افکت بلور (Glassmorphism)",
                onBack = onBack
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ==========================================
            // لایه ۳: فرم زیرین (اعمال فیلتر Blur)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (animatedBlurRadius > 0.dp) {
                            Modifier.blur(animatedBlurRadius)
                        } else {
                            Modifier
                        }
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // کارت راهنما و وضعیت لایه زیرین
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
                                    text = "با باز شدن مودال، این فرم به مقدار ${blurRadius.toInt()}dp تار می‌شود.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "این صفحه جهت ارزیابی زیبایی و خوانایی افکت شیشه‌ای تار (Glassmorphism) به جای Scrim کدر طراحی شده است.",
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
                        text = "باز کردن مودال و تنظیم افکت بلور",
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

                // کارت خلاصه مالی (جهت ارزیابی وضوح المان‌های رنگی زیر بلور)
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
    // لایه ۱ و لایه ۲: ModalBottomSheet به همراه Scrim سفارشی
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
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp),
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
                                text = "تنظیمات لایه‌های بلور و شفافیت",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "لایه ۱: مودال | لایه ۲: Scrim | لایه ۳: فرم زیرین",
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

                // کنترل ۱: تنظیم شعاع بلور لایه زیرین
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "شعاع تاری لایه زیرین (Blur Radius):",
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

                // کنترل ۲: تنظیم میزان تیرگی لایه Scrim
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تیرگی لایه میانی (Scrim Opacity):",
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

                // پیش‌تنظیم‌های آماده جهت مقایسه سریع
                Text(
                    text = "پیش‌تنظیم‌های سریع جهت مقایسه:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = blurRadius == 0f && (scrimAlpha in 0.30f..0.35f),
                        onClick = {
                            blurRadius = 0f
                            scrimAlpha = 0.32f
                        },
                        label = { Text("متریال عادی (بدون بلور)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = blurRadius == 16f && (scrimAlpha in 0.38f..0.42f),
                        onClick = {
                            blurRadius = 16f
                            scrimAlpha = 0.40f
                        },
                        label = { Text("شیشه‌ای استاندارد", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = blurRadius == 24f && (scrimAlpha in 0.53f..0.57f),
                        onClick = {
                            blurRadius = 24f
                            scrimAlpha = 0.55f
                        },
                        label = { Text("تار عمیق (Deep)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // دکمه تأیید و بستن
                Button(
                    onClick = { showBottomSheet = false },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("بستن مودال و بازگشت به فرم عادی")
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

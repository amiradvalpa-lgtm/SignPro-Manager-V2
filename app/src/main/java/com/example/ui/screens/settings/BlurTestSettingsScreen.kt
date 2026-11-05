package com.example.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.common.AppCard
import com.example.ui.components.common.AppFormBottomSheet
import com.example.ui.components.common.AppTopBar
import com.example.ui.components.common.SheetEffectConfig
import com.example.ui.components.common.SheetImpl
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * صفحه تنظیمات افکت مودال (تیرگی، بلور، مدت انیمیشن و روش پیاده‌سازی).
 *
 * - تمام تنظیمات در SheetEffectConfig ذخیره می‌شوند و روی همه مودال‌های برنامه اثر دارند.
 * - مودال تنظیمات خودش هم یک AppFormBottomSheet است؛ پس همان روش و افکت انتخابی را نشان می‌دهد.
 * - تغییرات بلور، تیرگی و مدت، همان لحظه اعمال می‌شود. تغییر «روش» مودال را می‌بندد و با روش جدید باز می‌کند.
 */
@Composable
fun BlurTestSettingsScreen(
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showSheet by remember { mutableStateOf(false) }

    // تغییر روش: مودال فعلی بسته و با روش جدید دوباره باز می‌شود
    fun changeImpl(newImpl: SheetImpl) {
        if (newImpl == SheetEffectConfig.impl) return
        SheetEffectConfig.impl = newImpl
        if (showSheet) {
            scope.launch {
                showSheet = false
                delay(SheetEffectConfig.durationMs + 100L)
                showSheet = true
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AppTopBar(
                title = "تنظیمات افکت مودال",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // کارت وضعیت
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
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.BlurOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "افکت مودال‌های برنامه",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "بلور: ${SheetEffectConfig.blurRadiusDp.toInt()}dp | Scrim: ${(SheetEffectConfig.scrimAlpha * 100).toInt()}% | زمان: ${SheetEffectConfig.durationMs}ms",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            text = "روش فعال: ${SheetEffectConfig.impl.label}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "این تنظیمات روی تمام مودال‌های فرم در کل برنامه اعمال می‌شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // دکمه باز کردن مودال تنظیمات
                Button(
                    onClick = { showSheet = true },
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
                        text = "باز کردن مودال تنظیمات و تست انیمیشن",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // مودال تنظیمات؛ همان AppFormBottomSheet برنامه (با روش و افکت سراسری)
    if (showSheet) {
        AppFormBottomSheet(
            title = "تنظیمات افکت مودال",
            subtitle = "روی تمام مودال‌های برنامه اعمال می‌شود",
            onDismissRequest = { showSheet = false }
        ) {
            EffectSettingsContent(
                onSelectImpl = { changeImpl(it) },
                onClose = { showSheet = false }
            )
        }
    }
}

@Composable
private fun EffectSettingsContent(
    onSelectImpl: (SheetImpl) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // انتخاب روش
        Text(
            text = "روش پیاده‌سازی مودال:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SheetImpl.entries.forEach { impl ->
                FilterChip(
                    selected = SheetEffectConfig.impl == impl,
                    onClick = { onSelectImpl(impl) },
                    label = { Text(impl.label, fontSize = 11.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chip_impl_${impl.name}")
                )
            }
        }
        Text(
            text = when (SheetEffectConfig.impl) {
                SheetImpl.DIALOG -> "M3 استاندارد (پنجره جدا). بلور و تیرگی از روی موقعیت واقعی شیت محاسبه می‌شوند."
                SheetImpl.CUSTOM -> "شیت سفارشی در لایه اصلی. شیت، تیرگی و بلور از یک progress واحد می‌آیند."
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // شعاع بلور
        LabeledSlider(
            title = "شعاع بلور (حداکثر ۱۰):",
            valueText = "${SheetEffectConfig.blurRadiusDp.toInt()} dp",
            badgeColor = MaterialTheme.colorScheme.primary,
            value = SheetEffectConfig.blurRadiusDp,
            onValueChange = { SheetEffectConfig.blurRadiusDp = it },
            range = 0f..10f,
            steps = 9,
            tag = "slider_blur_radius"
        )

        // تیرگی
        LabeledSlider(
            title = "تیرگی Scrim:",
            valueText = "${(SheetEffectConfig.scrimAlpha * 100).toInt()}%",
            badgeColor = MaterialTheme.colorScheme.secondary,
            value = SheetEffectConfig.scrimAlpha,
            onValueChange = { SheetEffectConfig.scrimAlpha = it },
            range = 0f..1f,
            steps = 0,
            tag = "slider_scrim_alpha"
        )

        // مدت انیمیشن
        LabeledSlider(
            title = "مدت انیمیشن (فقط روش ۲):",
            valueText = "${SheetEffectConfig.durationMs} ms",
            badgeColor = MaterialTheme.colorScheme.tertiary,
            value = SheetEffectConfig.durationMs.toFloat(),
            onValueChange = { SheetEffectConfig.durationMs = it.toInt() },
            range = 100f..800f,
            steps = 13,
            tag = "slider_animation_duration"
        )

        // پیش‌تنظیم‌ها
        Text(
            text = "پیش‌تنظیم‌های سریع:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PresetChip("بلور ۴ (۲۰٪)", 4f, 0.20f, 250, Modifier.weight(1f))
            PresetChip("بلور ۶ (۳۲٪)", 6f, 0.32f, 300, Modifier.weight(1f))
            PresetChip("بلور ۸ (۴۵٪)", 8f, 0.45f, 400, Modifier.weight(1f))
        }

        Button(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("بستن", fontSize = 13.sp)
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    blur: Float,
    scrim: Float,
    durationMs: Int,
    modifier: Modifier
) {
    FilterChip(
        selected = SheetEffectConfig.blurRadiusDp == blur &&
                (SheetEffectConfig.scrimAlpha * 100).toInt() == (scrim * 100).toInt(),
        onClick = {
            SheetEffectConfig.blurRadiusDp = blur
            SheetEffectConfig.scrimAlpha = scrim
            SheetEffectConfig.durationMs = durationMs
        },
        label = { Text(label, fontSize = 11.sp) },
        modifier = modifier
    )
}

@Composable
private fun LabeledSlider(
    title: String,
    valueText: String,
    badgeColor: Color,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    tag: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Surface(
                color = badgeColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = valueText,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .testTag(tag)
        )
    }
}

@Preview(name = "تنظیمات افکت مودال", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
fun BlurTestSettingsScreenPreview() {
    MyApplicationTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            BlurTestSettingsScreen(onBack = {})
        }
    }
}

package com.example.ui.components.common

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import com.example.data.preferences.AppPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// =============================================================================
// تنظیمات سراسری افکت مودال (در زمان اجرا قابل تغییر؛ از BlurTestSettingsScreen)
// =============================================================================

enum class SheetImpl(val label: String) {
    /** ModalBottomSheet استاندارد M3 (پنجره جدا) با بلور و تیرگی همگام از روی offset */
    DIALOG("۱) ModalBottomSheet"),

    /** شیت سفارشی بدون Dialog که در SheetEffectHost رسم می‌شود */
    CUSTOM("۲) شیت سفارشی")
}

object SheetEffectConfig {
    private var prefs: AppPreferences? = null
    private var boundScope: CoroutineScope? = null
    private var isSyncingFromPrefs = false

    private val _blurRadiusDp = mutableFloatStateOf(6f)
    /** شعاع بلور (dp) ؛ ۰ تا ۱۰ */
    var blurRadiusDp: Float
        get() = _blurRadiusDp.floatValue
        set(value) {
            _blurRadiusDp.floatValue = value
            if (!isSyncingFromPrefs) {
                boundScope?.launch { prefs?.setSheetBlurRadius(value) }
            }
        }

    private val _scrimAlpha = mutableFloatStateOf(0.32f)
    /** تیرگی Scrim ؛ ۰ تا ۱ */
    var scrimAlpha: Float
        get() = _scrimAlpha.floatValue
        set(value) {
            _scrimAlpha.floatValue = value
            if (!isSyncingFromPrefs) {
                boundScope?.launch { prefs?.setSheetScrimAlpha(value) }
            }
        }

    private val _durationMs = mutableIntStateOf(300)
    /** مدت انیمیشن (ms) ؛ فقط در روش ۲ و محو شدن بلور هنگام حذف ناگهانی شیت */
    var durationMs: Int
        get() = _durationMs.intValue
        set(value) {
            _durationMs.intValue = value
            if (!isSyncingFromPrefs) {
                boundScope?.launch { prefs?.setSheetDurationMs(value) }
            }
        }

    private val _impl = mutableStateOf(SheetImpl.CUSTOM)
    /** روش فعال؛ برای شیت‌هایی که از این به بعد باز می‌شوند اعمال می‌شود. */
    var impl: SheetImpl
        get() = _impl.value
        set(value) {
            _impl.value = value
            if (!isSyncingFromPrefs) {
                boundScope?.launch { prefs?.setSheetImpl(value.name) }
            }
        }

    fun bind(preferences: AppPreferences, scope: CoroutineScope) {
        prefs = preferences
        boundScope = scope
        scope.launch {
            preferences.sheetBlurRadius.collect { value ->
                isSyncingFromPrefs = true
                _blurRadiusDp.floatValue = value
                isSyncingFromPrefs = false
            }
        }
        scope.launch {
            preferences.sheetScrimAlpha.collect { value ->
                isSyncingFromPrefs = true
                _scrimAlpha.floatValue = value
                isSyncingFromPrefs = false
            }
        }
        scope.launch {
            preferences.sheetDurationMs.collect { value ->
                isSyncingFromPrefs = true
                _durationMs.intValue = value
                isSyncingFromPrefs = false
            }
        }
        scope.launch {
            preferences.sheetImpl.collect { name ->
                isSyncingFromPrefs = true
                _impl.value = if (name == SheetImpl.DIALOG.name) SheetImpl.DIALOG else SheetImpl.CUSTOM
                isSyncingFromPrefs = false
            }
        }
    }
}

/** سوییچ کوچک برای مقایسه؛ در هر صفحه تنظیمات بگذارید. */
@Composable
fun SheetImplSwitcher(modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        SheetImpl.entries.forEach { impl ->
            FilterChip(
                selected = SheetEffectConfig.impl == impl,
                onClick = { SheetEffectConfig.impl = impl },
                label = { Text(impl.label, fontSize = 12.sp) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// =============================================================================
// Controller
// =============================================================================

/** اطلاعات شیت سفارشی که AppFormBottomSheet به میزبان می‌دهد (portal). */
@Stable
internal class CustomSheetEntry {
    var title by mutableStateOf("")
    var subtitle by mutableStateOf<String?>(null)
    var headerAction by mutableStateOf<(@Composable () -> Unit)?>(null)
    var modifier by mutableStateOf<Modifier>(Modifier)
    var isDirty by mutableStateOf(false)
    var warnUnsaved by mutableStateOf(true)
    var content by mutableStateOf<@Composable ColumnScope.() -> Unit>({})
    var closing by mutableStateOf(false)
    var onAttemptDismiss: () -> Unit = {}
    var onDismissRequest: () -> Unit = {}
}

@Stable
class SheetEffectController internal constructor(private val scope: CoroutineScope) {

    internal var containerPx by mutableFloatStateOf(0f)

    // ---- روش ۱ ----
    private var dialogSource by mutableStateOf<(() -> Float)?>(null)
    private var dialogOwner: Any? = null
    private var tail by mutableFloatStateOf(0f)
    private var tailJob: Job? = null

    // ---- روش ۲ ----
    internal var customEntry by mutableStateOf<CustomSheetEntry?>(null)
        private set
    internal var customProgress by mutableFloatStateOf(0f)
        private set
    internal var customHeightPx by mutableFloatStateOf(0f)
    private var customJob: Job? = null

    /** مقدار ۰..۱ که بلور و تیرگی از آن می‌خوانند (در فاز draw صدا زده می‌شود). */
    fun progress(): Float {
        val source = dialogSource
        return when {
            source != null -> source()
            customEntry != null -> customProgress
            else -> tail
        }
    }

    // ------------------------- روش ۱ -------------------------
    internal fun attachDialog(owner: Any, source: () -> Float) {
        tailJob?.cancel()
        tail = 0f
        dialogOwner = owner
        dialogSource = source
    }

    internal fun releaseDialog(owner: Any, lastValue: Float) {
        if (dialogOwner !== owner) return
        dialogOwner = null
        dialogSource = null
        tail = lastValue
        tailJob?.cancel()
        if (lastValue <= 0.001f) return
        tailJob = scope.launch {
            animate(
                initialValue = lastValue,
                targetValue = 0f,
                animationSpec = tween(SheetEffectConfig.durationMs, easing = FastOutSlowInEasing)
            ) { value, _ -> tail = value }
        }
    }

    // ------------------------- روش ۲ -------------------------
    internal fun showCustom(entry: CustomSheetEntry) {
        customJob?.cancel()
        entry.closing = false
        if (customEntry == null) customProgress = 0f
        customHeightPx = 0f
        customEntry = entry
    }

    internal fun animateCustom(target: Float, onDone: () -> Unit = {}) {
        customJob?.cancel()
        customJob = scope.launch {
            animate(
                initialValue = customProgress,
                targetValue = target,
                animationSpec = tween(SheetEffectConfig.durationMs, easing = FastOutSlowInEasing)
            ) { value, _ -> customProgress = value }
            onDone()
        }
    }

    internal fun moveCustomBy(dyPx: Float) {
        if (customHeightPx <= 0f) return
        customJob?.cancel()
        customProgress = (customProgress - dyPx / customHeightPx).coerceIn(0f, 1f)
    }

    /** بستن با انیمیشن؛ اگر notify باشد بعد از پایان onDismissRequest فراخوانی می‌شود. */
    internal fun closeCustom(entry: CustomSheetEntry, notify: Boolean) {
        if (customEntry !== entry) return
        entry.closing = true
        animateCustom(0f) {
            if (customEntry === entry) customEntry = null
            if (notify) entry.onDismissRequest()
        }
    }

    /** درخواست کاربر برای بستن (Back، لمس بیرون، درگ). قفل isDirty اینجا اعمال می‌شود. */
    internal fun requestDismiss(entry: CustomSheetEntry) {
        if (customEntry !== entry || entry.closing) return
        if (entry.isDirty && entry.warnUnsaved) {
            entry.onAttemptDismiss()
            animateCustom(1f)
        } else {
            closeCustom(entry, notify = true)
        }
    }

    internal fun settleCustom(entry: CustomSheetEntry, velocityY: Float) {
        val close = velocityY > 1200f || (customProgress < 0.5f && velocityY > -1200f)
        if (close) requestDismiss(entry) else animateCustom(1f)
    }

    /** وقتی فرم صدا زننده (مثلاً بعد از ذخیره) شیت را از composition حذف کرد. */
    internal fun releaseCustom(entry: CustomSheetEntry) {
        if (customEntry === entry && !entry.closing) {
            closeCustom(entry, notify = false)
        }
    }
}

val LocalSheetEffect = staticCompositionLocalOf<SheetEffectController?> { null }

// =============================================================================
// Host: یک‌بار در ریشه برنامه (MainActivity) دور AppNavigation
// =============================================================================

@Composable
fun SheetEffectHost(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember { SheetEffectController(scope) }

    LaunchedEffect(Unit) {
        val appPrefs = AppPreferences(context.applicationContext)
        SheetEffectConfig.bind(appPrefs, scope)
    }

    CompositionLocalProvider(LocalSheetEffect provides controller) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { controller.containerPx = it.height.toFloat() }
        ) {
            // لایه ۱: کل برنامه + بلور (در فاز draw)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val r = SheetEffectConfig.blurRadiusDp.dp.toPx() * controller.progress()
                            renderEffect = if (r > 0.5f) BlurEffect(r, r, TileMode.Clamp) else null
                        }
                    }
            ) {
                content()
            }

            // لایه ۲: Scrim تمام‌صفحه (شامل Status Bar و Navigation Bar)
            val entry = controller.customEntry
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val base = SheetEffectConfig.scrimAlpha +
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) 0.15f else 0f
                        val a = (base * controller.progress()).coerceIn(0f, 1f)
                        if (a > 0f) drawRect(Color.Black.copy(alpha = a))
                    }
                    .then(
                        if (entry != null) {
                            Modifier.pointerInput(entry) {
                                detectTapGestures { controller.requestDismiss(entry) }
                            }
                        } else {
                            Modifier
                        }
                    )
            )

            // لایه ۳: شیت سفارشی (روش ۲)
            if (entry != null) {
                CustomSheetLayer(controller, entry)
            }
        }
    }
}

@Composable
private fun CustomSheetLayer(controller: SheetEffectController, entry: CustomSheetEntry) {
    // شروع انیمیشن باز شدن بعد از اندازه‌گیری شیت
    LaunchedEffect(entry) {
        snapshotFlow { controller.customHeightPx }.first { it > 0f }
        controller.animateCustom(1f)
    }

    val nested = remember(controller, entry) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (entry.closing || available.y >= 0f || controller.customProgress >= 1f) return Offset.Zero
                controller.moveCustomBy(available.y)
                return Offset(0f, available.y)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (entry.closing || available.y <= 0f) return Offset.Zero
                controller.moveCustomBy(available.y)
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (entry.closing || controller.customProgress >= 1f) return Velocity.Zero
                controller.settleCustom(entry, available.y)
                return available
            }
        }
    }

    val dragState = rememberDraggableState { delta ->
        if (!entry.closing) controller.moveCustomBy(delta)
    }

    // Box بیرونی: بالا زیر Status Bar و پایین بالای کیبورد
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .then(entry.modifier)
                .fillMaxWidth()
                .onSizeChanged { controller.customHeightPx = it.height.toFloat() }
                .graphicsLayer {
                    translationY = (1f - controller.customProgress) * controller.customHeightPx
                    alpha = if (controller.customHeightPx > 0f) 1f else 0f
                }
                .nestedScroll(nested)
                .draggable(
                    state = dragState,
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity ->
                        if (!entry.closing) controller.settleCustom(entry, velocity)
                    }
                )
                // لمس داخل شیت نباید به Scrim برسد
                .pointerInput(Unit) { detectTapGestures { } },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.navigationBarsPadding()) {
                SheetHandle()
                SheetBodyColumn(
                    title = entry.title,
                    subtitle = entry.subtitle,
                    headerAction = entry.headerAction,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    entry.content(this)
                }
            }
        }
    }
}

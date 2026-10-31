package com.example.ui.components.pickers

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.MyApplicationTheme
import com.example.util.JalaliDate
import kotlinx.coroutines.launch
import java.util.Locale

private enum class CalendarViewMode {
  DAYS,
  MONTHS,
  YEARS,
  KEYBOARD_INPUT
}

private const val BASE_PAGE_INDEX = 1200
private const val TOTAL_PAGES_COUNT = 2400

private fun getYearMonthForPage(page: Int, baseYear: Int, baseMonth: Int): Pair<Int, Int> {
  val diff = page - BASE_PAGE_INDEX
  val totalMonths = (baseYear * 12 + (baseMonth - 1)) + diff
  val year = totalMonths / 12
  val month = (totalMonths % 12) + 1
  return year to month
}

private fun getPageForYearMonth(year: Int, month: Int, baseYear: Int, baseMonth: Int): Int {
  val totalTarget = year * 12 + (month - 1)
  val totalBase = baseYear * 12 + (baseMonth - 1)
  return BASE_PAGE_INDEX + (totalTarget - totalBase)
}

// ---------------------------------------------------------------------------
// Helpers for manual input
// ---------------------------------------------------------------------------

/** Converts Persian / Arabic-Indic digits to Latin digits and keeps ONLY digits. */
private fun dateDigitsOnly(s: String): String = buildString {
  for (c in s) {
    when (c) {
      in '۰'..'۹' -> append('0' + (c - '۰'))
      in '٠'..'٩' -> append('0' + (c - '٠'))
      in '0'..'9' -> append(c)
      else -> Unit
    }
  }
}

private fun datePad2(s: String): String = when {
  s.isEmpty() -> "00"
  s.length == 1 -> "0$s"
  else -> s.takeLast(2)
}

private fun dateSelectAll(text: String) = TextFieldValue(text, TextRange(0, text.length))

private fun dateCaretAtEnd(text: String) = TextFieldValue(text, TextRange(text.length))

private fun addDaysTo(date: JalaliDate, count: Int): JalaliDate {
  var y = date.year
  var m = date.month
  var d = date.day
  repeat(count) {
    d += 1
    if (d > JalaliDate.getDaysInMonth(y, m)) {
      d = 1
      m += 1
      if (m > 12) {
        m = 1
        y += 1
      }
    }
  }
  return JalaliDate(y, m, d)
}

/** 0 = Saturday ... 6 = Friday (same indexing as the calendar grid). */
private fun weekIndexOf(date: JalaliDate): Int =
  (JalaliDate.getFirstDayOfWeekOffset(date.year, date.month) + date.day - 1) % 7

private data class DatePreset(val label: String, val date: JalaliDate)

/**
 * Standalone Card containing the complete Persian Date Picker UI.
 * Can be previewed directly in Android Studio Design mode without WindowManager errors.
 */
@Composable
fun PersianDatePickerCard(
  initialDate: JalaliDate? = null,
  initialKeyboardMode: Boolean = true,
  onDateSelected: (JalaliDate) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier
) {
  val today = remember { JalaliDate.today() }
  val activeInitial = initialDate ?: today

  val coroutineScope = rememberCoroutineScope()
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current

  val initialPage = remember(activeInitial) {
    getPageForYearMonth(activeInitial.year, activeInitial.month, today.year, today.month)
  }

  val pagerState = rememberPagerState(
    initialPage = initialPage,
    pageCount = { TOTAL_PAGES_COUNT }
  )

  val (currentYear, currentMonth) = remember(pagerState.currentPage) {
    getYearMonthForPage(pagerState.currentPage, today.year, today.month)
  }

  var selectedDay by remember { mutableIntStateOf(activeInitial.day) }

  // The modal opens in specified mode (manual or days grid)
  var viewMode by remember {
    mutableStateOf(if (initialKeyboardMode) CalendarViewMode.KEYBOARD_INPUT else CalendarViewMode.DAYS)
  }

  // Date handed to the manual input screen every time it is opened
  var manualInitial by remember { mutableStateOf(activeInitial) }

  Card(
    modifier = modifier
      .animateContentSize()
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
      .widthIn(max = 400.dp)
      .testTag("persian_date_picker_dialog"),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
  ) {
    if (viewMode == CalendarViewMode.KEYBOARD_INPUT) {
      ManualDateContent(
        initial = manualInitial,
        today = today,
        onConfirm = { date ->
          onDateSelected(date)
          onDismissRequest()
        },
        onBackToCalendar = { date ->
          selectedDay = date.day
          val targetPage = getPageForYearMonth(date.year, date.month, today.year, today.month)
          coroutineScope.launch {
            pagerState.scrollToPage(targetPage)
          }
          viewMode = CalendarViewMode.DAYS
        }
      )
    } else {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Dialog Title & Actions
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.CalendarToday,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp)
            )
            Text(
              text = "انتخاب تاریخ شمسی",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            TextButton(
              onClick = {
                selectedDay = today.day
                viewMode = CalendarViewMode.DAYS
                coroutineScope.launch {
                  pagerState.animateScrollToPage(BASE_PAGE_INDEX)
                }
              },
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
            ) {
              Text(
                text = "امروز",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }

            IconButton(
              onClick = {
                keyboardController?.hide()
                focusManager.clearFocus()
                val maxDays = JalaliDate.getDaysInMonth(currentYear, currentMonth)
                manualInitial = JalaliDate(currentYear, currentMonth, selectedDay.coerceIn(1, maxDays))
                viewMode = CalendarViewMode.KEYBOARD_INPUT
              },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = Icons.Default.EditCalendar,
                contentDescription = "ورود دستی با کیبورد",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Header Navigation Bar
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = {
                coroutineScope.launch {
                  pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
              },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                contentDescription = "ماه بعد",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                color = if (viewMode == CalendarViewMode.MONTHS) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .clickable {
                    viewMode = if (viewMode == CalendarViewMode.MONTHS) CalendarViewMode.DAYS else CalendarViewMode.MONTHS
                  }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Text(
                    text = JalaliDate.PERSIAN_MONTHS.getOrElse(currentMonth - 1) { "" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (viewMode == CalendarViewMode.MONTHS) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                  )
                  Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (viewMode == CalendarViewMode.MONTHS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Surface(
                color = if (viewMode == CalendarViewMode.YEARS) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .clickable {
                    viewMode = if (viewMode == CalendarViewMode.YEARS) CalendarViewMode.DAYS else CalendarViewMode.YEARS
                  }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Text(
                    text = "$currentYear",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (viewMode == CalendarViewMode.YEARS) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                  )
                  Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (viewMode == CalendarViewMode.YEARS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }

            IconButton(
              onClick = {
                coroutineScope.launch {
                  pagerState.animateScrollToPage(pagerState.currentPage - 1)
                }
              },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "ماه قبل",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Fixed-Height Container (285dp) ensuring zero height variation
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(285.dp)
        ) {
          AnimatedContent(
            targetState = viewMode,
            transitionSpec = { fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(140)) },
            label = "calendar_view_mode",
            modifier = Modifier.fillMaxSize()
          ) { mode ->
            when (mode) {
              CalendarViewMode.DAYS -> {
                Column(modifier = Modifier.fillMaxSize()) {
                  // Fixed Days of week header
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                  ) {
                    JalaliDate.WEEK_DAYS_PERSIAN.forEachIndexed { index, dayName ->
                      val isFriday = index == 6
                      Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isFriday) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(36.dp)
                      )
                    }
                  }

                  HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 2,
                    modifier = Modifier
                      .fillMaxWidth()
                      .weight(1f)
                  ) { page ->
                    val (pYear, pMonth) = getYearMonthForPage(page, today.year, today.month)
                    MonthPageContent(
                      year = pYear,
                      month = pMonth,
                      selectedDay = selectedDay,
                      currentActiveYear = currentYear,
                      currentActiveMonth = currentMonth,
                      today = today,
                      onDayClick = { day ->
                        selectedDay = day
                      }
                    )
                  }
                }
              }

              CalendarViewMode.MONTHS -> {
                MonthsGridView(
                  currentMonth = currentMonth,
                  onMonthSelected = { m ->
                    val targetPage = getPageForYearMonth(currentYear, m, today.year, today.month)
                    coroutineScope.launch {
                      pagerState.scrollToPage(targetPage)
                    }
                    val maxDays = JalaliDate.getDaysInMonth(currentYear, m)
                    if (selectedDay > maxDays) selectedDay = maxDays
                    viewMode = CalendarViewMode.DAYS
                  }
                )
              }

              CalendarViewMode.YEARS -> {
                YearsGridView(
                  currentYear = currentYear,
                  todayYear = today.year,
                  onYearSelected = { y ->
                    val targetPage = getPageForYearMonth(y, currentMonth, today.year, today.month)
                    coroutineScope.launch {
                      pagerState.scrollToPage(targetPage)
                    }
                    val maxDays = JalaliDate.getDaysInMonth(y, currentMonth)
                    if (selectedDay > maxDays) selectedDay = maxDays
                    viewMode = CalendarViewMode.DAYS
                  }
                )
              }

              // Manual input is rendered as its own screen (see ManualDateContent)
              CalendarViewMode.KEYBOARD_INPUT -> Unit
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Calculation of Date
        val (validDate, validationError) = remember<Pair<JalaliDate?, String?>>(currentYear, currentMonth, selectedDay) {
          val validDays = JalaliDate.getDaysInMonth(currentYear, currentMonth)
          val finalDay = selectedDay.coerceIn(1, validDays)
          JalaliDate(currentYear, currentMonth, finalDay) to null
        }

        Surface(
          color = if (validationError != null) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = if (validationError != null) validationError else "تاریخ انتخابی: ${validDate?.day} ${validDate?.monthName} ${validDate?.year}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (validationError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          OutlinedButton(
            onClick = {
              keyboardController?.hide()
              focusManager.clearFocus()
              onDismissRequest()
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp)
          ) {
            Text("انصراف", style = MaterialTheme.typography.labelLarge)
          }

          Button(
            onClick = {
              keyboardController?.hide()
              focusManager.clearFocus()
              if (validDate != null) {
                onDateSelected(validDate)
                onDismissRequest()
              }
            },
            enabled = validDate != null,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp)
          ) {
            Text("تایید تاریخ", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

/**
 * Modal Wrapper that displays PersianDatePickerCard inside an Android Dialog.
 * Centered in the area ABOVE the keyboard (imePadding); RTL from the very first frame.
 */
@Composable
fun PersianDatePickerModal(
  initialDate: JalaliDate? = null,
  onDateSelected: (JalaliDate) -> Unit,
  onDismissRequest: () -> Unit
) {
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current

  val dismiss: () -> Unit = {
    keyboardController?.hide()
    focusManager.clearFocus()
    onDismissRequest()
  }

  Dialog(
    onDismissRequest = dismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      decorFitsSystemWindows = false
    )
  ) {
    com.example.ui.components.common.SyncModalSystemBars()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .imePadding()
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = dismiss
          ),
        contentAlignment = Alignment.Center
      ) {
        PersianDatePickerCard(
          initialDate = initialDate,
          onDateSelected = onDateSelected,
          onDismissRequest = onDismissRequest,
          // Swallow taps on the card so they don't dismiss the dialog
          modifier = Modifier.pointerInput(Unit) { detectTapGestures { } }
        )
      }
    }
  }
}

// ---------------------------------------------------------------------------
// Manual input screen (same UX principles as the time picker)
// ---------------------------------------------------------------------------

@Composable
private fun ManualDateContent(
  initial: JalaliDate,
  today: JalaliDate,
  onConfirm: (JalaliDate) -> Unit,
  onBackToCalendar: (JalaliDate) -> Unit
) {
  val keyboard = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current

  var yearValue by remember { mutableStateOf(dateCaretAtEnd(initial.year.toString())) }
  var monthValue by remember { mutableStateOf(dateCaretAtEnd(datePad2(initial.month.toString()))) }
  var dayValue by remember { mutableStateOf(dateCaretAtEnd(datePad2(initial.day.toString()))) }
  var lastValidYear by remember { mutableIntStateOf(initial.year) }

  val yearFocus = remember { FocusRequester() }
  val monthFocus = remember { FocusRequester() }
  val dayFocus = remember { FocusRequester() }

  fun effYear(): Int = yearValue.text.takeIf { it.length == 4 }?.toIntOrNull() ?: lastValidYear
  fun effMonth(): Int = (monthValue.text.toIntOrNull() ?: 1).coerceIn(1, 12)
  fun maxDays(): Int = JalaliDate.getDaysInMonth(effYear(), effMonth())
  fun sanitizeMonth(t: String): String = datePad2((t.toIntOrNull() ?: 1).coerceIn(1, 12).toString())
  fun sanitizeDay(t: String): String = datePad2((t.toIntOrNull() ?: 1).coerceIn(1, maxDays()).toString())

  fun clampDay(y: Int, m: Int) {
    if (dayValue.text.length == 2) {
      val dv = dayValue.text.toIntOrNull() ?: return
      val mx = JalaliDate.getDaysInMonth(y, m)
      if (dv > mx) dayValue = dateCaretAtEnd(datePad2(mx.toString()))
    }
  }

  fun resolveDate(): JalaliDate {
    val y = effYear()
    val m = effMonth()
    val d = (dayValue.text.toIntOrNull() ?: 1).coerceIn(1, JalaliDate.getDaysInMonth(y, m))
    return JalaliDate(y, m, d)
  }

  fun goToMonth() {
    monthValue = dateSelectAll(sanitizeMonth(monthValue.text))
    monthFocus.requestFocus()
  }

  fun goToDay() {
    dayValue = dateSelectAll(sanitizeDay(dayValue.text))
    dayFocus.requestFocus()
  }

  val presets = remember(today) {
    listOf(
      DatePreset("امروز", today),
      DatePreset("فردا", addDaysTo(today, 1)),
      DatePreset("پس‌فردا", addDaysTo(today, 2)),
      DatePreset("آخر هفته", addDaysTo(today, (6 - weekIndexOf(today)).coerceAtLeast(0))),
      DatePreset(
        "آخر ماه",
        JalaliDate(today.year, today.month, JalaliDate.getDaysInMonth(today.year, today.month))
      )
    )
  }

  val clockStyle = MaterialTheme.typography.headlineLarge.copy(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.primary,
    textAlign = TextAlign.Center
  )

  val current = resolveDate()

  Column(modifier = Modifier.fillMaxWidth()) {

    // Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.CalendarToday,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(22.dp)
        )
        Text(
          text = "تنظیم تاریخ",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      // Back to calendar
      IconButton(
        onClick = {
          keyboard?.hide()
          focusManager.clearFocus()
          onBackToCalendar(resolveDate())
        },
        modifier = Modifier.size(36.dp)
      ) {
        Icon(
          imageVector = Icons.Default.CalendarMonth,
          contentDescription = "نمایش تقویم",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(22.dp)
        )
      }
    }

    // Divider
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(1.dp)
        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {

      // Large digital date display (always LTR: year on the left, day on the right)
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Year
            DateDigitField(
              value = yearValue,
              chars = 4,
              textStyle = clockStyle,
              focusRequester = yearFocus,
              imeAction = ImeAction.Next,
              onImeAction = { goToMonth() },
              onTap = {
                yearValue = dateSelectAll(
                  if (yearValue.text.length == 4) yearValue.text else lastValidYear.toString()
                )
                yearFocus.requestFocus()
                keyboard?.show()
              },
              onFocusChanged = { focused ->
                val t = if (yearValue.text.length == 4) yearValue.text else lastValidYear.toString()
                yearValue = if (focused) dateSelectAll(t) else dateCaretAtEnd(t)
              },
              onValueChange = { new ->
                var digits = dateDigitsOnly(new.text)
                if (digits.length > 4) digits = digits.takeLast(1)
                if (digits.length == 4) {
                  val v = digits.toInt().coerceIn(1300, 1460)
                  lastValidYear = v
                  yearValue = dateCaretAtEnd(v.toString())
                  clampDay(v, effMonth())
                  goToMonth()
                } else {
                  yearValue = dateCaretAtEnd(digits)
                }
              }
            )

            Text(
              text = "/",
              style = clockStyle,
              modifier = Modifier.padding(horizontal = 2.dp)
            )

            // Month
            DateDigitField(
              value = monthValue,
              chars = 2,
              textStyle = clockStyle,
              focusRequester = monthFocus,
              imeAction = ImeAction.Next,
              onImeAction = { goToDay() },
              onTap = {
                monthValue = dateSelectAll(sanitizeMonth(monthValue.text))
                monthFocus.requestFocus()
                keyboard?.show()
              },
              onFocusChanged = { focused ->
                val t = sanitizeMonth(monthValue.text)
                monthValue = if (focused) dateSelectAll(t) else dateCaretAtEnd(t)
              },
              onValueChange = { new ->
                var digits = dateDigitsOnly(new.text)
                if (digits.length > 2) digits = digits.takeLast(1)
                when (digits.length) {
                  0 -> monthValue = TextFieldValue("", TextRange(0))
                  1 -> {
                    val d = digits.toInt()
                    if (d >= 2) {
                      monthValue = dateCaretAtEnd("0$d")
                      clampDay(effYear(), d)
                      goToDay()
                    } else {
                      monthValue = dateCaretAtEnd(digits)
                    }
                  }
                  else -> {
                    val v = digits.toInt().coerceIn(1, 12)
                    monthValue = dateCaretAtEnd(String.format(Locale.US, "%02d", v))
                    clampDay(effYear(), v)
                    goToDay()
                  }
                }
              }
            )

            Text(
              text = "/",
              style = clockStyle,
              modifier = Modifier.padding(horizontal = 2.dp)
            )

            // Day
            DateDigitField(
              value = dayValue,
              chars = 2,
              textStyle = clockStyle,
              focusRequester = dayFocus,
              imeAction = ImeAction.Done,
              onImeAction = {
                focusManager.clearFocus()
                keyboard?.hide()
              },
              onTap = {
                dayValue = dateSelectAll(sanitizeDay(dayValue.text))
                dayFocus.requestFocus()
                keyboard?.show()
              },
              onFocusChanged = { focused ->
                val t = sanitizeDay(dayValue.text)
                dayValue = if (focused) dateSelectAll(t) else dateCaretAtEnd(t)
              },
              onValueChange = { new ->
                var digits = dateDigitsOnly(new.text)
                if (digits.length > 2) digits = digits.takeLast(1)
                when (digits.length) {
                  0 -> dayValue = TextFieldValue("", TextRange(0))
                  1 -> dayValue = dateCaretAtEnd(minOf(digits.toInt(), 3).toString())
                  else -> {
                    val v = digits.toInt().coerceIn(1, maxDays())
                    dayValue = dateCaretAtEnd(String.format(Locale.US, "%02d", v))
                  }
                }
              }
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Quick Preset Chips
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        presets.forEach { preset ->
          val isSelected = current.year == preset.date.year &&
                  current.month == preset.date.month &&
                  current.day == preset.date.day
          val contentColor =
            if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
          Surface(
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .clickable {
                lastValidYear = preset.date.year
                yearValue = dateCaretAtEnd(preset.date.year.toString())
                monthValue = dateCaretAtEnd(datePad2(preset.date.month.toString()))
                dayValue = dateCaretAtEnd(datePad2(preset.date.day.toString()))
              }
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 2.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = preset.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Confirm Button
      Button(
        onClick = {
          keyboard?.hide()
          focusManager.clearFocus()
          onConfirm(resolveDate())
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("تایید", fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun DateDigitField(
  value: TextFieldValue,
  onValueChange: (TextFieldValue) -> Unit,
  chars: Int,
  focusRequester: FocusRequester,
  onFocusChanged: (Boolean) -> Unit,
  onTap: () -> Unit,
  imeAction: ImeAction,
  onImeAction: () -> Unit,
  textStyle: TextStyle
) {
  val fieldWidth = with(LocalDensity.current) {
    (textStyle.fontSize * (chars * 0.6f + 0.3f)).toDp()
  }

  Box(modifier = Modifier.width(fieldWidth), contentAlignment = Alignment.Center) {
    BasicTextField(
      value = value,
      onValueChange = onValueChange,
      singleLine = true,
      textStyle = textStyle,
      cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
      keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Number,
        imeAction = imeAction
      ),
      keyboardActions = KeyboardActions(
        onNext = { onImeAction() },
        onDone = { onImeAction() }
      ),
      modifier = Modifier
        .fillMaxWidth()
        .focusRequester(focusRequester)
        .onFocusChanged { onFocusChanged(it.isFocused) }
    )
    Box(
      modifier = Modifier
        .matchParentSize()
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = onTap
        )
    )
  }
}

@Composable
private fun MonthPageContent(
  year: Int,
  month: Int,
  selectedDay: Int,
  currentActiveYear: Int,
  currentActiveMonth: Int,
  today: JalaliDate,
  onDayClick: (Int) -> Unit
) {
  val daysInMonth = JalaliDate.getDaysInMonth(year, month)
  val firstDayOffset = JalaliDate.getFirstDayOfWeekOffset(year, month)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(top = 4.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    for (weekIndex in 0 until 6) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {
        for (dayOfWeekIndex in 0 until 7) {
          val cellIndex = weekIndex * 7 + dayOfWeekIndex
          val dayNumber = cellIndex - firstDayOffset + 1
          if (dayNumber in 1..daysInMonth) {
            val isSelected = dayNumber == selectedDay && month == currentActiveMonth && year == currentActiveYear
            val isToday = dayNumber == today.day && month == today.month && year == today.year
            val isFriday = dayOfWeekIndex == 6

            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .then(
                  when {
                    isSelected -> Modifier.background(MaterialTheme.colorScheme.primary)
                    isToday -> Modifier
                      .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                      .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary), CircleShape)
                    else -> Modifier
                  }
                )
                .clickable { onDayClick(dayNumber) }
            ) {
              Text(
                text = "$dayNumber",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                color = when {
                  isSelected -> MaterialTheme.colorScheme.onPrimary
                  isFriday -> MaterialTheme.colorScheme.error
                  else -> MaterialTheme.colorScheme.onSurface
                }
              )
            }
          } else {
            Spacer(modifier = Modifier.size(36.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun MonthsGridView(
  currentMonth: Int,
  onMonthSelected: (Int) -> Unit
) {
  LazyVerticalGrid(
    columns = GridCells.Fixed(3),
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(vertical = 4.dp)
  ) {
    items(JalaliDate.PERSIAN_MONTHS.size) { index ->
      val monthIndex = index + 1
      val isCurrent = monthIndex == currentMonth
      Surface(
        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .clickable { onMonthSelected(monthIndex) }
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.padding(vertical = 16.dp)
        ) {
          Text(
            text = JalaliDate.PERSIAN_MONTHS[index],
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
            color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}

@Composable
private fun YearsGridView(
  currentYear: Int,
  todayYear: Int,
  onYearSelected: (Int) -> Unit
) {
  val years = remember(todayYear) {
    (todayYear until (todayYear + 12)).toList()
  }

  LazyVerticalGrid(
    columns = GridCells.Fixed(3),
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(vertical = 4.dp)
  ) {
    items(years.size) { idx ->
      val y = years[idx]
      val isSelected = y == currentYear
      val isTodayYear = y == todayYear

      Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        border = if (isTodayYear && !isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .clickable { onYearSelected(y) }
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.padding(vertical = 16.dp)
        ) {
          Text(
            text = "$y",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected || isTodayYear) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}

// =============================================================================
// پیش‌نمایش‌های دوگانه کامپوز برای کامپوننت تقویم جلالی
// =============================================================================

@Preview(name = "تقویم جلالی - ۱. حالت انتخاب دیداری از تقویم (Days Grid)", showBackground = true)
@Composable
fun PersianDatePickerCalendarDaysPreview() {
  MyApplicationTheme {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        contentAlignment = Alignment.Center
      ) {
        PersianDatePickerCard(
          initialDate = JalaliDate(1403, 8, 15),
          initialKeyboardMode = false,
          onDateSelected = {},
          onDismissRequest = {}
        )
      }
    }
  }
}

@Preview(name = "تقویم جلالی - ۲. حالت ورود دستی با کیبورد (Manual Input)", showBackground = true)
@Composable
fun PersianDatePickerManualInputPreview() {
  MyApplicationTheme {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        contentAlignment = Alignment.Center
      ) {
        PersianDatePickerCard(
          initialDate = JalaliDate(1403, 8, 15),
          initialKeyboardMode = true,
          onDateSelected = {},
          onDismissRequest = {}
        )
      }
    }
  }
}


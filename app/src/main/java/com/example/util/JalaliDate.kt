package com.example.util

import java.util.Calendar

data class JalaliDate(
  val year: Int,
  val month: Int,
  val day: Int
) {
  val monthName: String get() = PERSIAN_MONTHS.getOrElse(month - 1) { "" }

  fun formatPersian(): String {
    return "$day $monthName $year"
  }

  fun formatShort(): String {
    return "%04d/%02d/%02d".format(year, month, day)
  }

  companion object {
    val PERSIAN_MONTHS = listOf(
      "فروردین", "اردیبهشت", "خرداد",
      "تیر", "مرداد", "شهریور",
      "مهر", "آبان", "آذر",
      "دی", "بهمن", "اسفند"
    )

    // Persian week order starting with Saturday (شنبه) to Friday (جمعه)
    val WEEK_DAYS_PERSIAN = listOf(
      "ش", "ی", "د", "س", "چ", "پ", "ج"
    )

    val WEEK_DAYS_PERSIAN_FULL = listOf(
      "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه"
    )

    fun isLeapYear(jYear: Int): Boolean {
      val r = jYear % 33
      return r == 1 || r == 5 || r == 9 || r == 13 || r == 17 || r == 22 || r == 26 || r == 30
    }

    fun getDaysInMonth(jYear: Int, jMonth: Int): Int {
      return when (jMonth) {
        in 1..6 -> 31
        in 7..11 -> 30
        12 -> if (isLeapYear(jYear)) 30 else 29
        else -> 30
      }
    }

    fun parse(text: String): JalaliDate? {
      if (text.isBlank()) return null
      val cleanText = text
        .replace('۰', '0').replace('۱', '1').replace('۲', '2').replace('۳', '3').replace('۴', '4')
        .replace('۵', '5').replace('۶', '6').replace('۷', '7').replace('۸', '8').replace('۹', '9')
        .trim()

      val parts = cleanText.split(Regex("[\\s/\\-]+")).filter { it.isNotBlank() }
      if (parts.size >= 3) {
        val p0 = parts[0].toIntOrNull()
        val p2 = parts[2].toIntOrNull()
        val monthIdx = PERSIAN_MONTHS.indexOfFirst { parts[1].contains(it) }
        val m = if (monthIdx >= 0) monthIdx + 1 else parts[1].toIntOrNull()

        if (p0 != null && p2 != null && m != null) {
          return if (p0 > 1000) {
            JalaliDate(p0, m, p2)
          } else {
            JalaliDate(p2, m, p0)
          }
        }
      }
      return null
    }

    fun fromGregorian(gy: Int, gm: Int, gd: Int): JalaliDate {
      val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
      val isGLeap = (gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)
      if (isGLeap) gDaysInMonth[2] = 29

      val gy2 = gy - 1600
      val gm2 = gm - 1
      val gd2 = gd - 1

      var gDayNo = 365 * gy2 + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400
      for (i in 1..gm2) {
        gDayNo += gDaysInMonth[i]
      }
      gDayNo += gd2

      var jDayNo = gDayNo - 79
      val jNp = jDayNo / 12053
      jDayNo %= 12053

      var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
      jDayNo %= 1461

      if (jDayNo >= 366) {
        jy += (jDayNo - 1) / 365
        jDayNo = (jDayNo - 1) % 365
      }

      val jm: Int
      val jd: Int
      if (jDayNo < 186) {
        jm = 1 + jDayNo / 31
        jd = 1 + (jDayNo % 31)
      } else {
        jm = 7 + (jDayNo - 186) / 30
        jd = 1 + ((jDayNo - 186) % 30)
      }

      return JalaliDate(jy, jm, jd)
    }

    fun today(): JalaliDate {
      val cal = Calendar.getInstance()
      val gYear = cal.get(Calendar.YEAR)
      val gMonth = cal.get(Calendar.MONTH) + 1
      val gDay = cal.get(Calendar.DAY_OF_MONTH)
      return fromGregorian(gYear, gMonth, gDay)
    }

    fun fromMillis(millis: Long): JalaliDate {
      val cal = Calendar.getInstance().apply { timeInMillis = millis }
      return fromGregorian(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
      )
    }

    fun toGregorianCalendar(jy: Int, jm: Int, jd: Int): Calendar {
      val jy2 = jy - 979
      val jm2 = jm - 1
      val jd2 = jd - 1

      var jDayNo = 365 * jy2 + (jy2 / 33) * 8 + ((jy2 % 33 + 3) / 4)
      for (i in 0 until jm2) {
        jDayNo += if (i < 6) 31 else 30
      }
      jDayNo += jd2

      var gDayNo = jDayNo + 79
      var gy = 1600 + 400 * (gDayNo / 146097)
      gDayNo %= 146097

      var leap = true
      if (gDayNo >= 36525) {
        gDayNo--
        gy += 100 * (gDayNo / 36524)
        gDayNo %= 36524
        if (gDayNo >= 365) {
          gDayNo++
        } else {
          leap = false
        }
      }

      gy += 4 * (gDayNo / 1461)
      gDayNo %= 1461

      if (gDayNo >= 366) {
        leap = false
        gDayNo--
        gy += gDayNo / 365
        gDayNo %= 365
      }

      val gDaysInMonth = intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
      var gm = 0
      while (gm < 12 && gDayNo >= gDaysInMonth[gm]) {
        gDayNo -= gDaysInMonth[gm]
        gm++
      }

      val cal = Calendar.getInstance()
      cal.set(Calendar.YEAR, gy)
      cal.set(Calendar.MONTH, gm)
      cal.set(Calendar.DAY_OF_MONTH, gDayNo + 1)
      cal.set(Calendar.HOUR_OF_DAY, 0)
      cal.set(Calendar.MINUTE, 0)
      cal.set(Calendar.SECOND, 0)
      cal.set(Calendar.MILLISECOND, 0)
      return cal
    }

    // Offset in week: Saturday = 0, Sunday = 1, ..., Friday = 6
    fun getFirstDayOfWeekOffset(jYear: Int, jMonth: Int): Int {
      val cal = toGregorianCalendar(jYear, jMonth, 1)
      return when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SATURDAY -> 0
        Calendar.SUNDAY -> 1
        Calendar.MONDAY -> 2
        Calendar.TUESDAY -> 3
        Calendar.WEDNESDAY -> 4
        Calendar.THURSDAY -> 5
        Calendar.FRIDAY -> 6
        else -> 0
      }
    }
  }
}

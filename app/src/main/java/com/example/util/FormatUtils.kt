package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object FormatUtils {

    fun formatNumber(number: Long): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
        }
        val formatter = DecimalFormat("#,###", symbols)
        return formatter.format(number)
    }

    fun parseAmount(input: String): Long {
        val clean = input
            .replace(",", "")
            .replace("،", "")
            .replace(" ", "")
            .replace('۰', '0').replace('۱', '1').replace('۲', '2').replace('۳', '3').replace('۴', '4')
            .replace('۵', '5').replace('۶', '6').replace('۷', '7').replace('۸', '8').replace('۹', '9')
            .trim()
        return clean.toLongOrNull() ?: 0L
    }

    fun formatCurrency(amount: Long, currencyUnit: String = "TOMAN"): String {
        val formatted = formatNumber(amount)
        val unitLabel = if (currencyUnit == "RIAL") "ریال" else "تومان"
        return "$formatted $unitLabel"
    }

    /**
     * Converts an amount based on selected currency preference.
     * Stored amounts in DB are treated consistently (by default as Toman or user preference).
     */
    fun convertToDisplay(amount: Long, baseIsToman: Boolean, targetUnit: String): Long {
        return if (baseIsToman) {
            if (targetUnit == "RIAL") amount * 10L else amount
        } else {
            if (targetUnit == "TOMAN") amount / 10L else amount
        }
    }

    // Number to Persian Words converter for official contracts
    private val YEKAN = arrayOf("", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه")
    private val DAH_TA_NOZDAH = arrayOf(
        "ده", "یازده", "دوازده", "سیزده", "چهارده", "پانزده",
        "شانزده", "هفده", "هجده", "نوزده"
    )
    private val DAHGAN = arrayOf("", "", "بیست", "سی", "چهل", "پنجاه", "شصت", "هفتاد", "هشتاد", "نود")
    private val SADGAN = arrayOf("", "یکصد", "دویست", "سیصد", "چهارصد", "پانصد", "ششصد", "هفتصد", "هشتصد", "نهصد")
    private val SCALE = arrayOf("", "هزار", "میلیون", "میلیارد", "تریلیون")

    fun numberToPersianWords(number: Long): String {
        if (number == 0L) return "صفر"
        if (number < 0) return "منفی " + numberToPersianWords(-number)

        var n = number
        val parts = mutableListOf<String>()
        var scaleIndex = 0

        while (n > 0) {
            val chunk = (n % 1000).toInt()
            if (chunk > 0) {
                val chunkText = convertChunk(chunk)
                val scale = SCALE[scaleIndex]
                val part = if (scale.isNotEmpty()) "$chunkText $scale" else chunkText
                parts.add(0, part)
            }
            n /= 1000
            scaleIndex++
        }

        return parts.joinToString(" و ")
    }

    private fun convertChunk(number: Int): String {
        val result = mutableListOf<String>()
        val hundreds = number / 100
        val rem = number % 100

        if (hundreds > 0) {
            result.add(SADGAN[hundreds])
        }

        when {
            rem in 10..19 -> {
                result.add(DAH_TA_NOZDAH[rem - 10])
            }
            rem >= 20 -> {
                val tens = rem / 10
                val ones = rem % 10
                result.add(DAHGAN[tens])
                if (ones > 0) {
                    result.add(YEKAN[ones])
                }
            }
            rem in 1..9 -> {
                result.add(YEKAN[rem])
            }
        }

        return result.joinToString(" و ")
    }
}

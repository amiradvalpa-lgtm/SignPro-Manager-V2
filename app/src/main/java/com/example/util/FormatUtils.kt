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

    fun toPersianDigits(input: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val builder = StringBuilder()
        for (ch in input) {
            if (ch in '0'..'9') {
                builder.append(persianDigits[ch - '0'])
            } else {
                builder.append(ch)
            }
        }
        return builder.toString()
    }

    fun getCurrencyLabel(currencyUnit: String = "TOMAN"): String {
        return if (currencyUnit == "RIAL") "ریال" else "تومان"
    }

    fun formatCurrency(amount: Long, currencyUnit: String = "TOMAN"): String {
        val formatted = toPersianDigits(formatNumber(amount))
        val unitLabel = getCurrencyLabel(currencyUnit)
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

    fun toEnglishDigits(input: String): String {
        return input
            .replace('۰', '0').replace('۱', '1').replace('۲', '2').replace('۳', '3').replace('۴', '4')
            .replace('۵', '5').replace('۶', '6').replace('۷', '7').replace('۸', '8').replace('۹', '9')
            .replace('٠', '0').replace('١', '1').replace('٢', '2').replace('٣', '3').replace('٤', '4')
            .replace('٥', '5').replace('٦', '6').replace('٧', '7').replace('٨', '8').replace('٩', '9')
    }

    /**
     * Cleans pasted or typed phone numbers:
     * - Strips spaces and non-digits
     * - Removes +98, 0098, or leading 98
     * - Guarantees leading 0
     * - Caps at 11 digits (0 + 10 digits)
     */
    fun cleanAndNormalizePhone(raw: String): String {
        val english = toEnglishDigits(raw)
        var cleaned = english.replace(Regex("[^0-9+]"), "").trim()
        if (cleaned.startsWith("+98")) {
            cleaned = cleaned.removePrefix("+98")
        } else if (cleaned.startsWith("0098")) {
            cleaned = cleaned.removePrefix("0098")
        } else if (cleaned.startsWith("98") && cleaned.length >= 12) {
            cleaned = cleaned.removePrefix("98")
        }
        cleaned = cleaned.replace("+", "")
        if (cleaned.isNotEmpty() && !cleaned.startsWith("0")) {
            cleaned = "0$cleaned"
        }
        return cleaned.take(11)
    }

    /**
     * Formats phone number:
     * If mobile (09...): "0914 111 1111" (4-3-4)
     * If landline (021..., 041...): "021 1111 1111" (3-4-4)
     * Wrapped in Unicode LTR Isolate (\u2066 ... \u2069) so 0914 is always displayed on the left in RTL.
     */
    fun formatPhoneNumber(raw: String): String {
        val clean = cleanAndNormalizePhone(raw)
        if (clean.length < 2) return clean

        val formatted = if (clean.startsWith("09")) {
            when {
                clean.length <= 4 -> clean
                clean.length <= 7 -> "${clean.substring(0, 4)} ${clean.substring(4)}"
                else -> "${clean.substring(0, 4)} ${clean.substring(4, 7)} ${clean.substring(7)}"
            }
        } else {
            // Landline (e.g. 021, 041, 031...)
            when {
                clean.length <= 3 -> clean
                clean.length <= 7 -> "${clean.substring(0, 3)} ${clean.substring(3)}"
                else -> "${clean.substring(0, 3)} ${clean.substring(3, 7)} ${clean.substring(7)}"
            }
        }
        // Use Left-to-Right Isolate (\u2066) and Pop Directional Isolate (\u2069)
        return "\u2066$formatted\u2069"
    }

    /**
     * Validates if the phone number has exactly 11 digits (leading 0 + 10 digits).
     */
    fun isValidPhoneNumber(raw: String): Boolean {
        val clean = cleanAndNormalizePhone(raw)
        return clean.length == 11 && clean.startsWith("0")
    }

    /**
     * Validates 10-digit Iranian National Code using official checksum algorithm (Modulo 11).
     */
    fun isValidIranianNationalCode(raw: String): Boolean {
        val clean = toEnglishDigits(raw).replace(Regex("[^0-9]"), "").trim()
        if (clean.length != 10) return false

        // Check for disallowed repeated sequences like 0000000000, 1111111111, etc.
        val allSame = (0..9).any { clean == it.toString().repeat(10) }
        if (allSame) return false

        val checkDigit = clean[9].digitToInt()
        var sum = 0
        for (i in 0 until 9) {
            sum += clean[i].digitToInt() * (10 - i)
        }
        val remainder = sum % 11
        return if (remainder < 2) {
            checkDigit == remainder
        } else {
            checkDigit == (11 - remainder)
        }
    }
}


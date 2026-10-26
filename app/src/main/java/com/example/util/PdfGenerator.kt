package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.local.entities.CustomerEntity
import com.example.data.local.entities.InvoiceEntity
import com.example.data.local.entities.InvoiceItemEntity
import com.example.data.local.entities.ProjectEntity
import com.example.data.local.entities.ProjectSpecEntity
import com.example.data.preferences.ContractorInfo
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    private const val PAGE_WIDTH = 595 // Standard A4 points (72 dpi)
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f

    fun generateContractPdf(
        context: Context,
        contractor: ContractorInfo,
        customer: CustomerEntity,
        project: ProjectEntity,
        specs: List<ProjectSpecEntity>,
        durationDays: Int,
        totalAmount: Long,
        prepayment: Long,
        balanceAmount: Long,
        contractDate: String,
        arbitratorName: String = "مرضی‌الطرفین",
        customClauses: List<String> = emptyList()
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        drawBorderAndHeader(canvas, "قرارداد رسمی تابلوسازی", contractDate)

        var y = 90f
        val contentWidth = (PAGE_WIDTH - 2 * MARGIN).toInt()

        // 1. Parties
        val partiesText = """
            ۱ - طرفین قرارداد:
            • پیمانکار: جناب آقای ${contractor.name} نماینده مجموعه ${contractor.businessName} به نشانی ${contractor.address}، شماره تماس ${contractor.phone}.
            • کارفرما: جناب آقای/سرکار خانم ${customer.name}، کد ملی: ${customer.nationalCode.ifEmpty { "ثبت نشده" }}، تلفن: ${customer.phone}، نشانی: ${customer.address.ifEmpty { "آدرس قید نشده" }}.
        """.trimIndent()
        y = drawRtlText(canvas, partiesText, MARGIN, y, contentWidth, 10.5f, isBold = false) + 8f

        // 2. Subject & Specs
        val specSummary = if (specs.isEmpty()) "طبق توافق و نقشه‌های تاییدشده" else {
            specs.joinToString("\n") { s ->
                "• ${s.subTitle} (نوع: ${s.signType}) - ابعاد: طول ${s.lengthCm} cm × عرض ${s.widthCm} cm × عمق ${s.depthCm} cm ${if (s.description.isNotEmpty()) "- توضیحات: " + s.description else ""}"
            }
        }
        val subjectText = """
            ۲ - موضوع قرارداد:
            طراحی، ساخت و نصب تابلوی پروژه «${project.title}» طبق طرح و مشخصات فنی زیر:
            $specSummary
        """.trimIndent()
        y = drawRtlText(canvas, subjectText, MARGIN, y, contentWidth, 10f, isBold = false) + 8f

        // 3. Amount & Payment
        val totalWords = FormatUtils.numberToPersianWords(totalAmount)
        val balanceWords = FormatUtils.numberToPersianWords(balanceAmount)
        val financialText = """
            ۳ - مبلغ قرارداد و نحوه پرداخت:
            جمع مبلغ قابل پرداخت این قرارداد: ${FormatUtils.formatNumber(totalAmount)} ریال معادل ($totalWords ریال) می‌باشد.
            مبلغ ${FormatUtils.formatNumber(prepayment)} ریال بابت پیش‌پرداخت به صورت نقد یا کارت به کارت به پیمانکار پرداخت گردید و الباقی مبلغ قرارداد که معادل ${FormatUtils.formatNumber(balanceAmount)} ریال ($balanceWords ریال) می‌باشد، پس از اتمام و تحویل کار، به شماره حساب ${contractor.accountNumber}، شبا ${contractor.shabaNumber} یا شماره کارت ${contractor.cardNumber} نزد ${contractor.bankName} واریز می‌گردد.
        """.trimIndent()
        y = drawRtlText(canvas, financialText, MARGIN, y, contentWidth, 9.5f, isBold = false) + 8f

        // 4. Duration
        val durationText = """
            ۴ - مدت قرارداد:
            بر اساس توافق طرفین، مدت اجرای پروژه از تاریخ واریز پیش‌پرداخت معادل $durationDays روز کاری می‌باشد. (در صورت بروز فورس‌ماژور، مدت تمدید می‌گردد).
        """.trimIndent()
        y = drawRtlText(canvas, durationText, MARGIN, y, contentWidth, 9.5f, isBold = false) + 8f

        // 5 & 6. Terms Summary
        val termsText = """
            ۵ - تعهدات پیمانکار: ارائه کار طبق طرح توافقی، رفع هرگونه مغایرت، هزینه حمل برای ابعاد زیر ۲×۳ متر با پیمانکار، تست روشنایی با برق موقت (سیم‌کشی داخلی ساختمان با کارفرما).
            ۶ - تعهدات کارفرما: اخذ مجوزهای شهرداری و اتحادیه، جابجایی موانع، پرداخت به‌موقع. کلیه اجناس تا زمان تسویه کامل در حکم امانت نزد کارفرما و متعلق به پیمانکار بوده و در صورت عدم تسویه، حق استرداد بدون نیاز به حکم دادگاه برای پیمانکار محفوظ است. کلیه هزینه‌های داربست و جرثقیل بر عهده کارفرما می‌باشد.
            ۷ - ضمانت: ساخت، مونتاژ و روشنایی به مدت یک سال از تاریخ تحویل تحت گارانتی مجری می‌باشد. (دستکاری افراد غیرمجاز موجب ابطال ضمانت است).
        """.trimIndent()
        y = drawRtlText(canvas, termsText, MARGIN, y, contentWidth, 8.8f, isBold = false) + 8f

        if (customClauses.isNotEmpty()) {
            val customText = "بندهای الحاقی:\n" + customClauses.joinToString("\n") { "• $it" }
            y = drawRtlText(canvas, customText, MARGIN, y, contentWidth, 8.8f, isBold = false) + 6f
        }

        // Closing & Signatures
        val footerLegal = "طرفین کافه خیارات را از خود ساقط نمودند. داور مرضی‌الطرفین: $arbitratorName. این قرارداد در ۲ نسخه تنظیم شده است."
        drawRtlText(canvas, footerLegal, MARGIN, y, contentWidth, 8.5f, isBold = true)

        // Signatures boxes
        drawSignatures(canvas, PAGE_HEIGHT - 75f)

        pdfDocument.finishPage(page)

        val outputDir = File(context.filesDir, "documents").apply { mkdirs() }
        val file = File(outputDir, "contract_${project.id.take(8)}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    fun generateInvoicePdf(
        context: Context,
        contractor: ContractorInfo,
        customer: CustomerEntity,
        project: ProjectEntity,
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>,
        expensesTotal: Long
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        drawBorderAndHeader(canvas, "فاکتور فروش و خدمات تابلوسازی", invoice.issueDate)

        var y = 90f
        val contentWidth = (PAGE_WIDTH - 2 * MARGIN).toInt()

        // Info Box
        val customerInfo = "مشتری / کارفرما: ${customer.name} | تلفن: ${customer.phone} | پروژه: ${project.title}"
        val contractorSummary = "مجری: ${contractor.businessName} - ${contractor.name} (${contractor.phone})"
        y = drawRtlText(canvas, "$contractorSummary\n$customerInfo", MARGIN, y, contentWidth, 10f, isBold = false) + 12f

        // Table Header
        val colWidths = floatArrayOf(30f, 180f, 60f, 110f, 140f) // No, Service, Qty, Unit Price, Total
        val tableStartX = MARGIN
        val tableWidth = contentWidth.toFloat()

        val paint = Paint().apply {
            color = Color.rgb(240, 243, 246)
            style = Paint.Style.FILL
        }
        canvas.drawRect(tableStartX, y, tableStartX + tableWidth, y + 24f, paint)

        val borderPaint = Paint().apply {
            color = Color.DKGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRect(tableStartX, y, tableStartX + tableWidth, y + 24f, borderPaint)

        // Header titles
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9.5f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ردیف", tableStartX + tableWidth - 15f, y + 16f, textPaint)
        canvas.drawText("شرح خدمت / کالا", tableStartX + tableWidth - 120f, y + 16f, textPaint)
        canvas.drawText("متراژ/تعداد", tableStartX + tableWidth - 240f, y + 16f, textPaint)
        canvas.drawText("قیمت واحد (ریال)", tableStartX + tableWidth - 325f, y + 16f, textPaint)
        canvas.drawText("مبلغ کل (ریال)", tableStartX + 70f, y + 16f, textPaint)

        y += 24f
        var itemsSum = 0L

        items.forEachIndexed { idx, item ->
            val rowHeight = 22f
            canvas.drawRect(tableStartX, y, tableStartX + tableWidth, y + rowHeight, borderPaint)

            val rowPaint = Paint().apply {
                color = Color.BLACK
                textSize = 9f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("${idx + 1}", tableStartX + tableWidth - 15f, y + 15f, rowPaint)

            val servicePaint = Paint().apply {
                color = Color.BLACK
                textSize = 8.5f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(item.serviceType.take(30), tableStartX + tableWidth - 35f, y + 15f, servicePaint)
            canvas.drawText("${item.quantity}", tableStartX + tableWidth - 240f, y + 15f, rowPaint)
            canvas.drawText(FormatUtils.formatNumber(item.unitPrice), tableStartX + tableWidth - 325f, y + 15f, rowPaint)
            canvas.drawText(FormatUtils.formatNumber(item.calculatedTotal), tableStartX + 70f, y + 15f, rowPaint)

            itemsSum += item.calculatedTotal
            y += rowHeight
        }

        y += 16f
        // Totals
        val finalPayable = itemsSum - invoice.discount + expensesTotal
        val totalsText = """
            جمع اقلام فاکتور: ${FormatUtils.formatNumber(itemsSum)} ریال
            تخفیف: ${FormatUtils.formatNumber(invoice.discount)} ریال
            هزینه‌های جانبی پروژه: ${FormatUtils.formatNumber(expensesTotal)} ریال
            مبلغ نهایی قابل پرداخت: ${FormatUtils.formatNumber(finalPayable)} ریال (${FormatUtils.numberToPersianWords(finalPayable)} ریال)
        """.trimIndent()

        y = drawRtlText(canvas, totalsText, MARGIN + 120f, y, (contentWidth - 120), 10f, isBold = true) + 20f

        if (invoice.description.isNotEmpty()) {
            drawRtlText(canvas, "توضیحات فاکتور: ${invoice.description}", MARGIN, y, contentWidth, 9f, isBold = false)
        }

        drawSignatures(canvas, PAGE_HEIGHT - 75f)

        pdfDocument.finishPage(page)

        val outputDir = File(context.filesDir, "documents").apply { mkdirs() }
        val file = File(outputDir, "invoice_${invoice.id.take(8)}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    private fun drawBorderAndHeader(canvas: Canvas, title: String, date: String) {
        val borderPaint = Paint().apply {
            color = Color.rgb(20, 30, 50)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(MARGIN - 10, MARGIN - 10, PAGE_WIDTH - MARGIN + 10, PAGE_HEIGHT - MARGIN + 10, borderPaint)

        // Header Title
        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 14f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("به نام خدا", PAGE_WIDTH / 2f, MARGIN + 12f, titlePaint)
        titlePaint.textSize = 13f
        canvas.drawText(title, PAGE_WIDTH / 2f, MARGIN + 32f, titlePaint)

        // Date & Document Number
        val subPaint = Paint().apply {
            color = Color.GRAY
            textSize = 8.5f
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("تاریخ تنظیم: $date", MARGIN, MARGIN + 25f, subPaint)

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        canvas.drawLine(MARGIN, MARGIN + 45f, PAGE_WIDTH - MARGIN, MARGIN + 45f, linePaint)
    }

    private fun drawRtlText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Int,
        textSize: Float,
        isBold: Boolean
    ): Float {
        val textPaint = TextPaint().apply {
            color = Color.BLACK
            this.textSize = textSize
            isAntiAlias = true
            isFakeBoldText = isBold
        }

        @Suppress("DEPRECATION")
        val staticLayout = StaticLayout(
            text,
            textPaint,
            width,
            Layout.Alignment.ALIGN_NORMAL,
            1.25f,
            0f,
            false
        )

        canvas.save()
        canvas.translate(x, y)
        staticLayout.draw(canvas)
        canvas.restore()

        return y + staticLayout.height
    }

    private fun drawSignatures(canvas: Canvas, y: Float) {
        val sigPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("امضاء و اثر انگشت کارفرما", MARGIN + 90f, y, sigPaint)
        canvas.drawText("امضاء و مهر پیمانکار (آلپا)", PAGE_WIDTH - MARGIN - 90f, y, sigPaint)
    }

    fun sharePdf(context: Context, file: File, title: String = "ارسال سند تابلوسازی") {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun viewPdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

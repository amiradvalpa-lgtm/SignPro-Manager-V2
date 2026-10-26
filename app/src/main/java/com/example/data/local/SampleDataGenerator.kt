package com.example.data.local

import com.example.data.local.entities.*
import com.example.util.JalaliDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object SampleDataGenerator {

    // Identifiers for sample customers so they can be identified or cleared
    const val SAMPLE_TAG = "SAMPLE_DATA"

    suspend fun seedSampleData(database: AppDatabase) = withContext(Dispatchers.IO) {
        val today = JalaliDate.today()
        val dateToday = today.formatShort()
        val dateWeekAgo = JalaliDate.fromGregorian(2026, 10, 17).formatShort()
        val dateTwoWeeksAgo = JalaliDate.fromGregorian(2026, 10, 10).formatShort()
        val checkDueDate1 = JalaliDate.fromGregorian(2026, 11, 10).formatShort()
        val checkDueDate2 = JalaliDate.fromGregorian(2026, 11, 25).formatShort()
        val checkDueDate3 = JalaliDate.fromGregorian(2026, 10, 20).formatShort() // Overdue/Cleared

        // 1. Customers
        val customer1 = CustomerEntity(
            id = "sample_cust_1",
            name = "طلا و جواهر رحمانی (الماس)",
            phone = "09121112233",
            address = "بازار بزرگ تهران، سرای زرگران، پلاک ۱۴",
            nationalCode = "0012345678",
            description = "مشتری وفادار - سفارش تابلوی چلنیوم دوبل کریستال با زیرسازی کامپوزیت طلایی",
            userId = "admin"
        )
        val customer2 = CustomerEntity(
            id = "sample_cust_2",
            name = "مجتمع تجاری و پزشکی نیایش",
            phone = "09123456789",
            address = "بلوار شهید بهشتی، نرسیده به میدان شهدا، برج نیایش",
            nationalCode = "10100456789",
            description = "پروژه بزرگ نمای کامپوزیت نقره‌ای دودی و حروف استیل لایت اندرانیک",
            userId = "admin"
        )
        val customer3 = CustomerEntity(
            id = "sample_cust_3",
            name = "کافه رستوران زنجیره‌ای باران",
            phone = "09358889900",
            address = "خیابان ولیعصر، نبش کوچه فرشته، پلاک ۲",
            nationalCode = "0078901234",
            description = "مجموعه کافه‌ها - سفارش لایت‌باکس‌های دکوراتیو و حروف نئون‌فلکسی",
            userId = "admin"
        )

        val customers = listOf(customer1, customer2, customer3)
        database.customerDao().insertCustomers(customers)

        // 2. Projects
        val project1 = ProjectEntity(
            id = "sample_proj_1",
            customerId = customer1.id,
            title = "تابلوی چلنیوم طلایی دوبل کریستال طلا و جواهر رحمانی",
            startDate = dateTwoWeeksAgo,
            status = "در حال ساخت",
            description = "متراژ برآورد شده ۴۲ متر حروف چلنیوم با پلکسی یرلانگ تایوان و ورق کامپوزیت",
            userId = "admin"
        )
        val project2 = ProjectEntity(
            id = "sample_proj_2",
            customerId = customer2.id,
            title = "نمای کامپوزیت و حروف استیل میرور برج نیایش",
            startDate = dateWeekAgo,
            status = "در حال نصب",
            description = "نصب در ارتفاع با داربست و جرثقیل کابلی - تعهد تحویل قبل از افتتاحیه",
            userId = "admin"
        )
        val project3 = ProjectEntity(
            id = "sample_proj_3",
            customerId = customer3.id,
            title = "لایت‌باکس فریم‌لس آلومینیومی و نئون‌فلکسی کافه باران",
            startDate = dateToday,
            status = "تکمیل شده",
            description = "طراحی و اجرای تابلوهای داخلی منو و تابلوی سردر ورودی کافه",
            userId = "admin"
        )

        val projects = listOf(project1, project2, project3)
        database.projectDao().insertProjects(projects)

        // 3. Project Specs
        val specs = listOf(
            ProjectSpecEntity(
                id = "sample_spec_1",
                projectId = project1.id,
                subTitle = "حروف برجسته اصلی طلا و جواهر",
                signType = "چلنیوم",
                lengthCm = 450.0,
                widthCm = 90.0,
                depthCm = 9.0,
                description = "لبه چلنیوم طلایی پانچ‌دار با پلکسی دو لایه مشکی و طلایی و اس‌ام‌دی آفتابی",
                userId = "admin"
            ),
            ProjectSpecEntity(
                id = "sample_spec_2",
                projectId = project1.id,
                subTitle = "لوگوی الماس نوری",
                signType = "چلنیوم",
                lengthCm = 80.0,
                widthCm = 80.0,
                depthCm = 10.0,
                description = "برش لیزری طرح الماس با نورپردازی هفت‌رنگ RGB و کنترلر ریموت‌دار",
                userId = "admin"
            ),
            ProjectSpecEntity(
                id = "sample_spec_3",
                projectId = project2.id,
                subTitle = "نمای کامپوزیت پس‌زمینه",
                signType = "کامپوزیت",
                lengthCm = 900.0,
                widthCm = 240.0,
                depthCm = 15.0,
                description = "ورق کامپوزیت ضدحریق آلومکس با قوطی‌کشی آهن ۴۰×۴۰ و ضدزنگ صنعتی",
                userId = "admin"
            ),
            ProjectSpecEntity(
                id = "sample_spec_4",
                projectId = project2.id,
                subTitle = "حروف استیل میرور طلایی",
                signType = "استیل طلایی",
                lengthCm = 650.0,
                widthCm = 110.0,
                depthCm = 5.0,
                description = "استیل نگیر گرید ۳۰۴ ضخامت ۰.۸ با نور مخفی مهتابی (پایه اسپیسر)",
                userId = "admin"
            ),
            ProjectSpecEntity(
                id = "sample_spec_5",
                projectId = project3.id,
                subTitle = "لایت‌باکس ورودی کافه",
                signType = "لایت‌باکس",
                lengthCm = 220.0,
                widthCm = 120.0,
                depthCm = 6.0,
                description = "پروفیل آلومینیوم فریم‌لس آنودایز شده با پارچه تکستایل بک‌لایت قابل تعویض",
                userId = "admin"
            )
        )
        database.projectDao().insertSpecs(specs)

        // 4. Invoices
        val invoice1 = InvoiceEntity(
            id = "sample_inv_1",
            projectId = project1.id,
            issueDate = dateTwoWeeksAgo,
            discount = 1500000L,
            description = "فاکتور رسمی ساخت و اجرای تابلوی چلنیوم طلافروشی رحمانی با گارانتی ۲ ساله",
            userId = "admin"
        )
        val invoice2 = InvoiceEntity(
            id = "sample_inv_2",
            projectId = project2.id,
            issueDate = dateWeekAgo,
            discount = 3000000L,
            description = "فاکتور قطعی زیرسازی کامپوزیت و حروف استیل برج نیایش",
            userId = "admin"
        )
        val invoice3 = InvoiceEntity(
            id = "sample_inv_3",
            projectId = project3.id,
            issueDate = dateToday,
            discount = 500000L,
            description = "فاکتور لایت‌باکس و نئون‌فلکسی کافه باران",
            userId = "admin"
        )

        val invoices = listOf(invoice1, invoice2, invoice3)
        database.invoiceDao().insertInvoices(invoices)

        // 5. Invoice Items
        val items = listOf(
            // Invoice 1 items
            InvoiceItemEntity(
                id = "sample_item_1",
                invoiceId = invoice1.id,
                serviceType = "ساخت حروف برجسته چلنیوم",
                itemDescription = "محیط حروف چلنیوم طلایی با پلکسی تایوان و لبه پانچ",
                quantity = 38.0,
                unitPrice = 950000L,
                calculatedTotal = 36100000L,
                userId = "admin"
            ),
            InvoiceItemEntity(
                id = "sample_item_2",
                invoiceId = invoice1.id,
                serviceType = "زیرسازی ورق کامپوزیت",
                itemDescription = "اجرای کامپوزیت پرمیوم‌باند با قوطی‌کشی فلزی",
                quantity = 12.0,
                unitPrice = 1250000L,
                calculatedTotal = 15000000L,
                userId = "admin"
            ),
            InvoiceItemEntity(
                id = "sample_item_3",
                invoiceId = invoice1.id,
                serviceType = "ترانس و پاور سوئیچینگ",
                itemDescription = "ترانس فن‌دار ۳۰ آمپر ضدآب با فیوز محافظ",
                quantity = 3.0,
                unitPrice = 1150000L,
                calculatedTotal = 3450000L,
                userId = "admin"
            ),
            InvoiceItemEntity(
                id = "sample_item_4",
                invoiceId = invoice1.id,
                serviceType = "نصب و راه‌اندازی",
                itemDescription = "اجرت نصب کامل در محل با داربست و سیم‌کشی استاندارد",
                quantity = 1.0,
                unitPrice = 5000000L,
                calculatedTotal = 5000000L,
                userId = "admin"
            ),
            // Invoice 2 items
            InvoiceItemEntity(
                id = "sample_item_5",
                invoiceId = invoice2.id,
                serviceType = "ساخت حروف استیل",
                itemDescription = "حروف استیل طلایی میرور نگیر ۳۰۴ با نور اندرانیک",
                quantity = 45.0,
                unitPrice = 1400000L,
                calculatedTotal = 63000000L,
                userId = "admin"
            ),
            InvoiceItemEntity(
                id = "sample_item_6",
                invoiceId = invoice2.id,
                serviceType = "زیرسازی ورق کامپوزیت",
                itemDescription = "کامپوزیت دودی آلومکس مقاوم در برابر حرارت و نور خورشید",
                quantity = 26.0,
                unitPrice = 1300000L,
                calculatedTotal = 33800000L,
                userId = "admin"
            ),
            // Invoice 3 items
            InvoiceItemEntity(
                id = "sample_item_7",
                invoiceId = invoice3.id,
                serviceType = "ساخت لایت‌باکس فریم‌لس",
                itemDescription = "پروفیل آلومینیومی با چاپ پارچه‌ای یووی و ال‌ای‌دی لنزدار",
                quantity = 8.0,
                unitPrice = 1600000L,
                calculatedTotal = 12800000L,
                userId = "admin"
            ),
            InvoiceItemEntity(
                id = "sample_item_8",
                invoiceId = invoice3.id,
                serviceType = "نئون فلکسی سیلیکونی",
                itemDescription = "طراحی و مونتاژ نوشته‌های نئون روی پلکسی شفاف ۶ میل",
                quantity = 14.0,
                unitPrice = 650000L,
                calculatedTotal = 9100000L,
                userId = "admin"
            )
        )
        database.invoiceDao().insertInvoiceItems(items)

        // 6. Expenses
        val expenses = listOf(
            ExpenseEntity(
                id = "sample_exp_1",
                invoiceId = invoice1.id,
                category = "ورق پلکسی گلاس",
                amount = 4800000L,
                date = dateTwoWeeksAgo,
                description = "خرید ۲ ورق پلکسی یرلانگ تایوان ضخامت ۳ میل طلایی و مشکی",
                userId = "admin"
            ),
            ExpenseEntity(
                id = "sample_exp_2",
                invoiceId = invoice1.id,
                category = "بلوک اس‌ام‌دی (SMD)",
                amount = 3200000L,
                date = dateTwoWeeksAgo,
                description = "خرید ۵۰۰ عدد اس‌ام‌دی آفتابی سامسونگ لنزدار ضدآب",
                userId = "admin"
            ),
            ExpenseEntity(
                id = "sample_exp_3",
                invoiceId = invoice2.id,
                category = "کرایه جرثقیل و بالابر",
                amount = 3500000L,
                date = dateWeekAgo,
                description = "هزینه استقرار جرثقیل سبددار جهت نصب حروف در ارتفاع طبقه سوم برج",
                userId = "admin"
            ),
            ExpenseEntity(
                id = "sample_exp_4",
                invoiceId = null,
                category = "چسب و ملزومات مونتاژ",
                amount = 850000L,
                date = dateToday,
                description = "خرید چسب کلروفرم، چسب ۱۲۳ و پیچ‌های سرمته و رولپلاک کارگاه",
                userId = "admin"
            )
        )
        database.financialDao().insertExpenses(expenses)

        // 7. Payments
        val payments = listOf(
            PaymentEntity(
                id = "sample_pay_1",
                customerId = customer1.id,
                projectId = project1.id,
                type = "RECEIPT",
                amount = 28000000L,
                date = dateTwoWeeksAgo,
                method = "کارت به کارت",
                trackingCode = "64829103",
                description = "پیش‌پرداخت اولیه سفارش تابلوی طلافروشی",
                userId = "admin"
            ),
            PaymentEntity(
                id = "sample_pay_2",
                customerId = customer2.id,
                projectId = project2.id,
                type = "RECEIPT",
                amount = 45000000L,
                date = dateWeekAgo,
                method = "حواله پایا",
                trackingCode = "PAYA-9018442",
                description = "پیش‌پرداخت قرارداد نمای کامپوزیت برج نیایش",
                userId = "admin"
            ),
            PaymentEntity(
                id = "sample_pay_3",
                customerId = customer3.id,
                projectId = project3.id,
                type = "RECEIPT",
                amount = 21400000L,
                date = dateToday,
                method = "دستگاه پوز",
                trackingCode = "POS-100982",
                description = "تسویه کامل فاکتور لایت‌باکس و نئون کافه باران",
                userId = "admin"
            )
        )
        database.financialDao().insertPayments(payments)

        // 8. Checks
        val checks = listOf(
            CheckEntity(
                id = "sample_chk_1",
                customerId = customer1.id,
                projectId = project1.id,
                bankName = "بانک ملت",
                branch = "شعبه بازار بزرگ",
                checkNumber = "490182",
                sayadNumber = "1829304192834019",
                amount = 30050000L,
                dueDate = checkDueDate1,
                status = "PENDING",
                description = "چک صیادی بابت مانده حساب فاکتور طلافروشی رحمانی",
                userId = "admin"
            ),
            CheckEntity(
                id = "sample_chk_2",
                customerId = customer2.id,
                projectId = project2.id,
                bankName = "بانک صادرات",
                branch = "شعبه میدان ونک",
                checkNumber = "772109",
                sayadNumber = "7788990011223344",
                amount = 48800000L,
                dueDate = checkDueDate2,
                status = "PENDING",
                description = "چک مرحله دوم تحویل و نصب کامپوزیت مجتمع نیایش",
                userId = "admin"
            ),
            CheckEntity(
                id = "sample_chk_3",
                customerId = customer3.id,
                projectId = project3.id,
                bankName = "بانک ملی ایران",
                branch = "شعبه وصال شیرازی",
                checkNumber = "120934",
                sayadNumber = "5544332211009988",
                amount = 10000000L,
                dueDate = checkDueDate3,
                status = "CLEARED",
                description = "چک تضمین بیعانه کافه باران (وصول شده)",
                userId = "admin"
            )
        )
        database.financialDao().insertChecks(checks)

        // 9. Contracts
        val contract1 = ContractEntity(
            id = "sample_cont_1",
            customerId = customer1.id,
            projectId = project1.id,
            contractDate = dateTwoWeeksAgo,
            durationDays = 15,
            totalAmount = 59550000L,
            discount = 1500000L,
            prepayment = 28000000L,
            balanceAmount = 30050000L,
            termsJson = "ساخت حروف چلنیوم طلایی با گارانتی ۳۶ ماهه نور و ترانس|تحویل و نصب بدون آسیب به نمای اصلی ساختمان|تسویه نهایی پس از تست کامل روشنایی",
            arbitratorName = "اتحادیه تابلوسازان و تابلونویسان",
            description = "قرارداد رسمی تابلوسازی کارگاه آلپا با طلا و جواهر رحمانی",
            userId = "admin"
        )
        val contract2 = ContractEntity(
            id = "sample_cont_2",
            customerId = customer2.id,
            projectId = project2.id,
            contractDate = dateWeekAgo,
            durationDays = 20,
            totalAmount = 96800000L,
            discount = 3000000L,
            prepayment = 45000000L,
            balanceAmount = 48800000L,
            termsJson = "اجرای نمای کامپوزیت با ورق آلومکس ضدحریق|نصب حروف استیل نگیر با فاصله ۲ سانتی‌متر از نما|رعایت تمامی ضوابط ایمنی کار در ارتفاع",
            arbitratorName = "مهندس ناظر پروژه و کارشناس رسمی",
            description = "قرارداد رسمی تابلوسازی با مدیریت برج نیایش",
            userId = "admin"
        )
        database.contractDao().insertContracts(listOf(contract1, contract2))

        // Log audit
        database.auditDao().insertAuditLog(
            AuditLogEntity(
                entityType = "System",
                entityId = "SampleData",
                action = "CREATE",
                details = "ایجاد داده‌های فرضی تابلوسازی (۳ مشتری، ۳ پروژه، فاکتورها، چک‌ها و قراردادها)",
                userId = "admin"
            )
        )
    }

    suspend fun clearSampleData(database: AppDatabase) = withContext(Dispatchers.IO) {
        // Delete in order to satisfy foreign keys
        database.contractDao().deleteAllContracts()
        database.financialDao().deleteAllChecks()
        database.financialDao().deleteAllPayments()
        database.financialDao().deleteAllExpenses()
        database.invoiceDao().deleteAllInvoiceItems()
        database.invoiceDao().deleteAllInvoices()
        database.projectDao().deleteAllSpecs()
        database.projectDao().deleteAllProjects()
        database.customerDao().deleteAllCustomers()

        // Audit log
        database.auditDao().insertAuditLog(
            AuditLogEntity(
                entityType = "System",
                entityId = "Database",
                action = "DELETE",
                details = "پاک‌سازی داده‌های فرضی و آماده‌سازی کامل دیتابیس برای ثبت اطلاعات واقعی کارگاه",
                userId = "admin"
            )
        )
    }
}

# مستندات جامع معماری و مهاجرت به وب (Next.js)
## نرم‌افزار مدیریت کارگاه تابلوسازی آلپا (Alpa Signage Manager Web Edition)

این سند مرجع رسمی و ۱۰۰٪ کامل معماری، پایگاه داده، قوانین کسب‌وکار، سطوح دسترسی (RBAC) و استانداردهای رابط کاربری جهت پیاده‌سازی نسخه تحت وب سیستم مدیریت کارگاه تابلوسازی آلپا با فریم‌ورک **Next.js** در یک پروژه جدید است. تمامی جزئیات فنی بر اساس نسخه پایدار اندروید، فایل توافقنامه استانداردهای کاربری (`AGREEMENT.md`) و تصمیمات مهندسی (`ARCHITECTURE.md`) استخراج و منطبق بر استانداردهای مدرن وب بازآرایی شده است.

---

## ۱. نقشه راه و مقایسه پلتفرم (Platform Migration Overview)

| مؤلفه | نسخه اندروید (موجود) | نسخه وب (Next.js - پروژه جدید) | نکات معماری |
| :--- | :--- | :--- | :--- |
| **زبان و فریم‌ورک** | Kotlin + Jetpack Compose | TypeScript + Next.js 15+ (App Router) + React | ماژولار، کامپوننت‌محور و تایپ‌سیف |
| **استایل و تم** | Material 3 Theme (`Color.kt`, `Theme.kt`) | Tailwind CSS + CSS Variables + Dark/Light Theme | استفاده از پالت رنگی اصلی آلپا (Amber/Orange و Blue) |
| **فونت و زبان** | فونت بومی `Vazirmatn.ttf` با جهت‌بندی RTL | فونت وزیرمتن با جهت‌بندی سراسری `dir="rtl"` | اعمال فونت روی تگ `<html>` و متون فارسی |
| **پایگاه داده** | Room (SQLite محلی گوشی) | **Prisma ORM** متصل به SQLite (یا PostgreSQL) | آماده برای استفاده چندکاربره هم‌زمان در کارگاه و دفتر |
| **احراز هویت و دسترسی** | پین محلی و سنسور اثرانگشت سخت‌افزاری | **احراز هویت امن وب (Session/JWT) با نقش‌ها (RBAC)** | ۴ نقش مستقل با دسترسی‌های تفکیک‌شده |
| **تقویم و تاریخ** | کلاس اختصاصی `JalaliDate.kt` | کتابخانه استاندارد `jalaali-js` یا `date-fns-jalali` | انتخابگر تاریخ شمسی با قابلیت انتخاب سال، ماه و روز |
| **چاپ و فاکتور** | بوم گرافیکی `PdfGenerator.kt` | استایل استاندارد چاپی مرورگر (`@media print`) یا `@react-pdf` | خروجی بی‌نقص برگه A4 قرارداد و فاکتور بدون افت کیفیت |
| **آپلود تصاویر** | ذخیره آدرس محلی در حافظه دستگاه | ذخیره فایل در مسیر سرور (`/public/uploads`) یا باکت ابری | نمایش زنده طرح تابلو و عکس نمونه کار برای تمامی اعضا |

---

## ۲. ساختار پایگاه داده و مدل‌ها (Prisma Schema Reference)

پایگاه داده به صورت رابطه‌ای با رعایت تمامی کلیدهای خارجی (Foreign Keys)، حذف نرم (Soft Delete با `isDeleted`) و شناسه‌های نامتمرکز (UUID/CUID) به شرح زیر پیاده‌سازی می‌شود:

```prisma
datasource db {
  provider = "sqlite" // یا postgresql برای سرورهای آنلاین
  url      = env("DATABASE_URL")
}

generator client {
  provider = "prisma-client-js"
}

// ۱. جدول کاربران و نقش‌ها (RBAC)
model User {
  id           String      @id @default(uuid())
  fullName     String
  username     String      @unique
  passwordHash String
  role         String      @default("WORKSHOP_STAFF") // ADMIN, WORKSHOP_STAFF, DESIGNER, ACCOUNTANT
  isActive     Boolean     @default(true)
  createdAt    DateTime    @default(now())
  updatedAt    DateTime    @updatedAt
  auditLogs    AuditLog[]
}

// ۲. جدول مشتریان و کارفرمایان
model Customer {
  id           String      @id @default(uuid())
  name         String
  phone        String      // استاندارد ۰۹...
  companyName  String?     @default("")
  address      String?     @default("")
  nationalCode String?     @default("")
  description  String?     @default("")
  createdAt    DateTime    @default(now())
  updatedAt    DateTime    @updatedAt
  isDeleted    Boolean     @default(false)
  userId       String      @default("admin")

  projects     Project[]
  payments     Payment[]
  checks       Check[]
  contracts    Contract[]
}

// ۳. جدول مراحل ۵گانه استاندارد کارگاه تابلوسازی
model ProjectStage {
  id           String      @id @default(uuid())
  name         String      // مذاکره، طراحی، ساخت، نصب، تسویه
  description  String      @default("")
  displayOrder Int         // ۱ تا ۵ جهت ترتیب استپ‌پروگرس
  colorHex     String      @default("#FF9800")
  isDefault    Boolean     @default(false)
  createdAt    DateTime    @default(now())
  updatedAt    DateTime    @updatedAt
  isDeleted    Boolean     @default(false)
}

// ۴. جدول پروژه‌ها و سفارشات تابلو
model Project {
  id                String        @id @default(uuid())
  customerId        String
  customer          Customer      @relation(fields: [customerId], references: [id])
  title             String
  startDate         String        // تاریخ جلالی مانند ۱۴۰۳/۰۸/۲۰
  status            String        @default("در حال ساخت") // ACTIVE (در حال ساخت), PENDING (معلق), COMPLETED (تکمیل شده), CANCELLED (لغو شده)
  stageId           String?       // شناسه مرحله فعال فعلی
  completedStageIds String        @default("") // شناسه‌های مراحل تکمیل‌شده به صورت رشته جداشده با کاما
  description       String?       @default("")
  createdAt         DateTime      @default(now())
  updatedAt         DateTime      @updatedAt
  isDeleted         Boolean       @default(false)
  userId            String        @default("admin")

  specs             ProjectSpec[]
  invoice           Invoice?
  payments          Payment[]
  checks            Check[]
  expenses          Expense[]
  contract          Contract?
}

// ۵. جدول مشخصات فنی ساخت تابلو (ابعاد و متریال)
model ProjectSpec {
  id             String      @id @default(uuid())
  projectId      String
  project        Project     @relation(fields: [projectId], references: [id], onDelete: Cascade)
  subTitle       String      // عنوان بخش: حروف اصلی، لوگو، لایت‌باکس ورودی، زیرسازی کامپوزیت
  signType       String      // چلنیوم، استیل طلایی، پلاستیک (نئون)، نئون فلکسی، لایت‌باکس، کامپوزیت، ترمووود
  lengthCm       Float       // طول (سانتی‌متر)
  widthCm        Float       // عرض/ارتفاع (سانتی‌متر)
  depthCm        Float       @default(0.0) // عمق لبه چلنیوم یا قوطی
  attachmentUrls String      @default("") // آدرس تصاویر و طرح‌ها به صورت کاما یا JSON
  description    String?     @default("")
  createdAt      DateTime    @default(now())
  updatedAt      DateTime    @updatedAt
  isDeleted      Boolean     @default(false)
  userId         String      @default("admin")
}

// ۶. جدول صورتحساب و فاکتور پروژه
model Invoice {
  id          String        @id @default(uuid())
  projectId   String        @unique
  project     Project       @relation(fields: [projectId], references: [id])
  issueDate   String        // تاریخ شمسی
  discount    BigInt        @default(0) // مبلغ تخفیف به ریال/تومان
  description String?       @default("")
  createdAt   DateTime      @default(now())
  updatedAt   DateTime      @updatedAt
  isDeleted   Boolean       @default(false)
  userId      String        @default("admin")

  items       InvoiceItem[]
  expenses    Expense[]
}

// ۷. جدول اقلام ریز فاکتور تابلو
model InvoiceItem {
  id              String      @id @default(uuid())
  invoiceId       String
  invoice         Invoice     @relation(fields: [invoiceId], references: [id], onDelete: Cascade)
  serviceType     String      // ساخت حروف چلنیوم، زیرسازی کامپوزیت، منبع تغذیه (پاور)، اس‌ام‌دی، نصب و راه‌اندازی
  itemDescription String?     @default("")
  quantity        Float       // متراژ محیطی، تعداد یا متر مربع
  unit            String      @default("متر") // متر، عدد، متر مربع، سانتی‌متر
  unitPrice       BigInt      // قیمت واحد
  calculatedTotal BigInt      // محاسبه خودکار: quantity * unitPrice
  description     String?     @default("")
  createdAt       DateTime    @default(now())
  updatedAt       DateTime    @updatedAt
  isDeleted       Boolean     @default(false)
  userId          String      @default("admin")
}

// ۸. جدول هزینه‌ها (تفکیک هزینه‌های کارگاهی و فاکتور)
model Expense {
  id          String      @id @default(uuid())
  invoiceId   String?     // مرتبط با فاکتور در صورت وجود
  invoice     Invoice?    @relation(fields: [invoiceId], references: [id])
  projectId   String?     // مرتبط با پروژه
  project     Project?    @relation(fields: [projectId], references: [id])
  expenseType String      @default("PROJECT") // "INVOICE" (هزینه فاکتور مشتری) یا "PROJECT" (هزینه کارگاهی و سود/زیان)
  category    String      // ورق پلکسی، رول چلنیوم، اس‌ام‌دی، ترانس، داربست، جرثقیل، دستمزد کارگر، ایاب‌ذهاب
  amount      BigInt      // مبلغ هزینه
  date        String      // تاریخ شمسی
  description String?     @default("")
  createdAt   DateTime    @default(now())
  updatedAt   DateTime    @updatedAt
  isDeleted   Boolean     @default(false)
  userId      String      @default("admin")
}

// ۹. جدول دریافت‌ها و پرداخت‌های مالی (نقد، واریزی، پوز)
model Payment {
  id           String      @id @default(uuid())
  customerId   String
  customer     Customer    @relation(fields: [customerId], references: [id])
  projectId    String?
  project      Project?    @relation(fields: [projectId], references: [id])
  type         String      @default("RECEIPT") // RECEIPT (دریافتی از مشتری), PAYMENT (پرداختی/استرداد به مشتری)
  amount       BigInt
  date         String      // تاریخ شمسی
  method       String      @default("کارت به کارت") // نقدی، کارت به کارت، حواله پایا، دستگاه پوز
  trackingCode String?     @default("")
  description  String?     @default("")
  createdAt    DateTime    @default(now())
  updatedAt    DateTime    @updatedAt
  isDeleted    Boolean     @default(false)
  userId       String      @default("admin")
}

// ۱۰. جدول چک‌های صیادی و وضعیت آنها
model Check {
  id          String      @id @default(uuid())
  customerId  String
  customer    Customer    @relation(fields: [customerId], references: [id])
  projectId   String?
  project     Project?    @relation(fields: [projectId], references: [id])
  bankName    String?     @default("")
  branch      String?     @default("")
  checkNumber String?     @default("")
  sayadNumber String?     @default("") // شناسه صیادی ۱۶ رقمی
  amount      BigInt
  dueDate     String      // تاریخ سررسید شمسی
  status      String      @default("PENDING") // PENDING (در جریان وصول), CLEARED (وصول شده), BOUNCED (برگشت خورده), RETURNED (عودت داده شده)
  description String?     @default("")
  createdAt   DateTime    @default(now())
  updatedAt   DateTime    @updatedAt
  isDeleted   Boolean     @default(false)
  userId      String      @default("admin")
}

// ۱۱. جدول قراردادهای رسمی تابلوسازی
model Contract {
  id             String      @id @default(uuid())
  customerId     String
  customer       Customer    @relation(fields: [customerId], references: [id])
  projectId      String      @unique
  project        Project     @relation(fields: [projectId], references: [id])
  contractDate   String      // تاریخ عقد قرارداد
  durationDays   Int         @default(15) // مدت زمان اجرا (روز کاری)
  totalAmount    BigInt      @default(0)
  discount       BigInt      @default(0)
  prepayment     BigInt      @default(0) // پیش‌پرداخت
  balanceAmount  BigInt      @default(0) // مانده
  termsJson      String?     @default("") // بندهای حقوقی قرارداد به فرمت JSON
  arbitratorName String      @default("مرضی الطرفین")
  description    String?     @default("")
  createdAt      DateTime    @default(now())
  updatedAt      DateTime    @updatedAt
  isDeleted      Boolean     @default(false)
  userId         String      @default("admin")
}

// ۱۲. عناوین سفارشی هزینه‌ها
model ExpenseCategory {
  id            String      @id @default(uuid())
  title         String      @unique
  defaultAmount BigInt      @default(0)
  description   String?     @default("")
  createdAt     DateTime    @default(now())
  updatedAt     DateTime    @updatedAt
  isDeleted     Boolean     @default(false)
  userId        String      @default("admin")
}

// ۱۳. سابقه فعالیت‌ها و وقایع سیستم (Audit Logs)
model AuditLog {
  id         String      @id @default(uuid())
  entityType String      // Customer, Project, Invoice, Payment, Check, Contract, Expense
  entityId   String
  action     String      // CREATE, UPDATE, DELETE, RESTORE
  details    String
  userId     String      @default("admin")
  user       User?       @relation(fields: [userId], references: [id])
  timestamp  DateTime    @default(now())
}

// ۱۴. تنظیمات عمومی کارگاه و پیمانکار
model WorkshopSettings {
  id                 String   @id @default("default")
  currencyUnit       String   @default("TOMAN") // TOMAN یا RIAL
  contractorName     String   @default("امیرحسین حسنخانی")
  businessName       String   @default("تابلوسازی تبلیغاتی آلپا")
  address            String   @default("فردوسی ۲، نبش خیابان فاضل")
  phone              String   @default("09144862950")
  accountNumber      String   @default("0115396517004")
  shabaNumber        String   @default("IR250170000000115396517004")
  cardNumber         String   @default("6037998209583854")
  bankName           String   @default("بانک ملی ایران - امیرحسین حسنخانی")
  warnUnsavedChanges Boolean  @default(true)
  showPlaceholders   Boolean  @default(true)
}
```

---

## ۳. ماتریس سطوح دسترسی و نقش‌های کاربری (RBAC)

در نسخه وب، به دلیل چندکاربره بودن و کارکرد همزمان چندین پرسنل کارگاه، ۴ نقش تعریف شده و دسترسی‌های آن‌ها بر اساس نیاز کارگاه به شرح زیر محدود می‌شود:

| بخش‌های برنامه | مدیر کل (ADMIN) | نصاب و سرپرست کارگاه (WORKSHOP_STAFF) | طراح تابلو (DESIGNER) | حسابدار (ACCOUNTANT) |
| :--- | :---: | :---: | :---: | :---: |
| **داشبورد و آمار کلان مالی** | دسترسی کامل | ❌ غیرمجاز (پنهان) | ❌ غیرمجاز (پنهان) | دسترسی کامل |
| **مشاهده لیست مشتریان** | دسترسی کامل | فقط نام و تلفن (بدون مانده حساب) | اطلاعات عمومی | دسترسی کامل همراه مانده |
| **ثبت و ویرایش مشتری** | ✅ بله | ❌ غیرمجاز | ❌ غیرمجاز | ✅ بله |
| **پرونده و مراحل پروژه** | دسترسی کامل | **تغییر تیک مراحل ساخت و نصب** | مشاهده وضعیت پروژه | مشاهده اطلاعات کلی |
| **مشخصات فنی تابلو (ابعاد و متریال)** | دسترسی کامل | مشاهده ابعاد و نقشه‌ها | **ثبت ابعاد، متریال و طرح‌ها** | فقط مشاهده متراژ |
| **صدور فاکتور و اقلام** | دسترسی کامل | ❌ مخفی | ❌ مخفی | **دسترسی کامل و صدور فاکتور** |
| **امور مالی و ثبت دریافت/چک** | دسترسی کامل | ❌ مخفی | ❌ مخفی | **دسترسی کامل** |
| **ثبت هزینه‌های خرید کارگاهی** | دسترسی کامل | **ثبت هزینه خرید فوری (با تأیید)** | ❌ مخفی | دسترسی کامل |
| **تنظیم و چاپ قرارداد حقوقی** | دسترسی کامل | ❌ مخفی | ❌ مخفی | **تنظیم و چاپ قرارداد** |
| **سطل زباله و بازیابی اطلاعات** | دسترسی کامل | ❌ مخفی | ❌ مخفی | فقط مشاهده سوابق |
| **تنظیمات سیستم و مدیریت کاربران** | دسترسی کامل | ❌ مخفی | ❌ مخفی | ❌ مخفی |

---

## ۴. منطق کسب‌وکار، فرمول‌ها و محاسبات مالی (Business Logic)

### ۴.۱. ماشین وضعیت هوشمند پروژه‌ها (Project State Machine)
* **۵ مرحله پیش‌فرض کارگاه:**
  1. **مذاکره** (مذاکره و تاییدیه) - رنگ: `#2196F3` (آبی)
  2. **طراحی** (طراحی و شبیه‌سازی) - رنگ: `#9C27B0` (بنفش)
  3. **ساخت** (ساخت و مونتاژ کارگاه) - رنگ: `#FF9800` (نارنجی/کهربایی)
  4. **نصب** (نصب و راه‌اندازی) - رنگ: `#4CAF50` (سبز)
  5. **تسویه** (تسویه حساب و تحویل) - رنگ: `#009688` (سبز یشمی)
* **قانون تکمیل خودکار:** هرگاه تمامی مراحل اجرایی فعال پروژه تیک بخورند، وضعیت کلان پروژه به صورت خودکار به `COMPLETED` (تکمیل شده) تغییر می‌کند.
* **استثنای حیاتی:** اگر وضعیت پروژه «معلق» (`PENDING`) یا «لغو شده» (`CANCELLED`) باشد، تغییر دستی مراحل باعث تغییر وضعیت خودکار نخواهد شد.
* **مرحله فعال جاری:** اولین مرحله تکمیل‌نشده بر اساس شماره ترتیب (`displayOrder`) به عنوان مرحله جاری شناسایی شده و در کارت پروژه و پرونده با پیشوند «در مرحله [عنوان مرحله]» نمایش می‌یابد.

### ۴.۲. فرمت‌بندی و اعتبارسنجی ورودی‌ها
1. **شماره تماس (تلفن همراه و ثابت):**
   * هنگام ورود یا Paste، پیشوندهای `+98`، `0098` یا `98` و فاصله‌ها فوراً حذف شوند.
   * رقم اول همیشه عدد `0` باشد.
   * فرمت نمایشی برای موبایل (شروع با 09): الگوی ۴-۳-۴ (`0914 111 2222`).
   * فرمت نمایشی برای تلفن ثابت (شروع با 0 غیر از 9): الگوی ۳-۴-۴ (`021 1111 2222`).
   * برای جلوگیری از به هم ریختگی اعداد در زبان فارسی، از کنترلر جهت‌بندی متنی LTR درون کادر یا ایزولاسیون یونیکد (`\u2066` و `\u2069`) استفاده شود.
2. **کد ملی (اختیاری):**
   * محدود به حداکثر ۱۰ رقم عددی.
   * در صورت ورود، با الگوریتم استاندارد ماژول ۱۱ ثبت احوال ایران اعتبارسنجی شده و در صورت عدم تطابق، خطای درجا (Inline Error) نمایش داده شود.
3. **مبالغ پولی و واحد پول:**
   * در دیتابیس مبالغ به صورت عدد خام بزرگ (`BigInt` یا `number`) ذخیره می‌شوند.
   * در فیلدهای ورودی مبلغ، ارقام با جداکننده هزارگان (۳ رقم ۳ رقم) نمایش داده شوند.
   * عنوان فیلد ورودی نباید شامل کلمه «تومان» یا «ریال» باشد؛ نام واحد پولی فعال سیستم صرفاً در پسوند انتهایی کادر ورودی (Suffix) نمایش می‌یابد.
   * تبدیل خودکار مبالغ به حروف فارسی در زیر فیلد ورودی برای جلوگیری از اشتباه تایپی در تعداد صفرها.

### ۴.۳. محاسبات فاکتور و تراز مالی مشتری
* **مبلغ کل ردیف فاکتور:** `calculatedTotal = quantity * unitPrice`
* **مجموع فاکتور:** `SubTotal = Sum(calculatedTotal) - discount`
* **مجموع دریافتی‌های نقدی:** مجموع مبالغ جدول Payments با `type = 'RECEIPT'`
* **مجموع پرداختی‌های نقدی:** مجموع مبالغ جدول Payments با `type = 'PAYMENT'`
* **چک‌های وصول‌شده:** مبالغ چک‌های با `status = 'CLEARED'`
* **تراز مانده حساب کارفرما (Balance):**
  $$\text{مانده بدهی} = (\text{جمع فاکتورها} + \text{پرداختی‌ها به مشتری}) - (\text{دریافتی‌های نقدی} + \text{چک‌های وصول‌شده})$$
  * اگر حاصل مثبت باشد: بدهکار (رنگ هشدار/قرمز با عنوان «مانده بدهی»).
  * اگر حاصل صفر باشد: تسویه کامل (رنگ سبز).
  * اگر حاصل منفی باشد: بستانکار.

---

## ۵. استانداردهای رابط کاربری وب منطبق بر توافقنامه (AGREEMENT.md)

۱. **جهت‌بندی و تایپوگرافی:**
   * جهت‌بندی سراسری راست‌به‌چپ (`dir="rtl"`).
   * فونت پیش‌فرض تمامی صفحات، مودال‌ها، جداول و چاپ: **Vazirmatn** با اوزان متوازن.

۲. **ساختار فرم‌ها و دیالوگ‌ها (Forms & Modals):**
   * فرم‌های اصلی و دیالوگ‌های ثبت (مشتری جدید، پروژه جدید، تراکنش، چک، مشخصه تابلو) در صفحات بزرگ دسکتاپ به صورت Sheet/Drawer کشویی از سمت راست یا دیالوگ تمام‌ارتفاع باز شوند.
   * در نسخه موبایل به صورت Modal Bottom Sheet تمام‌عرض بالا بیایند.
   * دیالوگ‌های مرکزی (Modal Alert) منحصراً برای تأییدیه‌های کوتاه مانند حذف و بازیابی استفاده شوند.

۳. **نوار پایینی ثابت دکمه‌ها (Sticky Bottom Action Bar):**
   * در انتهای تمامی فرم‌های ثبت و ویرایش، نوار دکمه‌های پایینی ثابت قرار گیرد (با اسکرول جابه‌جا نشود).
   * **نسبت ۲ به ۱ دکمه‌ها:**
     * دکمه سمت راست: دکمه اصلی (Primary) با عرض دو برابر به رنگ اصلی سیستم؛ با برچسب مشخص «ذخیره + عنوان موجودیت» (مثلاً: «ذخیره پروژه»، «ذخیره مشتری»).
     * دکمه سمت چپ: دکمه انصراف (Cancel) با عرض نصف دکمه اصلی، به صورت خنثی/خطی.
   * **کنترل وضعیت تغییر (Dirty State):** دکمه ذخیره در ابتدا غیرفعال (Disabled) باشد و تنها زمانی فعال شود که کاربر حداقل یکی از فیلدها را تغییر داده باشد.

۴. **گارد تغییرات ذخیره‌نشده (Unsaved Changes Guard):**
   * اگر فرم در حالت Dirty باشد و کاربر بخواهد با بستن پنجره، کلیک خارج از کادر (Scrim) یا دکمه برگشت خارج شود، یک مودال هشدار باز شود.
   * در این مودال دکمه «ادامه ویرایش» به رنگ سبز و دکمه «خروج بدون ذخیره» به رنگ قرمز با فاصله‌گذاری متناسب تعبیه شود.
   * فشردن دکمه «انصراف» در نوار پایینی به معنای خروج آگاهانه بوده و دیالوگ اضافه نشان نمی‌دهد.

۵. **نوار جستجوی متمرکز و فیلترها:**
   * نوار جستجو در بالای تمامی صفحات اصلی (مشتریان، پروژه‌ها، مالی، قراردادها) با بردر ظریف و انیمیشن فوکوس (تغییر رنگ بردر به رنگ تم).
   * فیلترهای کپسولی (Status Chips) برای فیلتر سریع بر اساس مراحل کارگاهی و وضعیت‌ها.

۶. **کارت‌های شاخص‌های کلیدی (KPI Cards):**
   * در داشبورد و صفحه مشتریان: کارت‌های مدرن دو ستونه با گوشه‌های گرد، عنوان و آیکون در بالا، رقم درشت با رنگ‌های معنایی در وسط، و نوار نشانگر رنگی باریک در پایین کارت.

۷. **پرونده پروژه و استپ‌پروگرس‌بار عمودی (Vertical Step Progress):**
   * نمایش ۵ مرحله کارگاهی به صورت عمودی با خطوط اتصال.
   * مرحله تکمیل‌شده: دایره سبز با آیکون تیک.
   * مرحله جاری در دست اقدام: دایره با رنگ اختصاصی و افکت پالس حلقوی ملایم (Pulse Effect).
   * مراحل آتی: آیکون خاکستری خطی.

۸. **چاپ و فاکتور A4 استاندارد:**
   * طراحی برگه صدور فاکتور و قرارداد با سربرگ تابلوسازی آلپا، مشخصات پیمانکار، مشخصات کارفرما، جدول شکیل اقلام فاکتور با جمع مبالغ به عدد و حروف، شرایط پرداخت، محل مهر و امضای طرفین و سازگار با خروجی پرینت استاندارد (`window.print()`).

---

## ۶. پرامپت آماده و جامع برای پروژه جدید در AI Studio (Ready-to-Use Prompt)

کاربر گرامی، هنگام ساخت **پروژه جدید با پلتفرم Web (Next.js)** در Google AI Studio Build، کافی است متن زیر را به عنوان اولین پرامپت در چت کپی و ارسال کنید:

```text
سلام. من می‌خواهم یک وب‌اپلیکیشن کامل، بسیار شیک و مدرن برای «مدیریت کارگاه تابلوسازی تبلیغاتی آلپا» با Next.js 15 (App Router)، زبان TypeScript، استایل‌دهی Tailwind CSS، کامپوننت‌های Shadcn UI / Radix UI و پایگاه داده Prisma ORM بسازی.

لطفاً تمامی الزامات و مشخصات زیر را با بالاترین کیفیت پیاده‌سازی کن:

۱. طراحی و رابط کاربری (RTL & Styling):
- جهت‌بندی ۱۰۰٪ راست‌به‌چپ (dir="rtl") و پشتیبانی کامل از فونت فارسی وزیرمتن (Vazirmatn).
- پشتیبانی از تم تاریک و روشن (Dark/Light Mode) با پالت رنگی گرم کارگاهی (Amber/Orange و Blue).
- سایدبار ناوبری در دسکتاپ و Bottom Navigation Bar در موبایل شامل بخش‌های: داشبورد، پروژه‌ها، مشتریان، امور مالی، قراردادها، سطل زباله و تنظیمات.
- رعایت استانداردهای فرم: باز شدن شیت/دراور کشویی، نوار چسبان پایینی (Sticky Action Bar) با نسبت ۲ به ۱ دکمه‌ها (دکمه ذخیره دو برابر انصراف و فقط در صورت تغییر داده‌ها فعال شود)، و گارد جلوگیری از خروج ناخواسته با تغییرات ذخیره‌نشده (Unsaved Changes Guard با دکمه سبز ادامه ویرایش و قرمز خروج).

۲. مدیریت کاربران و نقش‌ها (RBAC):
- پیاده‌سازی سیستم احراز هویت با ۴ نقش:
  ۱) مدیر کل (ADMIN): دسترسی نامحدود به تمام بخش‌ها، مالی و تنظیمات.
  ۲) سرپرست کارگاه و نصاب (WORKSHOP_STAFF): دسترسی به پرونده پروژه‌ها، تغییر تیک مراحل ساخت و نصب، مشاهده ابعاد و نقشه‌های فنی، و ثبت هزینه کارگاه (بدون دسترسی به مبالغ سود/فاکتورها و تب‌های مالی).
  ۳) طراح تابلوسازی (DESIGNER): ثبت و مشاهده ابعاد تابلو، متریال، آپلود طرح‌ها و تصاویر تابلو.
  ۴) حسابدار (ACCOUNTANT): دسترسی کامل به فاکتورها، دریافتی‌ها، پرداخت‌ها، چک‌های صیادی، قراردادها و گزارشات مالی.

۳. مدیریت پروژه‌ها و مراحل ۵گانه کارگاه:
- ماشین وضعیت پروژه‌ها با ۵ مرحله کارگاهی: ۱. مذاکره، ۲. طراحی، ۳. ساخت، ۴. نصب، ۵. تسویه.
- پرونده پروژه با استپ‌پروگرس‌بار عمودی شکیل و پالس انیمیشنی برای مرحله در دست اقدام.
- قانون تکمیل خودکار: تکمیل تمام مراحل، وضعیت پروژه را خودکار به «تکمیل شده» تغییر می‌دهد.
- ثبت مشخصات فنی تابلو: ابعاد (طول، عرض، عمق بر حسب سانتی‌متر)، نوع تابلو (چلنیوم، استیل، نئون، لایت‌باکس، کامپوزیت)، آپلود عکس طرح و توضیحات فنی.

۴. امور مالی، فاکتورها و چک‌ها:
- صدور فاکتور و اقلام با محاسبه خودکار متراژ × قیمت واحد، اعمال تخفیف و پیش‌نمایش چاپی برگه استاندارد A4 فاکتور و قرارداد با دکمه پرینت/PDF.
- مدیریت چک‌های صیادی ۱۶ رقمی با وضعیت‌های در جریان، وصول‌شده، برگشتی و عودت‌داده‌شده.
- تفکیک هزینه‌های کارگاهی و هزینه‌های فاکتور شده.
- فرمت‌دهی خودکار ۳ رقمی مبالغ، تبدیل عدد به حروف فارسی در زیر فیلدها و اعتبارسنجی شماره تماس با فرمت ۴-۳-۴.
- سطل زباله جامع (Trash) برای بازیابی امن رکوردهای حذف‌شده.

لطفاً این سیستم را به همراه داده‌های اولیه نمونه (Seed Data) کارگاهی ایجاد کن.
```

---
این سند به عنوان نسخه رسمی مهاجرت تهیه شده و کلیه موازین معماری سیستم در آن محفوظ است.

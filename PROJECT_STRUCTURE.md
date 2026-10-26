# ساختار و نقشه‌برداری فنی پروژه (Project Structure)
## نرم‌افزار مدیریت کارگاه تابلوسازی آلپا (Alpa Signage Manager)

این سند نقشه رسمی و کامل ساختار پروژه، دایرکتوری‌ها، فایل‌ها، وظایف و ارتباطات معماری آنها است. طبق استاندارد الزامی بند ۷ سند توافقنامه (`AGREEMENT.md`)، این سند با ایجاد، جابه‌جایی یا حذف هر فایل یا پوشه به صورت مستمر و متناظر به‌روزرسانی می‌شود.

---

### ۱. فایل‌ها و پیکربندی‌های ریشه (Root Directory)

| مسیر کامل نسبی | وظیفه و کاربرد | مسئولیت اصلی و ارتباط با سایر بخش‌ها |
| :--- | :--- | :--- |
| `/PROJECT_STRUCTURE.md` | مستندات ساختار پروژه | مرجع راهنمای فایل‌ها، پکیج‌ها و مسئولیت‌ها بر اساس فایل‌های واقعی مخزن. |
| `/ARCHITECTURE.md` | سند تصمیمات معماری (ADR) | ثبت مستند تصمیمات مهندسی، استانداردها، ساختار لایه‌ها و قوانین پایداری. |
| `/AGREEMENT.md` | سند توافقنامه استانداردها | مرجع استانداردهای الزامی طراحی فرم‌ها، ناوبری، نوارهای سیستمی و رفتار برنامه. |
| `/AGREEMENT.txt` | نسخه متنی سند توافقنامه | رونوشت یکپارچه استانداردهای الزامی پروژه جهت سازگاری و مراجعات متنی. |
| `/metadata.json` | اطلاعات برنامه در پلتفرم AI Studio | کنترل نام برنامه، توضیحات و قابلیت‌های پلتفرمی در سایدبار هوش مصنوعی. |
| `/build.gradle.kts` | اسکریپت بیلد ریشه پروژه | تعریف پلاگین‌های سراسری اندروید و کاتلین در سطح پروژه. |
| `/settings.gradle.kts` | تنظیمات مخازن و ماژول‌ها | تعیین نام پروژه (`Alpa Signage Manager`) و مخازن Maven Central و Google. |
| `/gradle.properties` | تنظیمات حافظه و اجرای Gradle | پیکربندی بهینه‌سازی JVM، فشرده‌سازی بیلد و فعال‌سازی AndroidX. |
| `/.env.example` | الگوی متغیرهای محیطی | راهنمای پیکربندی متغیرهای امنیتی و کلیدهای مورد نیاز در صورت نیاز. |
| `/.gitignore` | نادیده‌گیری فایل‌های موقت گیت | جلوگیری از کامیت فایل‌های موقت، پوشه‌های بیلد (`build/`, `.gradle/`) و کش‌ها. |

---

### ۲. پوشه پیکربندی کاتالوگ نسخه‌ها (`/gradle/`)

| مسیر کامل نسبی | وظیفه و کاربرد | مسئولیت اصلی و ارتباط با سایر بخش‌ها |
| :--- | :--- | :--- |
| `/gradle/libs.versions.toml` | کاتالوگ نسخه‌های گرادل (Version Catalog) | منبع واحد تعیین نسخه تمام وابستگی‌ها، کتابخانه‌ها و پلاگین‌های اندروید و کامپوز. |
| `/gradle/wrapper/gradle-wrapper.properties` | پیکربندی توزیع Gradle Wrapper | مشخص‌کننده نسخه باینری گرادل برای کامپایل یکپارچه. |

---

### ۳. ماژول برنامه (`/app/`)

| مسیر کامل نسبی | وظیفه و کاربرد | مسئولیت اصلی و ارتباط با سایر بخش‌ها |
| :--- | :--- | :--- |
| `/app/build.gradle.kts` | اسکریپت بیلد و وابستگی‌های ماژول app | تنظیمات SDK (API 24 تا 36)، KSP، Room، Compose و پلاگین‌ها. |
| `/app/proguard-rules.pro` | قوانین ProGuard / R8 | قوانین بهینه‌سازی و حفظ کلاس‌ها در بیلد ریلیز نهایی. |
| `/app/src/main/AndroidManifest.xml` | مانیفست اصلی سیستم‌عامل اندروید | تعریف اکتیویتی‌ها، دسترسی‌های سیستم (بایومتریک، فایل، پرینت) و تم پایه. |

---

### ۴. کدهای سورس اندروید (`/app/src/main/java/com/example/`)

#### ۴.۱. ریشه سورس اپلیکیشن
| مسیر کامل نسبی | وظیفه و کاربرد | مسئولیت اصلی و ارتباط با سایر بخش‌ها |
| :--- | :--- | :--- |
| `/app/src/main/java/com/example/AlpaApplication.kt` | کلاس اصلی Application | نقطه ورود اصلی اپلیکیشن؛ فعال‌سازی مدیریت سراسری خطای ناشناخته (`CrashHandler`). |
| `/app/src/main/java/com/example/MainActivity.kt` | اکتیویتی اصلی Compose | اجرای `enableEdgeToEdge()`، تزریق تم پویا و اعمال جهت‌بندی سراسری RTL برای کل برنامه. |

#### ۴.۲. لایه داده و پایگاه داده (`/app/src/main/java/com/example/data/`)
| مسیر کامل نسبی | وظیفه و کاربرد | مسئولیت اصلی و ارتباط با سایر بخش‌ها |
| :--- | :--- | :--- |
| `/app/src/main/java/com/example/data/local/AppDatabase.kt` | کلاس پایگاه داده محلی Room | تعریف جداول، ورژن‌بندی دیتابیس، استراتژی ساخت و ارائه نمونه دیتابیس به صورت Singleton. |
| `/app/src/main/java/com/example/data/local/entities/Entities.kt` | انتیتی‌های دیتابیس (Entities) | مدل‌های داده‌ای مشتری، پروژه، مشخصات فنی، فاکتور، اقلام فاکتور، هزینه‌ها، چک‌ها، قرارداد و مراحل. |
| `/app/src/main/java/com/example/data/local/dao/Daos.kt` | رابط‌های دسترسی به داده (DAOs) | کوئری‌های تعاملی با SQLite به همراه Flowهای واکنشی و عملیات افزودن، ویرایش و حذف نرم. |
| `/app/src/main/java/com/example/data/local/SampleDataGenerator.kt` | تولیدکننده داده‌های نمونه کارگاهی | درج داده‌های واقعی تابلوسازی (پروژه‌های چلنیوم، استیل، نئون، مشتریان و هزینه‌ها) در اولین اجرا. |
| `/app/src/main/java/com/example/data/preferences/AppPreferences.kt` | مدیریت تنظیمات پایدار کاربر | ذخیره تنظیمات مانند حالت تم (تاریک/روشن)، رنگ اصلی، قفل بایومتریک و هشدار تغییرات فرم. |
| `/app/src/main/java/com/example/data/repository/SignageRepository.kt` | مخزن داده متمرکز (Repository) | لایه واسط میان DAOها و لایه ارائه؛ منبع واحد حقیقت (Single Source of Truth) برای ViewModelها. |

#### ۴.۳. لایه ناوبری و جهت‌یابی (`/app/src/main/java/com/example/ui/navigation/`)
| مسیر کامل نسبی | وظیفه و کاربرد | مسئولیت اصلی و ارتباط با سایر بخش‌ها |
| :--- | :--- | :--- |
| `/app/src/main/java/com/example/ui/navigation/Screen.kt` | تعاریف مسیرها و مقاصد ناوبری | تعریف مقاصد، روت‌ها، آیکون‌های توپر و خطی نوار پایین و آرگومان‌های مسیرها. |
| `/app/src/main/java/com/example/ui/navigation/AppNavigation.kt` | کانتینر ناوبری و گراف صفحات | مدیریت گراف ناوبری (`NavHost`)، نوار پایین (`NavigationBar`)، مدیریت Insets و احراز هویت ورود. |

#### ۴.۴. کامپوننت‌های مشترک رابط کاربری (`/app/src/main/java/com/example/ui/components/`)
| مسیر کامل نسبی | وظیفه و کاربرد | مسئولیت اصلی و ارتباط با سایر بخش‌ها |
| :--- | :--- | :--- |
| `/app/src/main/java/com/example/ui/components/common/CommonUi.kt` | کامپوننت‌های پایه و مشترک برنامه | شامل `AppTopBar`, `AppCard`, `PersianTextField`, `CurrencyInputField`, `PersianDateField`, `StickyBottomActionBar`, `UnsavedChangesDialog`, `AppFormBottomSheet` و منوهای کارت. |
| `/app/src/main/java/com/example/ui/components/common/SyncModalSystemBars.kt` | کامپوننت همگام‌ساز نوارهای سیستم | ابزار جانبی جهت همگام‌سازی بصری نوارهای سیستمی در شرایط خاص. |
| `/app/src/main/java/com/example/ui/components/pickers/PersianDatePickerModal.kt` | مودال تقویم شمسی جلالی | دیالوگ انتخاب تاریخ هجری شمسی ۱۰۰٪ بومی با نمایش روزهای ماه، تبدیل سال و ماه و روز. |

#### ۴.۵. صفحات و فرم‌های رابط کاربری (`/app/src/main/java/com/example/ui/screens/`)
| مسیر کامل نسبی | وظیفه و کاربرد | مسئولیت اصلی و ارتباط با سایر بخش‌ها |
| :--- | :--- | :--- |
| `/app/src/main/java/com/example/ui/screens/dashboard/DashboardScreen.kt` | داشبورد مدیریت کارگاه | خلاصه وضعیت مالی، پروژه‌های فعال، میانبرهای ثبت سریع مشتری/پروژه/دریافتی و هشدارهای کارگاه. |
| `/app/src/main/java/com/example/ui/screens/customers/CustomersScreen.kt` | صفحه مدیریت مشتریان و کارفرمایان | نمایش فهرست مشتریان، کارت‌های آماری KPI، فیلتر وضعیت، تراز مالی و استخراج تاریخچه تماس با چیدمان استاندارد RTL و تم پویا. |
| `/app/src/main/java/com/example/ui/screens/customers/CustomerDetailScreen.kt` | جزئیات پرونده مشتری | نمایش پروژه‌ها، صورتحساب‌ها، سابقه تراکنش‌های مالی و امکان تماس مستقیم با مشتری. |
| `/app/src/main/java/com/example/ui/screens/customers/CustomerEditDialog.kt` | فرم شیت ثبت و ویرایش مشتری | ورود نام، تلفن، کدملی، آدرس و توضیحات با نوار ثابت ذخیره/انصراف و گارد خروج. |
| `/app/src/main/java/com/example/ui/screens/projects/ProjectsScreen.kt` | صفحه مدیریت پروژه‌ها و سفارشات | فیلتر بر اساس وضعیت، جستجوی پروژه، کارت‌های پروژه و دکمه ایجاد سفارش جدید. |
| `/app/src/main/java/com/example/ui/screens/projects/ProjectDetailScreen.kt` | جزئیات فنی و مالی سفارش | مدیریت مراحل ساخت، مشخصات فنی تابلو، فاکتور، وضعیت مالی و صدور قرارداد. |
| `/app/src/main/java/com/example/ui/screens/projects/ProjectEditDialog.kt` | فرم شیت ایجاد/ویرایش سفارش | تنظیم عنوان سفارش، انتخاب مشتری، نوع تابلو، تاریخ تحویل و مرحله اولیه. |
| `/app/src/main/java/com/example/ui/screens/projects/ProjectSpecsScreen.kt` | صفحه مشخصات فنی ساخت تابلو | ثبت ابعاد (طول، ارتفاع، عمق)، متریال حروف، نورپردازی، منبع تغذیه (SMPS) و زیرسازی. |
| `/app/src/main/java/com/example/ui/screens/projects/SpecEditDialog.kt` | فرم شیت افزودن مشخصه فنی | ورود اطلاعات فنی اختصاصی هر بخش تابلو با اعتبارسنجی مقادیر. |
| `/app/src/main/java/com/example/ui/screens/projects/ProjectInvoiceScreen.kt` | صفحه صورتحساب و اقلام فاکتور | محاسبه خودکار متراژ محیطی، تعداد، قیمت واحد، تخفیف، مالیات و خروجی فاکتور PDF. |
| `/app/src/main/java/com/example/ui/screens/contracts/InvoiceItemEditDialog.kt` | فرم شیت ردیف قلم فاکتور | افزودن ردیف‌های اقلام تابلو با امکان تعیین واحد، مقدار، نرخ و توضیحات فنی. |
| `/app/src/main/java/com/example/ui/screens/contracts/ContractsScreen.kt` | صفحه فهرست قراردادهای کارگاه | مرور قراردادهای رسمی امضا شده و فیلتر قراردادها بر اساس وضعیت پروژه. |
| `/app/src/main/java/com/example/ui/screens/contracts/ContractEditorScreen.kt` | صفحه تمام‌صفحه تنظیم قرارداد | فرم رسمی قرارداد تابلوسازی منطبق بر ماده ۱۰ قانون مدنی با بندهای حقوقی و تولید PDF قرارداد. |
| `/app/src/main/java/com/example/ui/screens/finance/FinanceScreen.kt` | صفحه مدیریت مالی، چک‌ها و هزینه‌ها | تب‌بندی دریافتی‌ها، پرداختی‌ها، دسته چک‌های صیادی و ثبت هزینه‌های تفکیک‌شده کارگاهی و فاکتور. |
| `/app/src/main/java/com/example/ui/screens/finance/FinancialDialogs.kt` | فرم‌های شیت پرداخت، دریافت، چک و هزینه | ورود تراکنش‌های مالی، مدیریت چک و ثبت هزینه با محاسبات آنی به حروف. |
| `/app/src/main/java/com/example/ui/screens/settings/SettingsScreen.kt` | صفحه اصلی تنظیمات کارگاه | دسترسی به تم، رنگ اصلی، امنیت، پشتیبان‌گیری، سطل زباله و اطلاعات کارگاه. |
| `/app/src/main/java/com/example/ui/screens/settings/ExpenseCategoriesSettingsSheet.kt` | صفحه مستقل مدیریت عناوین هزینه‌ها | ویرایش و افزودن سرفصل‌های هزینه کارگاه و فاکتور به همراه دیالوگ‌های اختصاصی. |
| `/app/src/main/java/com/example/ui/screens/settings/ProjectStagesSettingsSheet.kt` | صفحه مستقل مدیریت مراحل پروژه | مدیریت پویای مراحل سفارش با قابلیت تغییر ترتیب، نام و کد رنگی مرحله. |
| `/app/src/main/java/com/example/ui/screens/settings/BackupRestoreSettingsSheet.kt` | صفحه مستقل پشتیبان‌گیری و بازیابی | ایجاد فایل پشتیبان رمزنگاری‌شده، بازگردانی پایگاه داده و اشتراک فایل پشتیبان. |
| `/app/src/main/java/com/example/ui/screens/settings/DatabaseManagementSettingsSheet.kt` | صفحه مستقل مدیریت دیتابیس و نمونه | بازنشانی دیتابیس کارگاه، پاکسازی داده‌ها و بارگذاری مجدد اطلاعات نمونه کارگاهی. |
| `/app/src/main/java/com/example/ui/screens/auth/LoginScreen.kt` | صفحه قفل و احراز هویت | ورود با پین‌کد اختصاصی یا حسگر اثر انگشت/تشخیص چهره بیومتریک برای امنیت اطلاعات مالی. |
| `/app/src/main/java/com/example/ui/screens/crash/CrashReportActivity.kt` | اکتیویتی اختصاصی گزارش خطا (Crash) | نمایش جزئیات فنی کرش، امکان کپی گزارش و اشتراک‌گذاری با توسعه‌دهنده بدون بسته‌شدن ناگهانی برنامه. |

#### ۴.۶. تم و طراحی بصری (`/app/src/main/java/com/example/ui/theme/`)
| مسیر کامل نسبی | وظیفه و کاربرد | مسئولیت اصلی و ارتباط با سایر بخش‌ها |
| :--- | :--- | :--- |
| `/app/src/main/java/com/example/ui/theme/Color.kt` | پالت رنگی متریال ۳ و رنگ‌های تابلوسازی | رنگ‌های سازمانی، رنگ‌های ۶ گانه سفارشی تم، و رنگ‌های وضعیت مالی مثبت/منفی/هشدار. |
| `/app/src/main/java/com/example/ui/theme/Type.kt` | تایپوگرافی و فونت‌های وزیرمتن | تنظیم فونت فارسی Vazirmatn در تمام مقیاس‌های عنوان، تیتر، متن و برچسب‌های متریال. |
| `/app/src/main/java/com/example/ui/theme/Theme.kt` | تم کلی برنامه (`MyApplicationTheme`) | مدیریت حالت تیره و روشن، ساخت Schemeهای رنگی سفارشی و اعمال استایل‌ها. |

#### ۴.۷. لایه ViewModel (`/app/src/main/java/com/example/ui/viewmodels/`)
| مسیر کامل نسبی | وظیفه و کاربرد | مسئولیت اصلی و ارتباط با سایر بخش‌ها |
| :--- | :--- | :--- |
| `/app/src/main/java/com/example/ui/viewmodels/SignageViewModels.kt` | ویومدل‌های متمرکز برنامه | مدیریت وضعیت (StateFlows)، منطق تجاری محاسبات فاکتور و تراکنش‌ها، ارتباط با مخزن داده. |

#### ۴.۸. ماژول‌ها و ابزارهای کاربردی (`/app/src/main/java/com/example/util/`)
| مسیر کامل نسبی | وظیفه و کاربرد | مسئولیت اصلی و ارتباط با سایر بخش‌ها |
| :--- | :--- | :--- |
| `/app/src/main/java/com/example/util/CallLogHelper.kt` | ابزار استعلام تاریخچه تماس‌های اندروید | بررسی دسترسی READ_CALL_LOG و استخراج جدیدترین تاریخ تماس ثبت‌شده با هر شماره تلفن و تبدیل به تاریخ جلالی. |
| `/app/src/main/java/com/example/util/JalaliDate.kt` | گاه‌شماری و محاسبات تاریخ جلالی | تبدیل تاریخ میلادی به هجری شمسی بدون وابستگی به java.time با پشتیبانی از API 24 به بالا. |
| `/app/src/main/java/com/example/util/FormatUtils.kt` | فرمت‌بندی اعداد و مبالغ پولی | جداسازی ۳ رقمی ارقام، تبدیل ارقام به فارسی، برچسب‌های پولی و تبدیل اعداد به حروف. |
| `/app/src/main/java/com/example/util/PdfGenerator.kt` | تولید اسناد رسمی PDF | خروجی استاندارد و چاپ‌پذیر برای فاکتورهای کارگاهی و قراردادهای حقوقی تابلوسازی. |
| `/app/src/main/java/com/example/util/BackupRestoreManager.kt` | موتور پشتیبان‌گیری و بازیابی پایگاه داده | استخراج دیتابیس SQLite در قالب فایل JSON، رمزنگاری و اعتبارسنجی هنگام بازگردانی. |
| `/app/src/main/java/com/example/util/CrashHandler.kt` | ناظر خطاهای ناشناخته سراسری | مدیریت UncaughtExceptionHandler و ثبت اطلاعات خطا در فایل لاگ و فراخوانی صفحه کرش. |
| `/app/src/main/java/com/example/util/SecurityUtils.kt` | توابع رمزنگاری و هش پین‌کد | هش امن کلمات عبور با SHA-256 و اعتبارسنجی اعتبارات امنیتی. |

---

### ۵. منابع اندروید (`/app/src/main/res/`)

| مسیر کامل نسبی | وظیفه و کاربرد | مسئولیت اصلی و ارتباط با سایر بخش‌ها |
| :--- | :--- | :--- |
| `/app/src/main/res/font/` | فونت‌های بومی وزیرمتن (Vazirmatn) | خانواده فونت‌های محلی ttf با وزن‌های Thin, Light, Regular, Medium, SemiBold, Bold, ExtraBold, Black. |
| `/app/src/main/res/values/strings.xml` | رشته‌های متنی و نام برنامه | تعریف عنوان لانچر برنامه (`تابلوسازی آلپا`) و عناوین پایه. |
| `/app/src/main/res/values/colors.xml` | رنگ‌های منابع XML اندروید | رنگ‌های اولیه برای پیش‌نمایش تم و نوار سیستمی پیش از لود Compose. |
| `/app/src/main/res/values/themes.xml` | تم اکتیویتی پایه در سطح OS | تم سازگار `Theme.AppCompat.DayNight.NoActionBar` جهت جلوگیری از کرش بیومتریک در آغاز به کار. |
| `/app/src/main/res/xml/file_paths.xml` | پیکربندی FileProvider | دسترسی اشتراک‌گذاری امن فایل‌های PDF و فایل‌های پشتیبان با سایر برنامه‌ها. |
| `/app/src/main/res/xml/backup_rules.xml` | قوانین پشتیبان‌گیری سیستم‌عامل اندروید | قوانین انتقال داده‌ها در پشتیبان‌گیری ابری خودکار گوگل. |
| `/app/src/main/res/xml/data_extraction_rules.xml` | قوانین استخراج داده در اندروید ۱۲+ | تعیین نحوه انتقال داده در هنگام تعویض دستگاه یا بازنشانی کارخانه. |
| `/app/src/main/res/drawable/` و `/app/src/main/res/mipmap-.../` | آیکون‌ها و تصاویر لانچر برنامه | آیکون‌های متناسب در سایزهای مختلف hdpi تا xxxhdpi و آیکون تطبیقی (Adaptive Icon). |

---

### ۶. آزمون‌ها و تست‌های محلی (`/app/src/test/`)

| مسیر کامل نسبی | وظیفه و کاربرد | مسئولیت اصلی و ارتباط با سایر بخش‌ها |
| :--- | :--- | :--- |
| `/app/src/test/java/com/example/ExampleRobolectricTest.kt` | تست‌های لایه داده با Robolectric | تست صحت ساخت دیتابیس Room، انتیتی‌ها و عملیات ثبت بدون نیاز به امولاتور. |
| `/app/src/test/java/com/example/ExampleUnitTest.kt` | آزمون‌های واحد پایه (Unit Test) | اعتبارسنجی توابع تبدیل تاریخ جلالی، فرمت‌بندی اعداد و منطق پایه ریاضی. |

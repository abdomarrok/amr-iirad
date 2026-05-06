# سجل التقدم التطويري — Amr-Iirad Mini-App
> آخر تحديث: 2026-05-06  
> المرحلة الحالية: **Phase 5 — Testing + Polish + Packaging**

---

## الخريطة الزمنية العامة

| المرحلة | الوصف | الحالة |
|---|---|---|
| **Phase 0** | Bootstrap: هيكل المشروع، قاعدة البيانات، الأدوات الأساسية | ✅ مكتمل |
| **Phase 1** | Data Layer: Models + Repositories | ✅ مكتمل |
| **Phase 2** | Service Layer: Business Logic + Validation | ✅ مكتمل |
| **Phase 3** | UI Layer: Screens + FXML + CSS | ✅ مكتمل |
| **Phase 4** | Reporting: JasperReports (5 Annexes) | ✅ مكتمل |
| **Phase 5** | Testing + Polish + Packaging | 🔄 جارٍ |

---

## Phase 0 — Project Bootstrap

**الهدف:** إنشاء الهيكل الكامل للمشروع وضمان أن التطبيق يبدأ ويتصل بقاعدة البيانات.

### المهام

| # | المهمة | الملف | الحالة |
|---|---|---|---|
| 0.1 | إنشاء مجلد المشروع وهيكل `src/` | أوامر mkdir | ✅ مكتمل |
| 0.2 | إنشاء `pom.xml` بجميع التبعيات | `pom.xml` | ✅ مكتمل |
| 0.3 | نقل ملفات المواصفات إلى `amr-iirad/specs/` | `specs/` | ✅ مكتمل |
| 0.4 | استيراد وتكييف `AppSettings.java` | `util/AppSettings.java` | ✅ مكتمل |
| 0.5 | استيراد وتكييف `AppMode.java` | `util/AppMode.java` | ✅ مكتمل |
| 0.6 | إعادة كتابة `DatabaseConnection.java` | `util/DatabaseConnection.java` | ✅ مكتمل |
| 0.7 | إعادة كتابة `EmbeddedDatabase.java` | `util/EmbeddedDatabase.java` | ✅ مكتمل |
| 0.8 | استيراد وتكييف `ConcurrencyManager.java` | `core/ConcurrencyManager.java` | ✅ مكتمل |
| 0.9 | إنشاء `DatabaseSchemaManager.java` (7 جداول + view) | `util/DatabaseSchemaManager.java` | ✅ مكتمل |
| 0.10 | إنشاء `AmrIiradApp.java` (entry point) | `AmrIiradApp.java` | ✅ مكتمل |
| 0.11 | إنشاء `PRD.md` و `advancement.md` | `specs/` | ✅ مكتمل |
| 0.12 | التحقق من الترجمة (`mvn clean compile`) | — | ✅ مكتمل |
| 0.13 | إنشاء `AGENTS.md` (دليل الأعوان المستقل) | `specs/AGENTS.md` | ✅ مكتمل |

---

## Phase 1 — Data Layer (✅ مكتمل)

**الهدف:** بناء Models وRepositories لجميع الكيانات.

### المهام المنجزة

| # | المهمة | الملف | الحالة |
|---|---|---|---|
| 1.1 | `FiscalYear.java` model | `model/FiscalYear.java` | ✅ مكتمل |
| 1.2 | `Debtor.java` model + Enums | `model/` | ✅ مكتمل |
| 1.3 | `RevenueOrder.java` model | `model/RevenueOrder.java` | ✅ مكتمل |
| 1.4 | `RevenueOrderCancellation.java` model | `model/RevenueOrderCancellation.java` | ✅ مكتمل |
| 1.5 | `DispatchSlip.java` model | `model/DispatchSlip.java` | ✅ مكتمل |
| 1.6 | `FiscalYearRepository.java` (CRUD) | `repository/FiscalYearRepository.java` | ✅ مكتمل |
| 1.7 | `DebtorRepository.java` (CRUD + Search) | `repository/DebtorRepository.java` | ✅ مكتمل |
| 1.8 | `BudgetChapterRepository.java` (Hierarchy) | `repository/BudgetChapterRepository.java` | ✅ مكتمل |
| 1.9 | `RevenueOrderRepository.java` (CRUD + Filters) | `repository/RevenueOrderRepository.java` | ✅ مكتمل |
| 1.10 | `CancellationOrderRepository.java` | `repository/CancellationOrderRepository.java` | ✅ مكتمل |
| 1.11 | `DispatchSlipRepository.java` | `repository/DispatchSlipRepository.java` | ✅ مكتمل |

---

## Phase 2 — Service Layer (✅ مكتمل)

**الهدف:** تطبيق قواعد العمل من التعليمة 08/2023.

### المهام المنجزة

| # | المهمة | الملف | قواعد العمل | الحالة |
|---|---|---|---|---|
| 2.1 | `RevenueOrderService` — إنشاء وتدفق الحالات | `service/RevenueOrderService.java` | RO-01→RO-12, INS-01, INS-02 | ✅ مكتمل |
| 2.2 | `CancellationOrderService` — إلغاء وتخفيض | `service/CancellationOrderService.java` | BR-03, BR-04, INS-04, INS-05 | ✅ مكتمل |
| 2.3 | `DispatchSlipService` — تجميع البوردرو | `service/DispatchSlipService.java` | BR-05, BR-06, INS-06 | ✅ مكتمل |
| 2.4 | `TafqeetService` — المبلغ بالحروف | `service/TafqeetService.java` | RO-10, INS-02 | ✅ مكتمل |
| 2.5 | `AuditService` — تسجيل العمليات | `service/AuditService.java` | — | ✅ مكتمل |

---

## Phase 3 — UI Layer (✅ مكتمل)

### الشاشات

| # | الشاشة | الملفات | الحالة |
|---|---|---|---|
| 3.1 | اختيار وضع التطبيق | `mode-selection-view.fxml` + `ModeSelectionController.java` | ✅ مكتمل |
| 3.2 | إعداد خادم قاعدة البيانات | `server-config-view.fxml` + `ServerConfigController.java` | ✅ مكتمل |
| 3.3 | لوحة التحكم الرئيسية | `dashboard-view.fxml` + `DashboardController.java` | ✅ مكتمل |
| 3.4 | CSS + Theme | `css/app.css` + `css/theme.css` | ✅ مكتمل |
| 3.5 | إصلاح AmrIiradApp (تسلسل التشغيل) | `AmrIiradApp.java` | ✅ مكتمل |
| 3.6 | قائمة أوامر الإيراد | `order-list-view.fxml` + `RevenueOrderListController.java` | ✅ مكتمل |
| 3.7 | نموذج أمر الإيراد (إنشاء/تعديل) | `order-form-view.fxml` + `RevenueOrderFormController.java` | ✅ مكتمل |
| 3.8 | نموذج الإلغاء / التخفيض | `cancellation-form-view.fxml` + `CancellationFormController.java` | ✅ مكتمل |
| 3.9 | نموذج البوردرو | `dispatch-slip-view.fxml` + `DispatchSlipController.java` | ✅ مكتمل |
| 3.10 | إدارة المدينين | `debtor-list-view.fxml` + `DebtorListController.java` | ✅ مكتمل |
| 3.11 | نموذج إضافة/تعديل مدين | `debtor-form-view.fxml` + `DebtorFormController.java` | ✅ مكتمل |
| 3.12 | إعداد السنة المالية (Modal) | `DashboardController.java` (TextInputDialog) | ✅ مكتمل |

---

## Phase 4 — Reporting (✅ مكتمل)

### التقارير المطلوبة (JasperReports .jrxml)

| # | الملف | المحتوى |
|---|---|---|
| 4.1 | `annexe1_order.jrxml` | أمر الإيراد الأصلي |
| 4.2 | `annexe2_debtor_copy.jrxml` | نسخة المدين |
| 4.3 | `annexe3_full_cancel.jrxml` | أمر الإلغاء |
| 4.4 | `annexe4_reduction.jrxml` | أمر التخفيض |
| 4.5 | `annexe5_dispatch.jrxml` | بوردرو الإرسال |

---

## Phase 5 — Testing & Packaging (🔄 جارٍ)

| # | المهمة |
|---|---|
| 5.1 | اختبارات وحدة للـ Services (JUnit 5) |
| 5.2 | اختبارات الـ Repositories |
| 5.3 | حزم التوزيع (`mvn package` + installer) |
| 5.4 | دليل المستخدم الأساسي |

---

## سجل التغييرات

| التاريخ | التغيير |
|---|---|
| 2026-05-06 | بدء المشروع — تحليل التعليمة 08/2023 |
| 2026-05-06 | إنشاء هيكل Maven وملفات Phase 0 الأساسية |
| 2026-05-06 | إنشاء `DatabaseSchemaManager` بـ 7 جداول + view |
| 2026-05-06 | تكييف utilities من GstockDz (DatabaseConnection, AppSettings, EmbeddedDatabase) |
| 2026-05-06 | إنشاء ملفات المواصفات (PRD.md, advancement.md) |
| 2026-05-06 | ✅ **BUILD SUCCESS** — المشروع يترجم بدون أخطاء (7 ملفات Java) |
| 2026-05-06 | إنشاء `AGENTS.md` — دليل مستقل للأعوان المستقبليين |
| 2026-05-06 | إنشاء مستودع Git مستقل وربطه بـ GitHub |
| 2026-05-06 | إتمام جميع الـ Models (RevenueOrder, Debtor, FiscalYear, etc.) |
| 2026-05-06 | إنشاء أول 3 مستودعات بيانات (FiscalYear, Debtor, BudgetChapter) |
| 2026-05-06 | ✅ **PHASE 1 COMPLETE** — جميع الـ Models والـ Repositories جاهزة ومختبرة بالتجميع |
| 2026-05-06 | إنشاء خدمات Phase 2: AuditService, TafqeetService, RevenueOrderService |
| 2026-05-06 | إنشاء CancellationOrderService و DispatchSlipService |
| 2026-05-06 | إضافة أعمدة is_deleted لجداول revenue_order, cancellation, dispatch_slip |
| 2026-05-06 | تصحيح TafqeetService باستخدام النمط الموجود في GstockDz (dinar/centime) |
| 2026-05-06 | ✅ **PHASE 2 COMPLETE** — جميع الخدمات جاهزة (BUILD SUCCESS: 27 ملف) |
| 2026-05-06 | إنشاء ملفات CSS (app.css, theme.css) مقتبسة من GstockDz |
| 2026-05-06 | إنشاء واجهات ModeSelection و ServerConfig و Dashboard مع وحدات التحكم |
| 2026-05-06 | تعديل نقطة الدخول AmrIiradApp لدعم تدفق بدء التشغيل (اختيار الوضع ثم تهيئة DB) |
| 2026-05-06 | ✅ **إتمام UI Shell** — التطبيق قابل للتشغيل بواجهات رئيسية (BUILD SUCCESS: 30 ملف) |
| 2026-05-06 | إنشاء واجهات ووحدات التحكم لكل من: RevenueOrderList, RevenueOrderForm, DebtorList, CancellationForm, DispatchSlip |
| 2026-05-06 | ✅ **PHASE 3 COMPLETE** — جميع واجهات المستخدم مبنية ومرتبطة بوحدات التحكم وطبقة الخدمات |
| 2026-05-06 | إنشاء ReportService وربطه مع GstockDz Jasper configuration |
| 2026-05-06 | إنشاء 5 قوالب JasperReports (الملحق 1 إلى الملحق 5) باللغة العربية (Traditional Arabic) |
| 2026-05-06 | ربط أزرار الطباعة في RevenueOrderForm, CancellationForm و DispatchSlip |
| 2026-05-06 | ✅ **PHASE 4 COMPLETE** — طبقة التقارير مكتملة (BUILD SUCCESS: 36 ملف) |
| 2026-05-06 | إنشاء واجهة إضافة مدين جديد (`DebtorFormController`) وربطها بالواجهات الرئيسية |
| 2026-05-06 | إضافة خاصية إنشاء سنة مالية جديدة مباشرة من لوحة التحكم |
| 2026-05-06 | إضافة بيانات تجريبية (Seed Data) لمحاور الميزانية في `DatabaseSchemaManager` |
| 2026-05-06 | ✅ **جاهز للاختبار اليدوي** — جميع المسارات الأساسية مفعلة (BUILD SUCCESS: 38 ملف) |
| 2026-05-06 | إعادة هيكلة شاملة لاعتماد **Constructor Injection** في كافة الـ Controllers |
| 2026-05-06 | توحيد نظام التنقل باستخدام نمط **Modal Dialogs** مقتبس من GstockDz |
| 2026-05-06 | إضافة خاصية **Auto-Seeding** للسنة المالية لضمان وجود سنة مفعلة عند أول تشغيل |
| 2026-05-06 | إصلاح حجم النوافذ المنبثقة لتناسب المحتوى تلقائياً (`sizeToScene`) |
| 2026-05-06 | حل مشكلة التقارير الفارغة عبر تفعيل `JREmptyDataSource(1)` للملاحق التي تعتمد على المعاملات |
| 2026-05-06 | تحديث `pom.xml` وإضافة تبعات JasperReports 7.0 المفقودة (`poi`, `view`) لحل أخطاء الطباعة |

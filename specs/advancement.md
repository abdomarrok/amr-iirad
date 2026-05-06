# سجل التقدم التطويري — Amr-Iirad Mini-App
> آخر تحديث: 2026-05-06  
> المرحلة الحالية: **Phase 0 — Project Bootstrap**

---

## الخريطة الزمنية العامة

| المرحلة | الوصف | الحالة |
|---|---|---|
| **Phase 0** | Bootstrap: هيكل المشروع، قاعدة البيانات، الأدوات الأساسية | ✅ مكتمل |
| **Phase 1** | Data Layer: Models + Repositories | 🔄 جارٍ |
| **Phase 2** | Service Layer: Business Logic + Validation | ⏳ لم يبدأ |
| **Phase 3** | UI Layer: Screens + FXML + CSS | ⏳ لم يبدأ |
| **Phase 4** | Reporting: JasperReports (5 Annexes) | ⏳ لم يبدأ |
| **Phase 5** | Testing + Polish + Packaging | ⏳ لم يبدأ |

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

## Phase 1 — Data Layer (⏳ لم يبدأ)

**الهدف:** بناء Models وRepositories لجميع الكيانات.

### المهام المخططة

| # | المهمة | الملف المتوقع |
|---|---|---|
| 1.1 | `FiscalYear.java` model | `model/FiscalYear.java` |
| 1.2 | `Debtor.java` model + `OrderStatus.java` enum | `model/` |
| 1.3 | `RevenueOrder.java` model | `model/RevenueOrder.java` |
| 1.4 | `RevenueOrderCancellation.java` model | `model/` |
| 1.5 | `DispatchSlip.java` model | `model/` |
| 1.6 | `FiscalYearRepository.java` (CRUD) | `repository/` |
| 1.7 | `DebtorRepository.java` (CRUD + Search) | `repository/` |
| 1.8 | `RevenueOrderRepository.java` (CRUD + Filters) | `repository/` |
| 1.9 | `CancellationOrderRepository.java` | `repository/` |
| 1.10 | `DispatchSlipRepository.java` | `repository/` |

---

## Phase 2 — Service Layer (⏳ لم يبدأ)

**الهدف:** تطبيق قواعد العمل من التعليمة 08/2023.

### المهام المخططة

| # | المهمة | قاعدة العمل |
|---|---|---|
| 2.1 | `RevenueOrderService` — إنشاء وتدفق الحالات | BR-01, BR-02 |
| 2.2 | `CancellationOrderService` — إلغاء وتخفيض | BR-03, BR-04 |
| 2.3 | `DispatchSlipService` — تجميع البوردرو | BR-05, BR-06 |
| 2.4 | `ReportService` — JasperReports (5 Annexes) | BR-07 |
| 2.5 | `AuditService` — تسجيل العمليات | — |

---

## Phase 3 — UI Layer (⏳ لم يبدأ)

### الشاشات المخططة

| # | الشاشة | الملفات |
|---|---|---|
| 3.1 | اختيار وضع التطبيق | `mode-selection-view.fxml` |
| 3.2 | إعداد خادم قاعدة البيانات | `server-config-view.fxml` |
| 3.3 | تسجيل الدخول | `login-view.fxml` |
| 3.4 | الشاشة الرئيسية / لوحة التحكم | `dashboard-view.fxml` |
| 3.5 | قائمة أوامر الإيراد | `order-list-view.fxml` |
| 3.6 | نموذج أمر الإيراد (إنشاء/تعديل) | `order-form-view.fxml` |
| 3.7 | نموذج الإلغاء / التخفيض | `cancellation-form-view.fxml` |
| 3.8 | نموذج البوردرو | `dispatch-slip-view.fxml` |
| 3.9 | إدارة المدينين | `debtor-list-view.fxml` |

---

## Phase 4 — Reporting (⏳ لم يبدأ)

### التقارير المطلوبة (JasperReports .jrxml)

| # | الملف | المحتوى |
|---|---|---|
| 4.1 | `annexe1_order.jrxml` | أمر الإيراد الأصلي |
| 4.2 | `annexe2_debtor_copy.jrxml` | نسخة المدين |
| 4.3 | `annexe3_full_cancel.jrxml` | أمر الإلغاء |
| 4.4 | `annexe4_reduction.jrxml` | أمر التخفيض |
| 4.5 | `annexe5_dispatch.jrxml` | بوردرو الإرسال |

---

## Phase 5 — Testing & Packaging (⏳ لم يبدأ)

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

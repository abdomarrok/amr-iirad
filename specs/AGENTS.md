# Amr-Iirad — Agent & Developer Onboarding Guide

## Quick Summary

`amr-iirad` is a **standalone JavaFX 21 / Maven desktop application** that digitizes the Algerian Revenue Order (أمر بالإيراد) workflow, fully aligned with **Instruction n° 08 du 09 avril 2023** (Direction Générale du Trésor). It is self-contained — you do NOT need the parent WGEBUDG project or GstockDz to continue development.

---

## Build & Run Commands

All commands are run from `C:\Users\marrokesm\Desktop\WGEBUDG\amr-iirad\`.

```powershell
# Verify compilation
..\mvnw.cmd clean compile

# Run the desktop app
..\mvnw.cmd clean javafx:run

# Run tests
..\mvnw.cmd test

# Build distributable
..\mvnw.cmd clean package
```

> The Maven wrapper (`mvnw.cmd`) is in the **parent directory** (`WGEBUDG/`), one level up. Always run commands with `..\mvnw.cmd` prefix from within the `amr-iirad/` directory.

---

## Project Structure

```
amr-iirad/
├── pom.xml                                  ← Maven dependencies & build config
├── specs/                                   ← ALL documentation lives here
│   ├── AGENTS.md                            ← THIS FILE — start here
│   ├── PRD.md                               ← Product Requirements Document
│   ├── advancement.md                       ← Phase-by-phase progress tracker
│   ├── amr_iirad_miniapp_plan.md            ← Detailed technical architecture plan
│   └── instruction_08_2023_analysis.md      ← Legal field-by-field extraction
└── src/main/java/org/marrok/amriirad/
    ├── AmrIiradApp.java                     ← Entry point (JavaFX Application)
    ├── util/
    │   ├── AppSettings.java                 ← Persistent settings (java.util.prefs)
    │   ├── AppMode.java                     ← Enum: LOCAL | SERVER
    │   ├── DatabaseConnection.java          ← HikariCP pool + Backup/Restore
    │   ├── EmbeddedDatabase.java            ← MariaDB4j (local mode)
    │   └── DatabaseSchemaManager.java       ← DDL: 7 tables + 1 view (idempotent)
    ├── core/
    │   └── ConcurrencyManager.java          ← Background task execution (JavaFX-safe)
    ├── model/                               ← [Phase 1] Domain objects (empty)
    ├── repository/                          ← [Phase 1] JDBC Data Access (empty)
    ├── service/                             ← [Phase 2] Business Logic (empty)
    └── controller/                          ← [Phase 3] JavaFX Controllers (empty)
```

---

## Technology Stack

| Layer | Technology | Version |
|---|---|---|
| UI | JavaFX + FXML | 21.0.6 |
| DB (Local mode) | MariaDB4j (embedded) | 2.5.3 |
| DB (Server mode) | MariaDB (external) | any |
| Connection Pool | HikariCP | 5.1.0 |
| Reports & PDF | JasperReports | 7.0.0 |
| Arabic Numbers→Words | TafqeetJ | 1.1-RELEASE |
| UI Icons | Ikonli (FontAwesome5) | 12.3.1 |
| UI Theme | AtlantaFX | 2.0.1 |
| Logging | Log4j2 | 2.24.1 |
| Build | Maven + Java 21 | — |

---

## Application Startup Flow

```
AmrIiradApp.start()
   │
   ├─► AppSettings.isModeConfigured()?
   │       NO  → show mode-selection-view.fxml  (user picks LOCAL or SERVER)
   │       YES → load saved mode
   │
   ├─► DatabaseConnection.initialize(mode)
   │       LOCAL  → EmbeddedDatabase.start() → MariaDB4j on port 3307
   │       SERVER → Use saved host/port from AppSettings
   │
   ├─► DatabaseSchemaManager.runMigrations()
   │       Creates 7 tables + 1 view if they don't exist (idempotent)
   │
   └─► Show login-view.fxml
```

---

## Database Schema (7 Tables)

All tables use `ENGINE=InnoDB` and `utf8mb4` charset.

| Table | Purpose | Key Fields |
|---|---|---|
| `fiscal_year` | Active fiscal year | `year_label`, `is_active` |
| `debtor` | المدين / الملزم بالدفع | `full_name`, `debtor_type` |
| `budget_chapter` | هيكل الميزانية (Titre→Chapitre→Article→Paragraphe) | `code`, `level`, `parent_id` |
| `revenue_order` | أمر الإيراد (Annexe 1 & 2) | `order_number`, `amount`, `status` |
| `revenue_order_cancellation` | أمر الإلغاء / التخفيض (Annexe 3 & 4) | `cancellation_type`, `reduced_amount` |
| `dispatch_slip` | بوردرو الإرسال (Annexe 5) | `slip_number`, `total_amount` |
| `dispatch_slip_order` | Link table (slip ↔ orders) | `slip_id`, `order_id` |
| `audit_log` | سجل العمليات | `action`, `performed_by` |

**View:** `v_revenue_order_summary` — joins `revenue_order` with `fiscal_year`, `debtor`, `budget_chapter`.

---

## Business Rules (from Instruction 08/2023)

| ID | Rule | Enforced In |
|---|---|---|
| BR-01 | Order number unique per fiscal year | DB UNIQUE KEY |
| BR-02 | Amount must be > 0 | `RevenueOrderService` guard |
| BR-03 | Cannot cancel a DRAFT order | `CancellationOrderService` guard |
| BR-04 | Reduction cannot exceed original amount | `CancellationOrderService` guard |
| BR-05 | Dispatch slip cannot mix fiscal years | `DispatchSlipService` guard |
| BR-06 | Only ISSUED orders can be dispatched | `DispatchSlipService` guard |
| BR-07 | Amount in Arabic words required for print | TafqeetJ called in `ReportService` |

---

## 5 Required Document Templates (JasperReports)

| Template | Content | JRXML File |
|---|---|---|
| Annexe 1 | Original Revenue Order | `reports/annexe1_order.jrxml` |
| Annexe 2 | Debtor's copy | `reports/annexe2_debtor_copy.jrxml` |
| Annexe 3 | Full Cancellation | `reports/annexe3_full_cancel.jrxml` |
| Annexe 4 | Reduction Order | `reports/annexe4_reduction.jrxml` |
| Annexe 5 | Dispatch Slip (Bordereau) | `reports/annexe5_dispatch.jrxml` |

---

## Current State — Phase 0 COMPLETE ✅

All utilities are written and the project compiles cleanly (`BUILD SUCCESS`). The following is ready:

- ✅ `pom.xml` with all dependencies
- ✅ `AppSettings` + `AppMode` (persistent mode selection via `java.util.prefs`)
- ✅ `DatabaseConnection` (HikariCP + GZIP backup/restore)
- ✅ `EmbeddedDatabase` (MariaDB4j for local mode)
- ✅ `DatabaseSchemaManager` (7 tables + view, idempotent)
- ✅ `ConcurrencyManager` (background task execution, JavaFX-thread-safe)
- ✅ `AmrIiradApp` (entry point with startup flow)
- ✅ `specs/` with PRD, advancement tracker, technical plan, and legal analysis

---

## Next Step — Phase 1: Data Layer

Create the **Model** and **Repository** classes. Follow this order strictly (dependencies):

### Step 1 — Models (in `model/` package)

```
FiscalYear.java          → id, yearLabel, isActive, createdAt
Debtor.java              → id, fullName, idNumber, address, phone, debtorType (enum)
DebtorType.java          → enum: INDIVIDUAL, COMPANY, STATE_ENTITY
BudgetChapter.java       → id, code, labelAr, labelFr, parentId, level
OrderStatus.java         → enum: DRAFT, ISSUED, DISPATCHED, CANCELLED, REDUCED
RevenueOrder.java        → all revenue_order fields + nested Debtor & BudgetChapter
RevenueOrderCancellation.java → all cancellation fields + nested RevenueOrder
CancellationType.java    → enum: FULL_CANCEL, REDUCTION
DispatchSlip.java        → all dispatch_slip fields + List<RevenueOrder>
```

### Step 2 — Repositories (in `repository/` package)

Each repository uses `DatabaseConnection.getConnection()` directly (no ORM).

```
FiscalYearRepository.java   → findAll(), findActive(), save(), setActive(id)
DebtorRepository.java       → findAll(), findById(), search(query), save(), update()
BudgetChapterRepository.java → findAll(), findByLevel(n), findChildren(parentId)
RevenueOrderRepository.java → findAll(fiscalYearId), findById(), save(), updateStatus()
CancellationOrderRepository.java → findByOrderId(), save()
DispatchSlipRepository.java → findAll(fiscalYearId), findById(), save(), addOrder()
```

### Step 3 — Compile Check

```powershell
..\mvnw.cmd clean compile
```

---

## Coding Conventions

- **4-space indentation**, braces on the same line
- PascalCase for classes: `RevenueOrderRepository`
- camelCase for methods/fields: `findByFiscalYear()`
- Package: `org.marrok.amriirad.*`
- Controller/View pairs: `RevenueOrderFormController.java` ↔ `order-form-view.fxml`
- All SQL via plain JDBC with `PreparedStatement` — NO ORM
- Log with Log4j2: `private static final Logger logger = LogManager.getLogger(MyClass.class);`
- Arabic UI text goes in the FXML directly (RTL by default)

---

## Important Files to Read Before Starting

1. `specs/PRD.md` — Full product requirements
2. `specs/advancement.md` — Exactly which tasks are done / pending
3. `specs/amr_iirad_miniapp_plan.md` — Detailed architecture, schema, and UI screens
4. `specs/instruction_08_2023_analysis.md` — Legal field-by-field requirements

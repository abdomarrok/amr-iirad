# أمر بالإيراد — Mini-App Project Plan
### Revenue Order Management System — Independent Module

> **Document Status:** Implementation Phase — Phase 4 Complete, ready for Testing/Packaging.
> **Parent Project:** WGEBUDG (Algerian Public Budget Management)
> **Institution Context:** Ministry of Justice / Superior School of Magistracy (المدرسة العليا للقضاء)
> **Reference Documents:**
> 1. Physical form photograph of "أمر بالإيراد" (Revenue Order)
> 2. `Instruction-n°-08-du-09-avril-2023-FR.pdf` (Direction Générale du Trésor)
> 3. [`instruction_08_2023_analysis.md`](./instruction_08_2023_analysis.md) ← Full legal extraction & field-by-field analysis
---

## 1. Executive Summary

The goal of this mini-app is to digitize and manage the **"أمر بالإيراد"** (Revenue Order) — a mandatory Algerian public accounting document used to formally authorize the collection of public funds. The app will allow operators to **create, validate, track, and print** these orders in a controlled workflow, fully aligned with Algerian public finance law.

This mini-app is built **in parallel** with WGEBUDG and may later be merged into it as **Module H (Revenue)**, once mature. It intentionally reuses the same architectural patterns established in WGEBUDG (layered MVC, FiscalYearGuard, AuditService, etc.) to ensure future integration is seamless.

### 1.1 Legal Framework (Instruction n° 08 / 2023)

This application strictly implements **Instruction n° 08 du 09 avril 2023** issued by the *Direction Générale du Trésor*. This instruction defines the mandatory standardized models for revenue orders under the new program budget framework (Organic Law 18-15).

The instruction mandates five distinct documents (Annexes) which the system must support:
*   **Annexe 1**: Ordre de recette (Revenue Order — notified to the public accountant).
*   **Annexe 2**: Avis d'émission d'ordre de recette (Emission Notice — notified to the debtor).
*   **Annexe 3**: Ordre d'annulation ou de réduction de recette (Cancellation/Reduction Order — for the accountant).
*   **Annexe 4**: Avis d'émission d'ordre d'annulation/réduction (Notice of Cancellation/Reduction — for the debtor).
*   **Annexe 5**: Bordereau d'envoi des ordres de recette (Dispatch slip summarizing sent orders).

*Note: The physical form originally analyzed corresponds exactly to Annexe 1 / Annexe 2 of this instruction.*

---

## 2. Understanding the Document (Source of Truth)

The physical form analyzed from the photo contains the following logical sections:

### 2.1 Document Header (الرأسية)
| Field | Arabic Label | Notes |
|---|---|---|
| Authorizing Officer | الآمر بالصرف | Pre-filled from dossier config |
| Order Code | رمز الأمر بالصرف | Auto-generated reference |
| Ministry / Institution Name | وزارة العدل / المدرسة العليا للقضاء | Static for this institution |

### 2.2 Budgetary Imputation (حساب القيد)
| Field | Arabic Label | Type |
|---|---|---|
| Fiscal Year | السنة المالية | Integer (e.g. 2025) |
| Program Portfolio | محفظة البرامج | Text / Lookup |
| Program | البرنامج | Text / Lookup |
| Sub-Program | البرنامج الفرعي | Text / Lookup |
| Action | النشاط | Text / Lookup (Instruction 08 terminology) |
| Sub-Action | النشاط الفرعي | Text / Lookup (Instruction 08 terminology) |
| Title (Budget Classification) | العنوان | TINYINT. *Used specifically for "rétablissement de crédits" (credit restoration).* |
| Category / Sub-Category | الصنف / الصنف الفرعي | Lookup code. *Used specifically for "rétablissement de crédits".* |
| Revenue Order Number | رقم الأمر بالإيراد | Auto-generated, unique per year |

### 2.3 Narrative Text (النص التفسيري)
A standardized, partially filled legal text that reads:
> *"يطلب من السيد... أن يدفع لصندوق المحاسب العمومي المعتمد لدي المدرسة العليا للقضاء وهو المؤهل وفقاً لأحكام المرسوم التنفيذي رقم 343-24..."*

This text is **mostly static** (institution-specific) with only the following fields being dynamic:
- The **debtor's name** (السيد...)
- The **target treasury account numbers** (configured once per dossier)

### 2.4 Debtor Table (جدول المدين)
| Field | Arabic Label | Notes |
|---|---|---|
| Name / Surname | اسم و لقب المدين | Person's full name |
| Commercial Name | الاسم التجاري للمدين | For businesses |
| Address | عنوان المدين | Full address |
| Bank Account | الحساب الجاري | RIB / CCP account number |
| CNAS Number | رقم CNAS | Social security registration |
| NIF | NIF | Tax ID (رقم التعريف الجبائي) |
| NIS | NIS | Statistical ID (رقم التعريف الإحصائي) |
| Other Information | معلومات أخرى | Free text |
| Reasons (الأسباب) | الأسباب | Description of why the debt is owed |
| Liquidation Basis | أساس التصفية | Legal or contractual basis for amount |

### 2.5 Financial Summary
| Field | Arabic Label | Type |
|---|---|---|
| Amount to be Collected | المبلغ المراد تحصيله | `DECIMAL(15,2)` — BigDecimal |
| Amount in Words (Tafqeet) | يتضمن الأمر بالإيراد مبلغ | Auto-computed from numeric value |

### 2.6 Authorization Footer
| Field | Arabic Label | Notes |
|---|---|---|
| Place | حرر في | City of issuance |
| Date | تاريخ | Date of signature |
| Authorizing Officer Signature | الآمر بالصرف | Role-based, printed name |

---

## 3. Goals & Scope

### 3.1 In-Scope
- ✅ Create, read, update, and delete (soft-delete) Revenue Orders.
- ✅ Manage a Debtor Registry (المدينون).
- ✅ Budgetary imputation using Program/Sub-program hierarchy.
- ✅ Workflow: DRAFT → SUBMITTED → APPROVED → COLLECTED / CANCELLED.
- ✅ Support for Cancellation/Reduction Orders (Annexe 3 & 4 of Instruction 08).
- ✅ Generate Dispatch Slips (Bordereau d'envoi - Annexe 5).
- ✅ Print pixel-accurate replicas of the official Instruction 08 forms in PDF.
- ✅ Fiscal year gate on all write operations.
- ✅ Audit trail for all state changes.
- ✅ Arabic-primary UI with French labels available.
- ✅ Amount-to-words (Tafqeet / التفقيط) in Arabic.

### 3.2 Out-of-Scope (for mini-app phase)
- ❌ Full RBAC system (simplified: single admin user for now, full RBAC on WGEBUDG merge).
- ❌ Treasury perception recording (link to WGEBUDG's treasury_perception table on merge).
- ❌ Budget credit consumption tracking (belongs to the full WGEBUDG budget module).
- ❌ Multi-dossier support (single institution, configured once).

---

## 4. Architecture — Reusing WGEBUDG Patterns

The mini-app will follow the **exact same architectural conventions** as WGEBUDG to guarantee future merge compatibility.

### 4.1 Technology Stack
| Concern | Choice | Justification |
|---|---|---|
| Language | Java 21 | Same as WGEBUDG |
| UI Framework | JavaFX 21 | Same as WGEBUDG |
| Database | MariaDB4j (embedded) | Same as WGEBUDG — zero install |
| Build | Maven + `mvnw` | Same as WGEBUDG |
| CSS | Custom JavaFX CSS | Reuse WGEBUDG theme |
| Icons | Ikonli (FontAwesome) | Same as WGEBUDG |
| PDF/Print | JasperReports | Same as WGEBUDG report strategy |
| Logging | Log4j2 | Same as WGEBUDG |

### 4.2 Application Modes & Connection Setup (Inspired by GstockDz)
The application will support two modes managed via `AppSettings` (using `java.util.prefs.Preferences`):
*   **Local Mode:** Connects to an embedded database (MariaDB4j) or local MySQL instance. Default for standalone users.
*   **Server/Network Mode:** Connects to a centralized database server. Requires a Server Configuration screen (`ServerConfigController`) to set IP, Port, User, and Password via `DatabaseConnection.configure()`.

### 4.3 Authentication Flow (Inspired by GstockDz)
1.  **Initial Run:** Checks `AppSettings`. Prompts for Mode Selection (Local/Network).
2.  **Enterprise Info Check:** Before showing the login screen, checks if `InstitutionInfo` exists. If not, forces the user to the Setup Screen.
3.  **Login:** `LoginController` uses an `AuthenticationService` running inside the `ConcurrencyManager` to keep the UI responsive. UI elements are disabled with a `ProgressIndicator` during authentication.

### 4.4 Reporting Engine (JasperReports - Inspired by GstockDz)
The application will use a centralized `ReportService` with the following characteristics:
*   **Compilation Cache:** A `ConcurrentHashMap` stores compiled `JasperReport` objects to eliminate recompilation overhead.
*   **Compilation:** `.jrxml` files are loaded from `src/main/resources/reports/` and compiled dynamically using `JasperCompileManager`.
*   **Execution & Display:** Uses `JasperFillManager` to bind data (either via JDBC `Connection` or `JRBeanCollectionDataSource`). Displays the result using `JasperViewer` executed safely on the Swing Event Dispatch Thread via `SwingUtilities.invokeLater()`.

### 4.5 Layered Architecture (Identical to WGEBUDG)

```
┌─────────────────────────────────────────────────────────┐
│  Presentation Layer (JavaFX FXML + Controllers)          │
│  - RevenueOrderListController                            │
│  - RevenueOrderFormController                            │
│  - CancellationFormController                            │
│  - DispatchSlipController                                │
│  - DebtorManagementController                            │
│  - PrintPreviewController                                │
└────────────────────┬────────────────────────────────────┘
                     │ calls
┌────────────────────▼────────────────────────────────────┐
│  Service Layer (Business Logic & Rule Enforcement)        │
│  - RevenueOrderService    (workflow, validation)          │
│  - CancellationOrderService(annulations & reductions)     │
│  - DispatchSlipService    (bordereau grouping)            │
│  - DebtorService          (debtor CRUD)                  │
│  - PrintService           (JasperReports orchestration)  │
└────────────────────┬────────────────────────────────────┘
                     │ calls
┌────────────────────▼────────────────────────────────────┐
│  Repository Layer (JDBC Data Access)                     │
│  - RevenueOrderRepository                                │
│  - CancellationOrderRepository                           │
│  - DispatchSlipRepository                                │
│  - DebtorRepository                                      │
└────────────────────┬────────────────────────────────────┘
                     │ SQL
┌────────────────────▼────────────────────────────────────┐
│  Database (MariaDB4j embedded)                           │
│  - revenue_order table                                   │
│  - revenue_order_cancellation table                      │
│  - dispatch_slip (and _order) tables                     │
│  - debtor table                                          │
│  - budget_hierarchy table (pre-defined lookups)          │
│  - fiscal_year table (shared concept)                    │
│  - audit_log table (reused)                              │
└─────────────────────────────────────────────────────────┘
```

### 4.3 Cross-Cutting Utilities (Reused Directly from WGEBUDG)
| Utility | Role |
|---|---|
| `FiscalYearGuard` | Block writes if year is closed |
| `AuditService` | Append-only audit trail |
| `SessionContext` | Active user / dossier / year |
| `ConcurrencyManager` | Non-blocking background DB calls |
| `MoneyCalculationUtil` | BigDecimal precision for amounts |
| `CalculationConfig` | Centralized rounding mode (DOWN, 2dp) |
| `BilingualResolver` | AR/FR field resolution at runtime |
| `DatabaseConnection` | HikariCP connection pool |

---

## 5. Database Schema

### 5.1 Core Tables

#### `revenue_order` (أوامر الإيراد)
```sql
CREATE TABLE revenue_order (
    id                  BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
    order_number        VARCHAR(20)         NOT NULL,           -- Auto-generated, unique per year
    fiscal_year         SMALLINT UNSIGNED   NOT NULL,
    institution_code    VARCHAR(20)         NOT NULL DEFAULT 'MEC-HJM', -- Pre-configured
    -- Budgetary Imputation
    program_portfolio   VARCHAR(100)        DEFAULT NULL,       -- محفظة البرامج
    program             VARCHAR(100)        DEFAULT NULL,       -- البرنامج
    sub_program         VARCHAR(100)        DEFAULT NULL,       -- البرنامج الفرعي
    activity            VARCHAR(100)        DEFAULT NULL,       -- النشاط
    sub_activity        VARCHAR(100)        DEFAULT NULL,       -- النشاط الفرعي
    budget_title        TINYINT UNSIGNED    DEFAULT NULL,       -- العنوان (1 or 2)
    category_code       VARCHAR(20)         DEFAULT NULL,       -- الصنف
    sub_category_code   VARCHAR(20)         DEFAULT NULL,       -- الصنف الفرعي
    -- Debtor
    debtor_id           BIGINT UNSIGNED     DEFAULT NULL,       -- FK to debtor
    -- Financial
    amount              DECIMAL(15,2)       NOT NULL,           -- المبلغ المراد تحصيله
    reasons             TEXT                DEFAULT NULL,       -- الأسباب
    liquidation_basis   TEXT                DEFAULT NULL,       -- أساس التصفية
    -- Workflow
    status              ENUM('DRAFT','SUBMITTED','APPROVED','COLLECTED','CANCELLED')
                                            NOT NULL DEFAULT 'DRAFT',
    rejection_reason    VARCHAR(200)        DEFAULT NULL,
    -- Authorization Footer
    issued_at_city      VARCHAR(100)        DEFAULT NULL,       -- حرر في (المكان)
    issued_date         DATE                DEFAULT NULL,       -- تاريخ الإصدار
    authorizing_officer VARCHAR(100)        DEFAULT NULL,       -- الآمر بالصرف
    -- Audit
    created_at          DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by          VARCHAR(50)         DEFAULT NULL,
    updated_by          VARCHAR(50)         DEFAULT NULL,
    is_deleted          TINYINT(1)          NOT NULL DEFAULT 0,
    deleted_at          DATETIME            DEFAULT NULL,
    deleted_by          VARCHAR(50)         DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_revenue_order (fiscal_year, order_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Revenue Orders (أوامر الإيراد) — أمر بالإيراد document';
```

#### `debtor` (المدينون)
```sql
CREATE TABLE debtor (
    id              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
    -- Identity
    full_name       VARCHAR(100)        NOT NULL,    -- اسم و لقب المدين
    commercial_name VARCHAR(100)        DEFAULT NULL, -- الاسم التجاري
    address         VARCHAR(200)        DEFAULT NULL, -- عنوان المدين
    -- Bank Info
    account_number  VARCHAR(50)         DEFAULT NULL, -- الحساب الجاري
    -- Legal IDs
    cnas_number     VARCHAR(30)         DEFAULT NULL, -- رقم CNAS
    nif             VARCHAR(30)         DEFAULT NULL, -- NIF (رقم التعريف الجبائي)
    nis             VARCHAR(30)         DEFAULT NULL, -- NIS (رقم التعريف الإحصائي)
    other_info      VARCHAR(200)        DEFAULT NULL, -- معلومات أخرى
    -- Audit
    is_active       TINYINT(1)          NOT NULL DEFAULT 1,
    created_at      DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted      TINYINT(1)          NOT NULL DEFAULT 0,
    deleted_at      DATETIME            DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_debtor_name (full_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Debtor registry (المدينون) — referenceable across revenue orders';
```

#### `budget_hierarchy` (تصنيفات الميزانية)
```sql
CREATE TABLE budget_hierarchy (
    id                  BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
    fiscal_year         SMALLINT UNSIGNED   NOT NULL,
    program_portfolio   VARCHAR(100)        NOT NULL,
    program             VARCHAR(100)        NOT NULL,
    sub_program         VARCHAR(100)        NOT NULL,
    action              VARCHAR(100)        NOT NULL, -- النشاط
    sub_action          VARCHAR(100)        NOT NULL, -- النشاط الفرعي
    -- Used only for rétablissement de crédits
    budget_title        TINYINT UNSIGNED    DEFAULT NULL,
    category_code       VARCHAR(20)         DEFAULT NULL,
    sub_category_code   VARCHAR(20)         DEFAULT NULL,
    
    is_active           TINYINT(1)          NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    KEY idx_hierarchy_year (fiscal_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Pre-defined budget classification lookups per fiscal year';
```

#### `audit_log` — Reused identically from WGEBUDG schema.

#### `revenue_order_cancellation` (Annexes 3 & 4 — أوامر الإلغاء والتخفيض)
```sql
CREATE TABLE revenue_order_cancellation (
    id                  BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
    original_order_id   BIGINT UNSIGNED     NOT NULL,           -- FK -> revenue_order.id
    fiscal_year         SMALLINT UNSIGNED   NOT NULL,
    cancellation_type   ENUM('ANNULATION','REDUCTION') NOT NULL,
    cancelled_amount    DECIMAL(15,2)       NOT NULL,           -- المبلغ المُلغى/المُخفَّض
    reason              TEXT                NOT NULL,           -- Motif — obligatoire (INS-05)
    status              ENUM('DRAFT','SUBMITTED','APPROVED') NOT NULL DEFAULT 'DRAFT',
    issued_at_city      VARCHAR(100)        DEFAULT NULL,
    issued_date         DATE                DEFAULT NULL,
    created_at          DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(50)         DEFAULT NULL,
    is_deleted          TINYINT(1)          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    FOREIGN KEY (original_order_id) REFERENCES revenue_order(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Cancellation/Reduction orders — Annexes 3 and 4 of Instruction 08';
```

#### `dispatch_slip` (Bordereau d'envoi — Annexe 5)
```sql
CREATE TABLE dispatch_slip (
    id              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
    slip_number     VARCHAR(20)         NOT NULL,
    fiscal_year     SMALLINT UNSIGNED   NOT NULL,
    account_number  VARCHAR(50)         DEFAULT NULL,
    total_amount    DECIMAL(15,2)       NOT NULL DEFAULT 0,  -- Total of this slip (🔵 computed)
    previous_total  DECIMAL(15,2)       NOT NULL DEFAULT 0,  -- Cumul des bordereaux précédents
    general_total   DECIMAL(15,2)       NOT NULL DEFAULT 0,  -- total_amount + previous_total (🔵 computed)
    issued_date     DATE                DEFAULT NULL,
    created_at      DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(50)         DEFAULT NULL,
    is_deleted      TINYINT(1)          NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Dispatch slip (Bordereau d envoi) — Annexe 5 of Instruction 08';

CREATE TABLE dispatch_slip_order (
    slip_id         BIGINT UNSIGNED NOT NULL,
    order_id        BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (slip_id, order_id),
    FOREIGN KEY (slip_id)  REFERENCES dispatch_slip(id),
    FOREIGN KEY (order_id) REFERENCES revenue_order(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 5.2 SQL View for Reporting
```sql
CREATE OR REPLACE VIEW v_revenue_order_summary AS
SELECT
    ro.id,
    ro.order_number,
    ro.fiscal_year,
    ro.status,
    ro.amount,
    d.full_name                         AS debtor_name,
    d.commercial_name                   AS debtor_commercial_name,
    ro.reasons,
    ro.issued_date,
    ro.authorizing_officer
FROM revenue_order ro
LEFT JOIN debtor d ON d.id = ro.debtor_id
WHERE ro.is_deleted = 0;
```

---

## 6. Business Rules

Inheriting all **Cross-Cutting Rules** from WGEBUDG (`CC-01` through `CC-15`) and adding the following module-specific rules:

### 6.1 Revenue Order Rules (RO-xx)

| # | Rule | Type |
|---|---|---|
| RO-01 | `order_number` is auto-generated: format `IRV-{YYYY}-{NNNN}` (e.g. `IRV-2025-0001`), unique per fiscal year | 🔵 |
| RO-02 | A Revenue Order **must have a debtor** before it can be submitted | 🔴 |
| RO-03 | `amount` must be strictly **greater than zero** | 🔴 |
| RO-04 | `reasons` (الأسباب) is **mandatory** — an order without justification cannot be submitted | 🔴 |
| RO-05 | `liquidation_basis` (أساس التصفية) is **mandatory** before submission | 🔴 |
| RO-06 | An order can only be edited in `DRAFT` or `REJECTED` status | 🔴 |
| RO-07 | Rejection requires a non-empty `rejection_reason` | 🔴 |
| RO-08 | An order cannot be deleted if its status is `APPROVED` or `COLLECTED` | 🔴 |
| RO-09 | `issued_date` must not be in the future | 🔴 |
| RO-10 | Amount-in-words (Tafqeet / التفقيط) is always **computed** from the numeric `amount` — never input manually | 🔵 |
| RO-11 | Every status transition must be logged in the audit trail | 📋 |
| RO-12 | A `COLLECTED` order is **permanently locked** — no further edits | 🔴 |

### 6.2 Rules from Instruction n° 08 (INS-xx)

> See full analysis in [`instruction_08_2023_analysis.md`](./instruction_08_2023_analysis.md) — Section 5.

| # | Rule | Type |
|---|---|---|
| INS-01 | Every order must contain ALL information needed for the public accountant to perform recovery | 🔴 |
| INS-02 | Amount-in-words (Tafqeet) is ALWAYS computed — never accepted from user input | 🔵 |
| INS-03 | Fields TITRE and CATÉGORIE are mandatory ONLY for "rétablissement de crédits" operations | 🔴 |
| INS-04 | A cancellation order (Annexe 3) MUST reference an existing revenue order number + date | 🔴 |
| INS-05 | A reason (motif) is mandatory for any cancellation or reduction | 🔴 |
| INS-06 | The dispatch slip (Annexe 5) must auto-compute TOTAL ANTÉRIEUR and TOTAL GÉNÉRAL | 🔵 |
| INS-07 | The legal reference `Décret n° 46-93 du 06.02.1993` must appear verbatim in Annexes 1 & 2 | 🔴 |
| INS-08 | Two print documents are produced per event: one for accountant (Annexe 1 or 3), one for debtor (Annexe 2 or 4) | 🔵 |

### 6.3 State Machine

```
DRAFT ──► SUBMITTED ──► APPROVED ──► COLLECTED
  ▲            │              │
  │            ▼              ▼
  └──── REJECTED        CANCELLED
```

| Transition | Guard Condition |
|---|---|
| DRAFT → SUBMITTED | Has debtor + amount > 0 + reasons + liquidation_basis filled (INS-01) |
| SUBMITTED → APPROVED | Secretary General (الأمين العام) approves |
| SUBMITTED → REJECTED | Rejection reason must be provided |
| REJECTED → DRAFT | User revises and resubmits |
| APPROVED → COLLECTED | Collection confirmed by accountant |
| APPROVED → CANCELLED | Cancellation reason required; triggers Annexe 3/4 |
| DRAFT → CANCELLED | Always allowed |

---

## 7. UI/UX Plan

### 7.1 Screens

| Screen | Description | Instruction 08 Annexe |
|---|---|---|
| **Mode Selection** | Select Local vs Network DB connection | — |
| **Server Config** | Set DB host, port, user, pass | — |
| **Enterprise Info** | Setup Institution name, logo, treasury accounts | — |
| **Login Screen** | Username/password with async validation | — |
| **Fiscal Year Setup** | Select or create the active fiscal year | — |
| **Revenue Order List** | Searchable, filterable table by year/status | — |
| **Revenue Order Form** | Create/edit أمر بالإيراد | Annexe 1 + 2 |
| **Cancellation/Reduction Form** | Issue a cancellation or reduction against an existing order | Annexe 3 + 4 |
| **Dispatch Slip (Bordereau)** | Group orders into a dispatch slip for the accountant | Annexe 5 |
| **Debtor Management** | CRUD screen for the debtor registry | — |
| **Print Preview** | Preview all 5 Annexe templates before printing | All Annexes |
| **Audit Log View** | Read-only log of all changes | — |

### 7.2 Revenue Order Form Layout (Annexes 1 & 2)
The form mirrors the physical document exactly:
1. **Section 1 — Header**: Order number (auto-`IRV-YYYY-NNNN`), fiscal year, institution name.
2. **Section 2 — Budget Imputation**: Portfolio → Programme → Sous-programme → Action → Sous-Action (cascading dropdowns). Title/Category shown only when "Rétablissement de crédits" is checked (INS-03).
3. **Section 3 — Legal Text**: Read-only narrative (INS-07), auto-populated with debtor name and configured treasury account numbers.
4. **Section 4 — Debtor Table**: Debtor selector (search from registry) + inline display of all debtor fields.
5. **Section 5 — Amount**: Numeric input + auto-computed Arabic Tafqeet (INS-02).
6. **Section 6 — Footer**: City, date pickers, officer name.
7. **Action Bar**: Save Draft | Submit | Print Annexe 1 (Comptable) | Print Annexe 2 (Débiteur) | Cancel.

### 7.3 Print Report Design (JasperReports — 5 Templates)
| Template | Annexe | Recipient | Notes |
|---|---|---|---|
| `annexe1_ordre_recette.jrxml` | 1 | Comptable public | Primary revenue order |
| `annexe2_avis_emission.jrxml` | 2 | Débiteur | Same data, different header |
| `annexe3_ordre_annulation.jrxml` | 3 | Comptable public | References original order |
| `annexe4_avis_annulation.jrxml` | 4 | Débiteur | Same data as Annexe 3 |
| `annexe5_bordereau_envoi.jrxml` | 5 | Comptable public | Summary table of grouped orders |

**All templates:** RTL Arabic, institution letterhead, legal text with decree reference, Tafqeet for amounts.

---

## 8. Project Structure

```
amr-iirad/
├── pom.xml
├── mvnw / mvnw.cmd
├── src/
│   └── main/
│       ├── java/org/marrok/amriirad/
│       │   ├── AmrIiradApp.java              ← JavaFX Application entry point
│       │   ├── context/
│       │   │   └── SessionContext.java       ← Reused pattern
│       │   ├── model/
│       │   │   ├── RevenueOrder.java           ← Annexes 1 & 2
│       │   │   ├── RevenueOrder.Status.java    ← Inner enum
│       │   │   ├── CancellationOrder.java      ← Annexes 3 & 4 (NEW)
│       │   │   ├── DispatchSlip.java           ← Annexe 5 (NEW)
│       │   │   └── Debtor.java
│       │   ├── repository/
│       │   │   ├── RevenueOrderRepository.java
│       │   │   ├── CancellationOrderRepository.java  ← NEW
│       │   │   ├── DispatchSlipRepository.java       ← NEW
│       │   │   └── DebtorRepository.java
│       │   ├── service/
│       │   │   ├── RevenueOrderService.java    ← RO-xx + INS-xx rules
│       │   │   ├── CancellationOrderService.java     ← INS-04, INS-05 (NEW)
│       │   │   ├── DispatchSlipService.java          ← INS-06 totals (NEW)
│       │   │   ├── DebtorService.java
│       │   │   ├── PrintService.java           ← Handles all 5 Annexe templates
│       │   │   └── AuditService.java           ← Reused pattern
│       │   ├── controller/
│       │   │   ├── ModeSelectionController.java  ← NEW
│       │   │   ├── ServerConfigController.java   ← NEW
│       │   │   ├── EnterpriseInfoController.java ← NEW
│       │   │   ├── LoginController.java
│       │   │   ├── RevenueOrderListController.java
│       │   │   ├── RevenueOrderFormController.java
│       │   │   ├── CancellationFormController.java   ← NEW (Annexes 3 & 4)
│       │   │   ├── DispatchSlipController.java       ← NEW (Annexe 5)
│       │   │   ├── DebtorController.java
│       │   │   └── PrintPreviewController.java
│       │   └── util/
│       │       ├── DatabaseConnection.java     ← Reused pattern
│       │       ├── DatabaseSchemaManager.java
│       │       ├── FiscalYearGuard.java        ← Reused pattern
│       │       ├── MoneyCalculationUtil.java   ← Reused pattern
│       │       ├── TafqeetUtil.java            ← NEW: Arabic number-to-words
│       │       └── GeneralUtil.java
│       └── resources/org/marrok/amriirad/
│           ├── view/
│           │   ├── mode-selection-view.fxml
│           │   ├── server-config-view.fxml
│           │   ├── enterprise-info-view.fxml
│           │   ├── login-view.fxml
│           │   ├── order-list-view.fxml
│           │   ├── order-form-view.fxml
│           │   ├── cancellation-form-view.fxml         ← NEW
│           │   ├── dispatch-slip-view.fxml             ← NEW
│           │   ├── debtor-view.fxml
│           │   └── print-preview-view.fxml
│           ├── reports/
│           │   ├── annexe1_ordre_recette.jrxml         ← Comptable
│           │   ├── annexe2_avis_emission.jrxml         ← Débiteur
│           │   ├── annexe3_ordre_annulation.jrxml      ← Comptable
│           │   ├── annexe4_avis_annulation.jrxml       ← Débiteur
│           │   └── annexe5_bordereau_envoi.jrxml       ← Dispatch slip
│           ├── css/
│           │   └── (reuse WGEBUDG css files)
│           └── i18n/
│               ├── messages_fr.properties
│               └── messages_ar.properties
```

---

## 9. Development Phases

### Phase 0 — Project Bootstrap
- [x] Initialize Maven project with JavaFX 21 + MariaDB4j dependencies.
- [x] Copy and adapt shared utilities from WGEBUDG (DatabaseConnection, FiscalYearGuard, AuditService, etc.).
- [x] Implement `DatabaseSchemaManager` with all 5 tables: `revenue_order`, `debtor`, `revenue_order_cancellation`, `dispatch_slip`, `dispatch_slip_order`, `audit_log`.
- [x] Configure institution-specific constants (institution name, Wilaya, treasury account numbers, decree reference) as a single `InstitutionConfig` class.
- [x] Verify project compiles and the DB initializes correctly.

### Phase 1 — Core Data Layer
- [x] Implement `Debtor` model and `DebtorRepository` (full CRUD + soft-delete).
- [x] Implement `RevenueOrder` model with `Status` enum.
- [x] Implement `RevenueOrderRepository` (full CRUD + soft-delete + `findByYear`, `findByStatus`).
- [x] Implement `RevenueOrderService` with all RO-01 to RO-12 rules enforced.
- [x] Implement `TafqeetService` for Arabic number-to-words conversion (GstockDz dinar/centime pattern).
- [x] Implement `CancellationOrderService` (BR-03, BR-04, INS-04, INS-05).
- [x] Implement `DispatchSlipService` (BR-05, BR-06, INS-06).
- [x] Implement `AuditService` (append-only audit trail).
- [x] Add soft-delete columns (is_deleted, deleted_at, deleted_by) to revenue_order schema.

### Phase 2 — UI Screens
- [ ] Login screen & Fiscal year setup.
- [ ] Debtor Management CRUD screen.
- [ ] Revenue Order List screen (table with search/filter by status and year).
- [ ] Revenue Order Form screen (Annexes 1 & 2).
- [ ] Cancellation/Reduction Form screen (Annexes 3 & 4).
- [ ] Dispatch Slip screen (Annexe 5).

### Phase 3 — Print & Report
- [ ] Design all 5 `.jrxml` templates in JasperReports matching Instruction 08 layouts.
- [ ] Implement `PrintService` to compile and fill the reports.
- [ ] Implement `PrintPreviewController` for on-screen preview of all annexes before printing.
- [ ] Test print output for RTL Arabic layout accuracy and Tafqeet correctly populating.

### Phase 4 — Polish & Merge Readiness
- [x] Architectural Migration: Implemented strict Constructor Injection for DI.
- [x] Navigation Standardization: Standardized on Modal Dialogs for forms (GstockDz pattern).
- [x] Database Robustness: Added auto-seeding for Fiscal Years and schema migration fixes.
- [ ] Bilingual support (Arabic primary, French secondary).
- [ ] Audit log viewer screen.
- [ ] Full business rule test coverage.
- [ ] Document the merge strategy for integrating into WGEBUDG Module H.

---

## 10. Key Technical Challenges

| Challenge | Strategy |
|---|---|
| **Arabic RTL layout in JavaFX** | Use `NodeOrientation.RIGHT_TO_LEFT` on root nodes; test all form fields |
| **Tafqeet (التفقيط)** | Build dedicated `TafqeetUtil` using Algerian Dinar denomination rules |
| **JasperReports RTL** | Configure report locale to `ar_DZ`; use RTL-capable Arabic fonts (Amiri, Scheherazade) |
| **Budget Hierarchy Dropdowns** | Cascading ComboBox: Portfolio → Program → Sub-program → Activity → Sub-activity |
| **Future WGEBUDG merge** | Table names must not conflict. Prefix with `rev_` or use the existing `revenue_order` table in WGEBUDG schema |

---

## 11. Integration Point with WGEBUDG

When the mini-app is mature enough to merge into WGEBUDG:

| Mini-App Table | WGEBUDG Equivalent | Action on Merge |
|---|---|---|
| `revenue_order` | `revenue_entry` (partial overlap) | Extend `revenue_entry` schema or create dedicated `revenue_order` table |
| `debtor` | `debtor` table in WGEBUDG | **Direct reuse** — table already exists |
| `audit_log` | `audit_log` in WGEBUDG | **Direct reuse** — identical structure |
| `fiscal_year` | `fiscal_year` in WGEBUDG | **Direct reuse** — multi-dossier version |

The mini-app's `RevenueOrderService` maps directly to **Module H (Revenue)** of the WGEBUDG architecture proposal. The `RevenueOrderRepository` and `DebtorRepository` become additions to the existing `repository/` package.

---

## 12. Resolved Constraints & Stakeholder Decisions

Based on stakeholder feedback, the following design decisions have been finalized:

| Area | Decision | Impact on Implementation |
|---|---|---|
| **Institution Scope** | Single institution only. | The institution code (`MEC-HJM`) and details will be hardcoded/fixed in the configuration to keep the mini-app simple. |
| **Treasury Accounts** | Fixed account numbers. | The text block containing the RIB/CCP numbers will be static in the print template. |
| **Roles & Workflow** | Creator: Regular Employee<br>Approver: Secretary General (الأمين العام) | Simplified RBAC. We need at least two distinct user roles: `OPERATOR` (for data entry) and `APPROVER` (for final validation). |
| **Budget Hierarchy** | Predefined per fiscal year. | The UI will use predefined lookup lists (ComboBoxes) for Portfolio, Program, Sub-program, etc., rather than free-text fields. |
| **Historical Data** | Must support entering old records. | The system must allow backdating of `issued_date` and manual entry of past fiscal years to support data migration. |
| **Print Output** | Basic layout first, improve later. | The initial JasperReports template will focus on data accuracy and a clean layout, rather than being a pixel-perfect replica of the current physical form. |

---

*This plan is the contract between the analysis of the WGEBUDG project architecture and the implementation of the أمر بالإيراد mini-app.*
*Every 🔴 rule = one guard in `RevenueOrderService`.*
*Every 🔵 rule = one computed property — never accept from user input.*

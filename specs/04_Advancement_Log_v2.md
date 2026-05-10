# Advancement Log v2 — Amr-Iirad Stabilization
> **Latest Update:** 2026-05-10 (Afternoon)  
> **Current Focus:** Phase 6 — Maintenance & Feature Expansion

---

## 📊 Overall Progress

| Phase | Milestone | Status |
|---|---|---|
| **Phase 0** | Core Foundation & Refactoring | ✅ Complete |
| **Phase 1** | Data Layer & Persistence | ✅ Complete |
| **Phase 2** | Service Layer & Business Logic | ✅ Complete |
| **Phase 3** | UI Layer & Design System | ✅ Complete |
| **Phase 4** | Reporting & Jasper Integration | ✅ Complete |
| **Phase 5** | Stability, RTL & Final Polish | ✅ Complete |
| **Phase 6** | Maintenance & Security | 🚧 Active |

---

## 🔧 Recent Achievements (Stabilization Phase)

### 📅 2026-05-10: Full Bilingual Reporting & 2-JRXML Strategy
- ✅ **End-to-End Bilingual Support**: Implemented French fields across the entire order lifecycle, including Revenue Orders, Cancellations, Reductions, and Dispatch Slips.
- ✅ **2-JRXML Printing Strategy**: Adopted a robust reporting architecture using separate templates for Arabic (`_ar.jrxml`) and French (`_fr.jrxml`), ensuring high-fidelity layouts for both RTL and LTR orientations.
- ✅ **Bilingual Schema Migration**: Updated `revenue_order`, `revenue_order_cancellation`, and `institution_info` tables with parallel French columns (`object_fr`, `reason_fr`, `address_fr`, etc.).
- ✅ **On-the-Fly Language Selection**: Standardized a bilingual `Alert` dialog across all print actions, allowing users to choose the output language at the moment of generation.
- ✅ **Multi-Language Tafqeet**: Expanded `TafqeetService` and `ReportParamBuilder` to support automated amount-to-words conversion in both Arabic and French.
- ✅ **Enterprise Settings Expansion**: Added French support for Institution Address and Authorizing Officer in the settings view, ensuring compliance with bilingual administrative requirements.

### 📅 2026-05-10: Fiscal Year Scoping & Premium UI Refinement
- ✅ **Fiscal-Year-Scoped Budgeting**: Refactored the entire budget hierarchy to be specific to the active fiscal year. Budget chapters are now isolated by year, preventing data pollution across financial cycles.
- ✅ **Database Migration**: Implemented idempotent migration for `budget_chapter` table, adding `fiscal_year_id` and a `UNIQUE KEY uq_code_year (code, fiscal_year_id)`.
- ✅ **Repository Hardening**: Updated `BudgetChapterRepository` to strictly enforce year-based filtering for all query and update operations.
- ✅ **Design System Modularization**: Refactored CSS architecture by creating `buttons.css` and expanding `theme.css` design tokens. Moved over 60 lines of styles into modular components.
- ✅ **Premium UI/UX Polish**: Upgraded "Cancel" and "Reduce" action buttons with linear gradients, depth shadows, and interactive hover effects, while maintaining geometric consistency (8px radius).
- ✅ **Timeline Rendering Fix**: Resolved "broken icons" issue in the status timeline by adopting the dashboard's CSS pattern (`styleClass` based rendering) and Constructor-based `FontIcon` initialization in Java.
- ✅ **Global Theme Cleanup**: Successfully eliminated all inline hex codes and RGBA values from CSS and Controller files, routing all styling through centralized design tokens.

### 📅 2026-05-10 (Morning): Database Lifecycle & Server Configuration Hardening
- ✅ **Graceful Shutdown**: Implemented a JVM shutdown hook in `AmrIiradApp` to ensure the MariaDB connection pool is properly closed on application exit.
- ✅ **Advanced Server Configuration**: Updated `AppSettings` and `ServerConfigController` to support custom Database Names and Password persistence for remote connections.
- ✅ **Background Connection Testing**: Refactored the database initialization and connection test logic to run on background threads, ensuring a smooth, non-freezing UI during setup.
- ✅ **UX Labeling Overhaul**: Replaced cryptic regulatory labels (e.g., "Annexe 1") with clear, descriptive Arabic titles (e.g., "أمر الإيراد (المحاسب)") across all printing actions.
- ✅ **Premium UI Refinement**: Modernized the `ModeSelection` and `ServerConfig` views with GstockDz-inspired aesthetics, larger iconography, and responsive layouts.
- ✅ **Code Quality**: Hardened `DatabaseConnection` with synchronized shutdown logic and fixed `FontIcon` literal initialization in `OrderDetailsController`.

### 📅 2026-05-07 (Late Night): Reporting Unification & Design System Polish
- ✅ **Annexes 1-4 Printing**: Fully restored and enhanced order-specific reporting in `OrderDetailsController`. Support added for Annex 1 (Admin Order), Annex 2 (Debtor), Annex 3 (Cancellation), and Annex 4 (Reduction).
- ✅ **Cancellation & Reduction Workflows**: Integrated `CancellationOrderService` into the Order Details view, enabling secure, RBAC-guarded order lifecycle management.
- ✅ **GstockDz Design Import**: Successfully imported and adapted the premium table styling from GstockDz. Created `tableview.css` with improved spacing, zebra-padding, and hover effects.
- ✅ **CSS Orchestration Refactor**: Unified the styling system by routing all styles through `app.css` → `master.css` → `tableview.css` / `theme.css`.
- ✅ **Global Typography**: Enforced the `Cairo` font-family application globally at the `.root` level for professional Arabic UI consistency.
- ✅ **Settings Navigation**: Fixed navigation dead-ends in Enterprise and Audit Log views by standardizing on `BorderPane` with integrated `TopBar` back-button support.

### 📅 2026-05-07 (Night): Security Hardening & Dispatch Module
- ✅ **Full RBAC Enforcement**: Integrated `AuthService` into all core controllers (`RevenueOrderList`, `RevenueOrderForm`, `DebtorList`, `Dashboard`).
- ✅ **Action-Level Permissions**: Secured critical actions (Create, Edit, Print) with `authService.canDo()` checks and user-friendly error dialogs.
- ✅ **Dispatch Slips Activation**: Enabled the "Bordereaux d'Envoi" feature, registered controllers in `AppContext`, and fixed FXML controller resolution issues.
- ✅ **Permission Matrix Expansion**: Added granular permissions (`dispatch.*`, `settings.manage`, `budget_chapter.manage`) to the database seed logic.

### 📅 2026-05-07 (Late Evening): Compilation & Runtime Stabilization
- ✅ **BaseFormController Pattern**: Introduced a unified base class for all modal forms to handle window management, standard error display, and concurrency.
- ✅ **Strict Validation Enforcement**: Standardized the `validateForm()` method across all controllers with proper `@Override` and visibility protection.
- ✅ **Compilation Fixes**: Resolved major build blockers related to abstract method mismatches in `BudgetChapterFormController` and `OrderDetailsController`.
- ✅ **Runtime Fixes**: Resolved CSS parsing errors by replacing unsupported standard CSS `var()` syntax with JavaFX-specific variable definitions.
- ✅ **Ikonli Resolver Fix**: Fixed application crashes in Audit Log view by replacing missing Material Design icons with standard FontAwesome equivalents.
- ✅ **Audit Log Integration**: Finalized the `AuditLogController` and `AuditLogService` integration, enabling administrative tracking of all system changes.

### 📅 2026-05-07 (Evening): UX Expansion & Testing Milestone
- ✅ **Edit Debtor Feature**: Implemented double-click editing on debtor list with `handleEditDebtor()` modal flow.
- ✅ **Quick-Add Budget Chapter**: Added inline `+` button in order form (`budget-chapter-form-view.fxml`) with hierarchical level support (Titre/Chapitre/Article/Paragraphe).
- ✅ **BudgetChapterFormController**: Built complete controller with parent chapter loading and validation.
- ✅ **Order Details Modal**: Created read-only `order-details-view.fxml` with comprehensive order info display.
- ✅ **Status Timeline UI**: Implemented visual status progression (DRAFT → ISSUED → DISPATCHED/CANCELLED/REDUCED) with icons and timestamps.
- ✅ **OrderDetailsController**: Built full controller with report print buttons and status-aware timeline generation.
- ✅ **Smart Modal Routing**: Updated `RevenueOrderListController` to route DRAFT orders → edit form, non-DRAFT orders → details modal.
- ✅ **Comprehensive Unit Tests**: Created `RevenueOrderServiceTest` with 20+ test cases covering:
  - Order creation, validation, and business rules (RO-01 to RO-12)
  - Amount validation, debtor requirements, fiscal year checks
  - State transitions (DRAFT → ISSUED → DISPATCHED)
  - Deletion restrictions for locked orders
  - Query operations and mock repository integration
- ✅ **Test Infrastructure**: Added JUnit 5, Mockito dependencies, and Maven Surefire plugin for test execution.
- ✅ **AppContext Registration**: Registered `OrderDetailsController` and `BudgetChapterFormController` in DI container.

### 📅 2026-05-07 (Morning): The Stability & RTL Milestone
- ✅ **Database Locking Fix**: Resolved multiple instance locks in Local mode by implementing `running` state tracking in `EmbeddedDatabase`.
- ✅ **Navigation Stability**: Fixed a critical bug where saving Enterprise settings would close the app; implemented smart redirects to Dashboard/Login.
- ✅ **UI Component Sync**: Fixed `LoadException` crashes by aligning FXML field types (TextField vs TextArea) in `DebtorFormController`.
- ✅ **Global RTL Implementation**: Enforced Right-To-Left orientation at the Scene level for all windows and modals.
- ✅ **Cairo Font Integration**: Integrated and globally applied the Cairo font family for professional Arabic typography.
- ✅ **Audit Log Viewer**: Created comprehensive UI to browse the system audit trail with table-based visualization.
- ✅ **Specs Reorganization**: Established a chronological naming convention for documentation and created a `legacy/` subfolder.
- ✅ **Workflow Automation**: Introduced `specs-maintenance.md` workflow to ensure documentation stays updated during dev.

### 📅 2026-05-06: The Great Refactor
- ✅ **Modular Package Migration**: Successfully migrated to a feature-based structure (`orders`, `debtors`, `users`).
- ✅ **DI Consolidation**: Registered all controllers in `AppContext` for reliable dependency injection.
- ✅ **Theme Consolidation**: Consolidated HSL-based theme tokens and standardized UI components.

---

## 🎯 Next Objectives
8. ✅ **Fiscal Scoping**: Refactored budget management to be year-isolated.
9. 🚧 **Copy to Next Year**: Planned feature to clone budget hierarchies across fiscal years.


# Technical Manifest: Amr-Iirad State of the Union
> **Date:** 2026-05-07  
> **Status:** Production-Ready Core / Refactored & Polished  
> **Context:** This document serves as the primary "fingerprint" for any future AI agent or developer taking over this project. It summarizes the architectural truth as of May 7th, 2026.

---

## 1. Architectural Backbone
The application has transitioned from a flat structure to a **Feature-Based Modular Architecture**. This is non-negotiable for all future development.

### 1.1 Package Structure
- `org.marrok.amriirad.controller.[feature]`: Each feature (orders, debtors, dispatch, users, login, settings) has its own sub-package.
- `org.marrok.amriirad.core`: Contains the "Engine" — `AppContext` (DI Container) and `ConcurrencyManager`.
- `org.marrok.amriirad.service`: Business logic layer.
- `org.marrok.amriirad.repository`: Data access layer.
- `org.marrok.amriirad.util`: Cross-cutting concerns (`SceneManager`, `DialogHelper`, `AppSettings`).

### 1.2 Dependency Injection (DI)
We use a **Strict Constructor Injection** pattern managed by `AppContext`.
- **NO** static fields for services/repositories in controllers.
- **NO** manual instantiation of controllers in FXML or other controllers.
- Everything MUST be registered in `AppContext.createInstance()`.

### 1.3 Navigation & View Management
- **`SceneManager`**: The single source of truth for scene transitions and modal dialogs.
- **`BaseFormController`**: Abstract base for all modal controllers, enforcing standard `validateForm()` and `getLogger()` patterns.
- **Root Layout**: All main views are wrapped in a `BorderPane` that includes `top-bar.fxml` and `footer.fxml`.
- **State Awareness**: `SceneManager` tracks the `lastLoadedFxml` to support the `refresh()` method, which is triggered when global state (like Fiscal Year) changes.

---

## 2. Design System: "The GstockDz Premium Look"
The application adheres to high-end design principles to ensure a professional, modern feel, utilizing the `Cairo` font family for all Arabic text.

### 2.1 CSS Strategy (Tiered Orchestration)
The CSS system is now structured to prevent style leakage and ensure theme consistency:
- **`app.css`**: The main entry point loaded by `SceneManager`. It acts as the orchestrator.
- **`master.css`**: Defines global application patterns, typography, and imports the base design system.
- **`tableview.css`**: Specialized styling for data grids (imported from GstockDz), providing advanced spacing, zebra-padding, and hover states.
- **`theme.css`**: Defines the foundational design tokens (HSL colors, status-aware variables, and shadows).

### 2.2 UI Principles & Components
- **Typography**: The `Cairo` font is mandatory and applied at the `.root` level via `master.css`.
- **TopBar**: Contains global actions (Back, Fiscal Year selection, User Management, Settings, Logout).
- **Footer**: Displays system status and current user info.
- **Data Grids**: All tables should use the standard `TableView` or `.data-table` classes to benefit from the premium `tableview.css` styling.
- **Cards**: Use the `.card` class for container-based layouts to achieve the modern, elevated look.

---

## 3. Data & Workflow Truths
- **Fiscal Year Gate**: Almost all data operations are scoped to the `active` fiscal year.
- **Audit Trail**: Every significant business action (Save, Issue, Print) MUST be logged via `AuditService`.
- **Tafqeet**: Numeric amounts are automatically converted to Arabic words via `TafqeetService`. NEVER accept manual text input for amounts in words.
- **Soft Deletion**: Records are never permanently deleted from the DB; they are marked `is_deleted = 1`.

---

## 4. Pending / Next Steps
1. ✅ **Audit Log Viewer**: A dedicated UI to browse the `audit_log` table — **IMPLEMENTED**.
2. ✅ **Permission Enforcement**: Integrated a comprehensive Permission Matrix using `authService.canDo()` across all core controllers and UI actions — **IMPLEMENTED**.
3. **Report Fine-Tuning**: Ensure all 5 Jasper Annexes align with the latest regulatory tweaks mentioned in `instruction_08_2023_analysis.md`.

---
*If you are the next AI agent: Maintain the pattern. If you need to add a feature, create a sub-package in `controller/` and `view/`, register the controller in `AppContext`, and use `SceneManager` for navigation.*

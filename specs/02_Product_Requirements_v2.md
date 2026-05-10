# PRD v2 — Revenue Order System (Amr-Iirad)
> **Refined Goal:** A stabilized, multi-user, premium desktop application for digitizing the full revenue order lifecycle.

---

## 1. Executive Summary
The Amr-Iirad system has evolved from a simple digitizer into a robust, secure, and aesthetically premium desktop application. Version 1.0 has been successfully delivered, featuring a modular architecture, multi-user authentication, and a design system specifically optimized for the Algerian administrative context (RTL, Cairo font, GstockDz-inspired).

## 2. Core Functional Pillars (Current State)

### 2.1 Security & Multi-User Support
- **RBAC (Role-Based Access Control):** Integrated `AuthService` with roles (ADMIN, USER).
- **Secure Login:** Encrypted passwords and session-based navigation.
- **Enterprise Identity:** Centralized enterprise settings with smart redirect logic.

### 2.2 Revenue Order Lifecycle
- **Dynamic Creation:** Auto-generated order numbers (YYYY-NNN) and automated "Tafqeet" (Amount to Arabic Words).
- **State Management:** Tracking orders through `DRAFT` -> `ISSUED` -> `DISPATCHED` -> `CANCELLED`/`REDUCED`.
- **Relational Integrity:** Seamless linking between Fiscal Years, Debtors, and Budget Chapters.

### 2.3 Debtor & Dispatch Management
- **Comprehensive Profiles:** Detailed debtor tracking including CNAS, NIF, NIS, and Bank Accounts.
- **Batch Dispatching:** Collective "Dispatch Slips" (Bordereaux) for sending multiple issued orders to the treasury.

### 2.4 Reporting Excellence
- **Five Mandatory Annexes:** Pixel-perfect JasperReports for all mandatory documents.
- **Bilingual Core:** Full support for Arabic (AR) and French (FR) documentation.
- **2-JRXML Strategy:** Specialized templates for each language (`_ar.jrxml` and `_fr.jrxml`) ensuring linguistic accuracy and cultural compliance.
- **On-the-fly Selection:** Interactive language choosing at the moment of printing.

## 3. Design System & UX
- **Premium Aesthetics:** GstockDz-inspired UI with HSL colors, smooth transitions, and shadow-based cards.
- **Full RTL Support:** Native Right-To-Left layout orientation at the Scene level for Arabic mode.
- **Bilingual Inputs:** Dedicated fields for French translations of objects, reasons, and institution info.
- **Modern Typography:** High-quality **Cairo** font integration for superior Arabic legibility.
- **Smart Modals:** Standardized modal window management with auto-sizing and centering.

## 4. Technical Architecture
- **Language:** Java 21 / JavaFX 21.
- **Database:** MariaDB (Embedded MariaDB4j for Local mode, full MariaDB for Server mode).
- **Pattern**: Feature-based modular structure with Constructor-based Dependency Injection via `AppContext`.
- **UI Architecture**: FXML-First design. All UI views are strictly separated from logic, utilizing declarative FXML templates and CSS styling, eliminating programmatic UI construction.
- **Persistence**: HikariCP connection pooling with transaction safety.

## 5. Future Roadmap
- [x] Phase 5.1: **Enhanced Visualization** — Modern order details view and visual audit timeline.
- [x] Phase 5.2: **Extended CRUD** — Implement "Edit Debtor" functionality and direct "Budget Chapter" creation from the order form.
- [x] Phase 5.3: **Security & QA** — Full RBAC integration, Dispatch Slips activation, and unit test coverage.
- [x] Phase 6.1: **Bilingual Reporting** — Implement French documentation support and 2-JRXML printing strategy.
- [ ] Phase 6.2: **Data Export** — CSV/Excel export for financial auditing.

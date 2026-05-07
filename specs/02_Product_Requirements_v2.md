# PRD v2 — Revenue Order System (Amr-Iirad)
> **Refined Goal:** A stabilized, multi-user, premium desktop application for digitizing the full revenue order lifecycle.

---

## 1. Executive Summary
The Amr-Iirad system has evolved from a simple digitizer into a robust, secure, and aesthetically premium desktop application. It now features a modular architecture, multi-user authentication, and a design system specifically optimized for the Algerian administrative context (RTL, Cairo font, GstockDz-inspired).

## 2. Core Functional Pillars (Current State)

### 2.1 Security & Multi-User Support
- **RBAC (Role-Based Access Control):** Integrated `AuthService` with roles (ADMIN, USER).
- **Secure Login:** Encrypted passwords and session-based navigation.
- **Enterprise Identity:** Centralized enterprise settings with smart redirect logic.

### 2.2 Revenue Order Lifecycle
- **Dynamic Creation:** Auto-generated order numbers (YYYY-NNN) and automated "Tafqeet" (Amount to Arabic Words).
- **State Management:** Tracking orders through `DRAFT` → `ISSUED` → `DISPATCHED` → `CANCELLED`/`REDUCED`.
- **Relational Integrity:** Seamless linking between Fiscal Years, Debtors, and Budget Chapters.

### 2.3 Debtor & Dispatch Management
- **Comprehensive Profiles:** Detailed debtor tracking including CNAS, NIF, and Bank Accounts.
- **Batch Dispatching:** Collective "Dispatch Slips" (Bordereaux) for sending multiple issued orders to the treasury.

### 2.4 Reporting Excellence
- **Five Mandatory Annexes:** Pixel-perfect JasperReports for:
  1. Original Revenue Order (Annexe 1)
  2. Debtor Copy (Annexe 2)
  3. Full Cancellation Order (Annexe 3)
  4. Reduction Order (Annexe 4)
  5. Dispatch Slip (Annexe 5)

## 3. Design System & UX
- **Premium Aesthetics:** GstockDz-inspired UI with HSL colors, smooth transitions, and shadow-based cards.
- **Full RTL Support:** Native Right-To-Left layout orientation at the Scene level.
- **Modern Typography:** High-quality **Cairo** font integration for superior Arabic legibility.
- **Smart Modals:** Standardized modal window management with auto-sizing and centering.

## 4. Technical Architecture
- **Language:** Java 21 / JavaFX 21.
- **Database:** MariaDB (Embedded MariaDB4j for Local mode, full MariaDB for Server mode).
- **Pattern:** Feature-based modular structure with Constructor-based Dependency Injection via `AppContext`.
- **Persistence:** HikariCP connection pooling with transaction safety.

## 5. Future Roadmap
- [x] Phase 5.1: **Enhanced Visualization** — Modern order details view and visual audit timeline.
- [x] Phase 5.2: **Extended CRUD** — Implement "Edit Debtor" functionality and direct "Budget Chapter" creation from the order form.
- [x] Phase 5.3: **Security & QA** — Full RBAC integration, Dispatch Slips activation, and unit test coverage.


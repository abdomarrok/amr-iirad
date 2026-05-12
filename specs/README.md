# Amr-Iirad Specifications & Design Docs
> **Status:** Production-Ready Core / Hardened & Optimized
> **Institution:** School of Magistracy (Algeria)
> **Regulation:** Decree 24-358 (November 2024)

This directory contains the authoritative documentation for the Amr-Iirad enterprise application. Any development or architectural changes must be reflected here.

## 📂 Directory Index

### 1. [01_Legal_Reference.md](01_Legal_Reference.md)
*Legal foundation and field-level requirements derived from Decree 24-358.*
- Summary of the 6 mandatory Annexes.
- Business rules for adjustments (Cancellation, Reduction, Increase).
- Zero-Value Decision workflow.

### 2. [02_Product_Requirements.md](02_Product_Requirements.md)
*The high-level PRD (Product Requirements Document).*
- Core Functional Pillars (RBAC, Lifecycle, Reporting).
- Design System & UX goals.
- Implementation Roadmap.

### 3. [03_Technical_Architecture.md](03_Technical_Architecture.md)
*The system "Fingerprint" for developers and AI agents.*
- Feature-Based Modular Architecture.
- Strict Dependency Injection (AppContext).
- Application Startup Flow (Mermaid diagram).
- Reporting Engine (2-JRXML Strategy).

### 4. [04_Advancement_Log.md](04_Advancement_Log.md)
*Chronological history of features, bug fixes, and refactoring milestones.*
- Track record of stabilization phases.
- Current project status and next objectives.

### 5. [05_Coding_Standards.md](05_Coding_Standards.md)
*Mandatory development rules and patterns.*
- **Project File Tree**.
- FXML-First Architectural Pattern.
- Design System Tokens & Geometry.
- Concurrency & Non-blocking UI rules.
- Report Parameter Standards (Null-safety).

---

## 🏛️ Legal Assets
- **[legal/Decree_24-358.pdf](legal/Decree_24-358.pdf)**: The primary regulatory authority for the current version.
- **[legal/Instruction_08-2023_AR.pdf](legal/Instruction_08-2023_AR.pdf)**: Historical reference for base models (Arabic).
- **[legal/Instruction_08-2023_FR.pdf](legal/Instruction_08-2023_FR.pdf)**: Historical reference for base models (French).

---

## 🕰️ Archive & Audits
- **[PRODUCT_POLISH_AUDIT.md](PRODUCT_POLISH_AUDIT.md)**: Detailed checklist of UI/UX improvements.
- **[legacy/](legacy/)**: Outdated versions of PRDs, logs, and initial planning documents.

# Coding Skills & Best Practices — Amr-Iirad

This document captures the architectural patterns and development standards established during the stabilization of the Amr-Iirad application.

## 🎨 Design System & CSS Architecture

### 1. Centralized Design Tokens
Avoid hardcoding colors (Hex/RGBA) in specific components. Always use `theme.css` to define tokens.
- **Bad**: `-fx-background-color: #0e7c7b;`
- **Good**: `-fx-background-color: -fx-theme-primary;`

### 2. Modular CSS Imports
Keep stylesheets manageable by splitting them by concern and importing them into `master.css`.
- `theme.css`: Design tokens and variables.
- `buttons.css`: All button variants and hover states.
- `tableview.css`: Data grid styling.
- `app.css`: Global reset, fonts, and layout.

### 3. Premium UI Effects
To create a "premium" feel while maintaining consistency:
- **Geometry**: Stick to a consistent corner radius (e.g., `8px`) across the app.
- **Depth**: Use `dropshadow` with low opacity (e.g., `rgba(0,0,0,0.05)`) for cards and subtle glows for buttons.
- **Interaction**: Use `linear-gradient` for primary actions and apply `translate-y` transitions on hover for a tactile response.

##  ikonli Icon Implementation

### 1. Rendering Consistency
When creating icons dynamically in Java (instead of FXML), follow the **Dashboard Pattern**:
- **Pattern**: Initialize `FontIcon` with the literal in the constructor, then apply a `styleClass`.
- **Why**: Inline styles (`setStyle`) can sometimes fail to trigger the correct glyph rendering if the icon pack isn't fully initialized for that specific node.

```java
// ✅ Recommended Java Pattern
FontIcon icon = new FontIcon("fas-check-circle");
icon.getStyleClass().add("icon-primary"); // Defined in app.css
```

### 2. Compatibility
Stick to standard literals (e.g., `fas-edit`, `fas-check`, `fas-trash`) from FontAwesome Solid to ensure 100% compatibility across different OS rendering engines.

## 💾 Data Modeling & Persistence

### 1. Fiscal Year Scoping
For financial applications, data must be isolated by fiscal period to prevent cross-year pollution.
- **Pattern**: Every hierarchical entity (like `BudgetChapter`) must include a `fiscal_year_id`.
- **Database**: Enforce integrity using unique constraints across the year:
  ```sql
  UNIQUE KEY uq_code_year (code, fiscal_year_id)
  ```

### 2. Idempotent Migrations
When updating schemas, use patterns that allow the code to run multiple times without failure.
- Use `IF NOT EXISTS` for columns.
- Use `SELECT COUNT(*)` checks before adding keys or constraints in `DatabaseSchemaManager`.

## ⚡ Concurrency & UX

### 1. Non-Blocking UI
Never perform I/O (Database or Network) on the JavaFX Application Thread.
- Use `ConcurrencyManager` to run tasks asynchronously.
- Always provide a callback for UI updates to ensure thread safety.

### 2. Form Lifecycle
Use the `BaseFormController` pattern to standardize:
- Window closing (`closeWindow`).
- Validation (`validateForm`).
- Error reporting (`showError`).

## 🏗️ FXML-First Architectural Pattern

### 1. Declarative UI Enforcement
Strictly avoid programmatic UI construction (e.g., `new VBox()`, `new Label()`, `new Button()`). All UI structure must reside in `.fxml` files.
- **Why**: Ensures clean separation of concerns, enables non-developers to review layouts, and simplifies CSS mapping.
- **Exception**: Very simple containers used purely for spacing or wrapper purposes where no styling is required.

### 2. Component Modularization
For dynamic UI elements (like timeline items, grid rows, or repeated cards), create specialized component-level FXML templates.
- **Pattern**: Load the template via `FXMLLoader` and inject it into the main container.
- **Example**: `timeline-item.fxml` loaded repeatedly for an order's status history.

### 3. Controller Dependency Injection
Never instantiate controllers manually. Always retrieve them from `AppContext` or let `SceneManager` handle the loading.
- **Strict Rule**: Use constructor-based injection for services and repositories within controllers.
- **Pattern**: Register every new controller in `AppContext.createInstance()` to maintain a single source of truth for dependencies.

## 📋 Report Parameter Standard
To prevent "null" placeholders and broken layouts in JasperReports:
- **Fallback to Empty**: Always use `val != null ? val : ""` for all report parameters.
- **Unified Naming**: Use the standardized parameter dictionary (e.g., `REASON_AR`, `LIQUIDATION_BASIS`, `ORDONNATEUR_CODE`) defined in `ReportParamBuilder`.
- **Localization**: Pass the `PrintLanguage` to the builder to ensure automatic mapping of institutional metadata (Ministry, Treasury, etc.).

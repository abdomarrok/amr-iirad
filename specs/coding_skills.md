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

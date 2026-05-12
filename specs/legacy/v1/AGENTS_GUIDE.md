# Agent & Developer Guide: Amr-Iirad Development Standards

This document serves as the authoritative guide for coding patterns, architectural rules, and development workflows for the **Amr-Iirad** sub-project. All future developers and AI agents MUST follow these patterns to maintain system stability and UI consistency.

---

## 1. Architectural Patterns

### 1.1 Strict Dependency Injection (DI)
We use a lightweight, manual DI container: `org.marrok.amriirad.core.AppContext`.
- **Rule**: NEVER use `new MyController()` or static service fields.
- **Workflow**: 
  1. Add your service/repository to `AppContext` as a private field.
  2. Initialize it in the `AppContext` constructor.
  3. Register your controller in the `createInstance()` method.
  4. Inject dependencies via the Controller's constructor.

### 1.2 Feature-Based Modularity
Packages are organized by feature, not by layer.
- **Pattern**: `org.marrok.amriirad.[controller|view].[feature_name]`
- **Example**: `controller.orders`, `controller.debtors`, etc.
- **Rule**: Keep FXML files in a `view/` subfolder that mirrors the controller package structure.

### 1.3 Modal & Form Pattern (`BaseFormController`)
Every form or modal window must extend `BaseFormController`.
- **Benefit**: Standardizes window closing, error handling, and logging.
- **Requirement**: You must override `validateForm()` to implement business rules before saving.
- **Pattern**: 
  ```java
  public class MyFormController extends BaseFormController {
      @Override
      protected boolean validateForm() {
          // implementation
      }
  }
  ```

---

## 2. UI & Design System Rules

### 2.1 CSS Orchestration
The application uses a tiered CSS loading strategy. Do not modify `theme.css` or `master.css` for one-off tweaks.
1. `app.css`: The main entry point (orchestrator).
2. `master.css`: Global typography and layout rules.
3. `tableview.css`: Premium data grid styles (Imported from GstockDz).
4. `theme.css`: HSL design tokens.

**Mandatory Rule**: All Arabic text MUST use the `Cairo` font family, which is applied at the `.root` level in `master.css`.

### 2.2 Navigation via `SceneManager`
All scene transitions and modal opens MUST go through `org.marrok.amriirad.util.SceneManager`.
- **Main View**: `SceneManager.loadScene(stage, "/path/to/view.fxml")`
- **Modal**: `SceneManager.openModal(owner, "/path/to/view.fxml", "Title")`
- **Standard**: Always wrap main views in a `BorderPane` with `top-bar.fxml` and `footer.fxml`.

### 2.3 Async Data Loading
To keep the UI responsive, use the `AsyncTableLoader` for all `TableView` data fetching.
- **Pattern**:
  ```java
  tableLoader = new AsyncTableLoader<>(concurrencyManager, tableView, loadingIndicator);
  tableLoader.load(() -> myRepository.findAll());
  ```

---

## 3. Data & Business Logic Rules

### 3.1 Fiscal Year Scoping
- **Rule**: Every data query (Orders, Receipts, etc.) MUST be filtered by the currently active Fiscal Year.
- **Source**: Get the active year via `AppSettings.getFiscalYear()`.

### 3.2 Security & RBAC
- **Rule**: Never assume a user has permission. Use `AuthService.canDo(permission)` before showing a button or executing an action.
- **Checklist**: 
  - `orders.create`, `orders.edit`, `orders.print`
  - `debtors.manage`
  - `users.manage`

### 3.3 Soft Deletion
- **Rule**: Records are never `DELETE`d from the database.
- **Implementation**: Set `is_deleted = 1` and ensure all repositories filter for `is_deleted = 0` in their `findAll()` and `findById()` methods.

---

## 4. Development Workflow for Agents

1. **Analysis**: Check `specs/03_Technical_Architecture.md` for the current state of the union.
2. **Implementation**: 
   - Create Model → Repository → Service → Controller → FXML.
   - Register in `AppContext`.
3. **Styling**: Use existing classes (`.card`, `.btn-primary`, `.text-heading`). 
4. **Validation**: Use `mvnw.cmd clean compile` from the `amr-iirad` directory to verify your changes.
5. **Documentation**: Log your progress in `specs/04_Advancement_Log_v2.md`.

---
*Follow these patterns and the user will be happy. Break them, and the system will become unmaintainable.*

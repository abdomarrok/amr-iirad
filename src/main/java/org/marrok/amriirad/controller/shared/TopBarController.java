package org.marrok.amriirad.controller.shared;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.marrok.amriirad.core.AppContext;
import org.marrok.amriirad.model.FiscalYear;
import org.marrok.amriirad.repository.FiscalYearRepository;
import org.marrok.amriirad.service.AuthService;
import org.marrok.amriirad.util.DialogHelper;
import org.marrok.amriirad.util.SceneManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ResourceBundle;

public class TopBarController implements Initializable {

    private static final Logger logger = LogManager.getLogger(TopBarController.class);

    @FXML private Button backBtn;
    @FXML private ComboBox<FiscalYear> fiscalYearCombo;
    @FXML private Button addFiscalYearBtn;
    @FXML private Button viewUsersBtn;
    @FXML private Button managePermissionsBtn;
    @FXML private Button enterpriseSettingsBtn;
    @FXML private Button viewAuditLogBtn;
    @FXML private Button logoutBtn;
    @FXML private HBox fiscalYearBox;

    private final FiscalYearRepository fyRepo;
    private final AuthService authService;

    public TopBarController(FiscalYearRepository fyRepo, AuthService authService) {
        this.fyRepo = fyRepo;
        this.authService = authService;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        checkPermissions();
        loadFiscalYears();
        
        fiscalYearCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isActive()) {
                try {
                    fyRepo.setActive(newVal.getId());
                    // Refresh current scene
                    Stage stage = (Stage) fiscalYearCombo.getScene().getWindow();
                    SceneManager.refresh(stage);
                } catch (java.sql.SQLException e) {
                    logger.error("Failed to set active fiscal year", e);
                }
            }
        });
    }

    public void setBackVisible(boolean visible) {
        backBtn.setVisible(visible);
        backBtn.setManaged(visible);
    }

    public ComboBox<FiscalYear> getFiscalYearCombo() {
        return fiscalYearCombo;
    }

    private void checkPermissions() {
        setBtnVisible(addFiscalYearBtn, authService.canDo("settings.manage"));
        setBtnVisible(viewUsersBtn, authService.canDo("users.manage"));
        setBtnVisible(managePermissionsBtn, authService.canDo("users.manage"));
        setBtnVisible(enterpriseSettingsBtn, authService.canDo("settings.manage"));
        setBtnVisible(viewAuditLogBtn, authService.canDo("settings.manage"));
    }

    private void setBtnVisible(Button btn, boolean visible) {
        if (btn != null) {
            btn.setVisible(visible);
            btn.setManaged(visible);
        }
    }

    private void loadFiscalYears() {
        try {
            fiscalYearCombo.getItems().setAll(fyRepo.findAll());
            // Select active or latest
            fiscalYearCombo.getItems().stream()
                .filter(FiscalYear::isActive)
                .findFirst()
                .ifPresent(fy -> fiscalYearCombo.getSelectionModel().select(fy));
        } catch (java.sql.SQLException e) {
            logger.error("Failed to load fiscal years in TopBar", e);
        }
    }

    @FXML
    private void handleBack() {
        Stage stage = (Stage) backBtn.getScene().getWindow();
        SceneManager.loadScene(stage, "/org/marrok/amriirad/view/dashboard/dashboard-view.fxml");
    }

    @FXML
    private void handleAddFiscalYear() {
        try {
            Stage stage = (Stage) fiscalYearCombo.getScene().getWindow();
            Stage dialog = new Stage();
            dialog.initOwner(stage);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("سنة مالية جديدة");

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/org/marrok/amriirad/view/shared/fiscal-year-dialog.fxml"));
            javafx.scene.Parent root = loader.load();

            TextField yearField = (TextField) root.lookup("#yearField");
            Button saveBtn = (Button) root.lookup("#saveBtn");
            Button cancelBtn = (Button) root.lookup("#cancelBtn");

            yearField.setText(String.valueOf(java.time.Year.now().getValue()));

            saveBtn.setOnAction(e -> {
                String year = yearField.getText().trim();
                if (!year.isEmpty()) {
                    try {
                        fyRepo.save(year);
                        loadFiscalYears();
                        DialogHelper.showInfo("نجاح", "تمت إضافة السنة المالية " + year + " بنجاح.");
                        dialog.close();
                    } catch (java.sql.SQLException ex) {
                        logger.error("Failed to add fiscal year", ex);
                        DialogHelper.showError("خطأ", "فشل إضافة السنة المالية: " + ex.getMessage());
                    }
                }
            });

            cancelBtn.setOnAction(e -> dialog.close());

            Scene scene = new Scene(root);
            SceneManager.applyStylesAndTheme(scene);
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (java.io.IOException e) {
            logger.error("Failed to load fiscal year dialog FXML", e);
        }
    }

    @FXML
    private void handleViewUsers() {
        Stage stage = (Stage) viewUsersBtn.getScene().getWindow();
        SceneManager.loadScene(stage, "/org/marrok/amriirad/view/users/user-management-view.fxml");
    }

    @FXML
    private void handleManagePermissions() {
        Stage stage = (Stage) managePermissionsBtn.getScene().getWindow();
        SceneManager.loadScene(stage, "/org/marrok/amriirad/view/users/permission-management-view.fxml");
    }

    @FXML
    private void handleEnterpriseSettings() {
        Stage stage = (Stage) enterpriseSettingsBtn.getScene().getWindow();
        SceneManager.loadScene(stage, "/org/marrok/amriirad/view/settings/enterprise-info-view.fxml");
    }

    @FXML
    private void handleViewAuditLog() {
        Stage stage = (Stage) viewAuditLogBtn.getScene().getWindow();
        SceneManager.loadScene(stage, "/org/marrok/amriirad/view/settings/audit-log-view.fxml");
    }

    @FXML
    private void handleLogout() {
        if (DialogHelper.showConfirmation("تسجيل الخروج", "هل أنت متأكد من رغبتك في تسجيل الخروج؟")) {
            authService.logout();
            Stage stage = (Stage) logoutBtn.getScene().getWindow();
            SceneManager.loadScene(stage, "/org/marrok/amriirad/view/login/login-view.fxml");
        }
    }
}

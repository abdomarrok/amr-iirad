package org.marrok.amriirad.controller.shared;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
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
        
        // Hide back button if on dashboard
        // We can check the current scene if needed, but for now we'll handle it via a public method
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
        SceneManager.loadScene(stage, "/org/marrok/amriirad/view/dashboard-view.fxml");
    }

    @FXML
    private void handleAddFiscalYear() {
        // Implementation logic from Dashboard
        DialogHelper.showInfo("قريباً", "هذه الميزة ستتوفر في التحديث القادم.");
    }

    @FXML
    private void handleViewUsers() {
        Stage stage = (Stage) viewUsersBtn.getScene().getWindow();
        SceneManager.loadScene(stage, "/org/marrok/amriirad/view/user-management-view.fxml");
    }

    @FXML
    private void handleManagePermissions() {
        Stage stage = (Stage) managePermissionsBtn.getScene().getWindow();
        SceneManager.loadScene(stage, "/org/marrok/amriirad/view/permission-management-view.fxml");
    }

    @FXML
    private void handleEnterpriseSettings() {
        Stage stage = (Stage) enterpriseSettingsBtn.getScene().getWindow();
        SceneManager.loadScene(stage, "/org/marrok/amriirad/view/enterprise-info-view.fxml");
    }

    @FXML
    private void handleLogout() {
        if (DialogHelper.showConfirmation("تسجيل الخروج", "هل أنت متأكد من رغبتك في تسجيل الخروج؟")) {
            authService.logout();
            Stage stage = (Stage) logoutBtn.getScene().getWindow();
            SceneManager.loadScene(stage, "/org/marrok/amriirad/view/login-view.fxml");
        }
    }
}

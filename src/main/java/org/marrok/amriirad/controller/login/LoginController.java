package org.marrok.amriirad.controller.login;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.core.ConcurrencyManager;
import org.marrok.amriirad.service.AuthService;
import org.marrok.amriirad.util.SceneManager;
import org.marrok.amriirad.util.DialogHelper;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    private static final Logger logger = LogManager.getLogger(LoginController.class);

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginBtn;
    @FXML private Label errorLabel;
    @FXML private StackPane loadingOverlay;

    private final AuthService authService;
    private final ConcurrencyManager concurrencyManager;

    public LoginController(AuthService authService, ConcurrencyManager concurrencyManager) {
        this.authService = authService;
        this.concurrencyManager = concurrencyManager;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        errorLabel.setText("");
        // Focus username field by default
        javafx.application.Platform.runLater(() -> usernameField.requestFocus());
    }

    @FXML
    private void handleLogin() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            errorLabel.setText("يرجى إدخال اسم المستخدم وكلمة المرور");
            return;
        }

        errorLabel.setText("");
        loadingOverlay.setVisible(true);
        loginBtn.setDisable(true);

        concurrencyManager.runAsync(
            () -> authService.login(user, pass),
            success -> {
                loadingOverlay.setVisible(false);
                loginBtn.setDisable(false);
                if (success) {
                    navigateToDashboard();
                } else {
                    errorLabel.setText("اسم المستخدم أو كلمة المرور غير صحيحة");
                }
            },
            err -> {
                loadingOverlay.setVisible(false);
                loginBtn.setDisable(false);
                errorLabel.setText("خطأ أثناء تسجيل الدخول: " + err.getMessage());
                logger.error("Login error", err);
            }
        );
    }

    @FXML
    private void handleSettings() {
        Stage stage = (Stage) loginBtn.getScene().getWindow();
        SceneManager.loadScene(stage, "/org/marrok/amriirad/view/settings/mode-selection-view.fxml");
    }

    private void navigateToDashboard() {
        Stage stage = (Stage) loginBtn.getScene().getWindow();
        SceneManager.loadScene(stage, "/org/marrok/amriirad/view/dashboard/dashboard-view.fxml");
    }
}

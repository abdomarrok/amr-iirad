package org.marrok.amriirad.controller.settings;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.util.*;
import org.marrok.amriirad.util.DatabaseSchemaManager;
import org.marrok.amriirad.util.DatabaseConnection;
import org.marrok.amriirad.util.AppMode;
import org.marrok.amriirad.util.SceneManager;
import org.marrok.amriirad.util.DialogHelper;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ResourceBundle;

/**
 * Controller for the server configuration screen.
 * Allows user to enter MariaDB connection details for SERVER mode.
 */
public class ServerConfigController implements Initializable {

    private static final Logger logger = LogManager.getLogger(ServerConfigController.class);

    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private TextField userField;
    @FXML private TextField dbNameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private javafx.scene.control.Button saveBtn;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Pre-fill with saved values
        hostField.setText(AppSettings.getDbHost());
        portField.setText(String.valueOf(AppSettings.getDbPort()));
        userField.setText(AppSettings.getDbUser());
        dbNameField.setText(AppSettings.getDbName());
        passwordField.setText(AppSettings.getDbPass());
    }

    @FXML
    private void handleTest() {
        statusLabel.setStyle("-fx-text-fill: -fx-theme-text-secondary;");
        statusLabel.setText("جاري اختبار الاتصال...");

        String host = hostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            statusLabel.setStyle("-fx-text-fill: -fx-theme-danger;");
            statusLabel.setText("❌ رقم المنفذ غير صالح");
            return;
        }

        String user = userField.getText().trim();
        String password = passwordField.getText();

        String jdbcUrl = "jdbc:mariadb://" + host + ":" + port + "/?connectTimeout=5000";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
            statusLabel.setStyle("-fx-text-fill: -fx-theme-success;");
            statusLabel.setText("✅ تم الاتصال بنجاح!");
            logger.info("Test connection successful to {}:{}", host, port);
        } catch (Exception e) {
            statusLabel.setStyle("-fx-text-fill: -fx-theme-danger;");
            statusLabel.setText("❌ فشل الاتصال: " + e.getMessage());
            logger.error("Test connection failed", e);
        }
    }

    @FXML
    private void handleSave() {
        String host = hostField.getText().trim();
        String dbName = dbNameField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            statusLabel.setStyle("-fx-text-fill: -fx-theme-danger;");
            statusLabel.setText("❌ رقم المنفذ غير صالح");
            return;
        }

        String user = userField.getText().trim();
        String password = passwordField.getText();

        if (host.isEmpty() || dbName.isEmpty() || user.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: -fx-theme-danger;");
            statusLabel.setText("❌ يرجى ملء جميع الحقول المطلوبة");
            return;
        }

        statusLabel.setStyle("-fx-text-fill: -fx-theme-text-secondary;");
        statusLabel.setText("جاري الاتصال بقاعدة البيانات...");
        saveBtn.setDisable(true);

        // Run on background thread to keep UI alive
        new Thread(() -> {
            try {
                // Save to preferences
                AppSettings.saveServerConfig(host, port, user, password, dbName);

                // Configure and initialize the connection pool
                DatabaseConnection.configure(host, port, user, password, dbName);
                DatabaseConnection.initialize(AppMode.SERVER);
                DatabaseSchemaManager.runMigrations();

                javafx.application.Platform.runLater(() -> {
                    Stage stage = (Stage) saveBtn.getScene().getWindow();
                    SceneManager.loadScene(stage, "/org/marrok/amriirad/view/login/login-view.fxml");
                });
            } catch (Exception e) {
                logger.error("Database connection failed", e);
                javafx.application.Platform.runLater(() -> {
                    saveBtn.setDisable(false);
                    statusLabel.setStyle("-fx-text-fill: -fx-theme-danger;");
                    statusLabel.setText("❌ فشل الاتصال: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleBack() {
        SceneManager.loadScene(
            (Stage) saveBtn.getScene().getWindow(),
            "/org/marrok/amriirad/view/settings/mode-selection-view.fxml"
        );
    }
}

package org.marrok.amriirad.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.util.AppSettings;
import org.marrok.amriirad.util.DatabaseConnection;

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
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Pre-fill with saved values
        hostField.setText(AppSettings.getDbHost());
        portField.setText(String.valueOf(AppSettings.getDbPort()));
        userField.setText(AppSettings.getDbUser());
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

        // Save to preferences
        AppSettings.saveServerConfig(host, port, user);

        // Configure the connection pool
        DatabaseConnection.configure(host, port, user, password);

        // Navigate to dashboard
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/marrok/amriirad/view/dashboard-view.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/org/marrok/amriirad/css/app.css").toExternalForm());
            Stage stage = (Stage) hostField.getScene().getWindow();
            stage.setScene(scene);
            stage.setMaximized(true);
        } catch (Exception ex) {
            logger.error("Failed to load dashboard", ex);
            statusLabel.setStyle("-fx-text-fill: -fx-theme-danger;");
            statusLabel.setText("❌ خطأ في تحميل الشاشة الرئيسية");
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/marrok/amriirad/view/mode-selection-view.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/org/marrok/amriirad/css/app.css").toExternalForm());
            Stage stage = (Stage) hostField.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception ex) {
            logger.error("Failed to load mode selection", ex);
        }
    }
}

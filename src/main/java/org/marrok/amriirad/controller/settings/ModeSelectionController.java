package org.marrok.amriirad.controller.settings;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kordamp.ikonli.javafx.FontIcon;
import org.marrok.amriirad.util.AppMode;
import org.marrok.amriirad.util.AppSettings;
import org.marrok.amriirad.util.SceneManager;
import org.marrok.amriirad.util.DialogHelper;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the mode-selection screen.
 * Adapted from GstockDz ModeSelectionController.
 * Lets the user choose LOCAL (MariaDB4j) or SERVER (XAMPP/network) mode.
 */
public class ModeSelectionController implements Initializable {

    private static final Logger logger = LogManager.getLogger(ModeSelectionController.class);

    @FXML public Button localBtn;
    @FXML public Button serverBtn;
    @FXML public Button continueBtn;
    @FXML public Label localBadge;
    @FXML public FontIcon localIcon;
    @FXML public FontIcon serverIcon;
    @FXML public VBox cardsContainer;

    private AppMode selectedMode;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        AppMode savedMode = AppSettings.getSavedAppMode();

        if (savedMode != null) {
            selectedMode = savedMode;
            if (savedMode == AppMode.LOCAL) {
                setLocalAsActive();
                setServerAsSecondary();
            } else {
                setServerAsActive();
                setLocalAsSecondary();
            }
        } else {
            // First run: recommend local
            selectedMode = AppMode.LOCAL;
            setLocalRecommended();
        }

        setupCardActions();
        setupContinueButton();
    }

    // ===================== STATE METHODS =====================

    private void setLocalAsActive() {
        localBtn.getStyleClass().removeAll("mode-card-active", "mode-card-secondary");
        localBtn.getStyleClass().add("mode-card-active");
        showBadge(localBadge, "✓ الوضع الحالي", "mode-badge-current");
    }

    private void setLocalAsSecondary() {
        localBtn.getStyleClass().removeAll("mode-card-active", "mode-card-secondary");
        localBtn.getStyleClass().add("mode-card-secondary");
        hideBadge(localBadge);
    }

    private void setLocalRecommended() {
        localBtn.getStyleClass().removeAll("mode-card-active", "mode-card-secondary");
        localBtn.getStyleClass().add("mode-card-active");
        showBadge(localBadge, "⭐ مُوصى به", "mode-badge-recommended");
    }

    private void setServerAsActive() {
        serverBtn.getStyleClass().removeAll("mode-card-active", "mode-card-secondary");
        serverBtn.getStyleClass().add("mode-card-active");
    }

    private void setServerAsSecondary() {
        serverBtn.getStyleClass().removeAll("mode-card-active", "mode-card-secondary");
        serverBtn.getStyleClass().add("mode-card-secondary");
    }

    // ===================== HELPERS =====================

    private void showBadge(Label badge, String text, String styleClass) {
        badge.setText(text);
        badge.getStyleClass().removeAll("mode-badge-current", "mode-badge-recommended");
        badge.getStyleClass().add(styleClass);
        badge.setVisible(true);
        badge.setManaged(true);
    }

    private void hideBadge(Label badge) {
        badge.setVisible(false);
        badge.setManaged(false);
    }

    private void setupCardActions() {
        localBtn.setOnAction(e -> {
            selectedMode = AppMode.LOCAL;
            setLocalAsActive();
            setServerAsSecondary();
        });

        serverBtn.setOnAction(e -> {
            selectedMode = AppMode.SERVER;
            setServerAsActive();
            setLocalAsSecondary();
        });
    }

    private void setupContinueButton() {
        continueBtn.setOnAction(e -> {
            AppSettings.setAppMode(selectedMode);
            logger.info("Mode selected: {}", selectedMode);

            if (selectedMode == AppMode.SERVER) {
                navigateToServerConfig();
            } else {
                // Disable UI while initializing to prevent double-clicks
                continueBtn.setDisable(true);
                localBtn.setDisable(true);
                serverBtn.setDisable(true);
                continueBtn.setText("جاري التهيئة...");

                // Initialize DB on background thread to avoid freezing the UI
                Thread initThread = new Thread(() -> {
                    try {
                        org.marrok.amriirad.util.DatabaseConnection.initialize(selectedMode);
                        org.marrok.amriirad.util.DatabaseSchemaManager.runMigrations();
                        javafx.application.Platform.runLater(this::navigateToLogin);
                    } catch (Exception ex) {
                        logger.error("Failed to initialize local database", ex);
                        javafx.application.Platform.runLater(() -> {
                            continueBtn.setDisable(false);
                            localBtn.setDisable(false);
                            serverBtn.setDisable(false);
                            continueBtn.setText("متابعة");
                            DialogHelper.showError("خطأ", "فشل بدء قاعدة البيانات المحلية.");
                        });
                    }
                });
                initThread.setDaemon(true);
                initThread.setName("LocalDB-Init");
                initThread.start();
            }
        });
    }

    private void navigateToServerConfig() {
        SceneManager.loadScene(
            (Stage) continueBtn.getScene().getWindow(),
            "/org/marrok/amriirad/view/settings/server-config-view.fxml"
        );
    }

    private void navigateToLogin() {
        Stage stage = (Stage) continueBtn.getScene().getWindow();
        SceneManager.loadScene(stage, "/org/marrok/amriirad/view/login/login-view.fxml");
    }
}

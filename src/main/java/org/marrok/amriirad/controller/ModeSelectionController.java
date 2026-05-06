package org.marrok.amriirad.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kordamp.ikonli.javafx.FontIcon;
import org.marrok.amriirad.util.AppMode;
import org.marrok.amriirad.util.AppSettings;

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
                try {
                    // Initialize DB for local mode immediately
                    org.marrok.amriirad.util.DatabaseConnection.initialize(selectedMode);
                    org.marrok.amriirad.util.DatabaseSchemaManager.runMigrations();
                    navigateToDashboard();
                } catch (Exception ex) {
                    logger.error("Failed to initialize local database", ex);
                    // Show error or alert?
                }
            }
        });
    }

    private void navigateToServerConfig() {
        org.marrok.amriirad.util.GeneralUtil.loadScene(
            (Stage) continueBtn.getScene().getWindow(),
            "/org/marrok/amriirad/view/server-config-view.fxml"
        );
    }

    private void navigateToDashboard() {
        Stage stage = (Stage) continueBtn.getScene().getWindow();
        org.marrok.amriirad.util.GeneralUtil.loadScene(stage, "/org/marrok/amriirad/view/dashboard-view.fxml");
        stage.setMaximized(true);
    }
}

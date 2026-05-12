package org.marrok.amriirad.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.core.AppContext;

import java.io.IOException;

/**
 * Handles FXML loading, scene switching, and modal management.
 */
public class SceneManager {
    private static final Logger logger = LogManager.getLogger(SceneManager.class);
    private static String lastLoadedFxml;

    public static FXMLLoader loadScene(Stage stage, String fxmlPath) {
        return loadScene(stage, fxmlPath, true); // default to resizable for main views
    }

    public static FXMLLoader loadScene(Stage stage, String fxmlPath, boolean isResizable) {
        try {
            lastLoadedFxml = fxmlPath;
            logger.info("Loading scene: {}", fxmlPath);
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            loader.setControllerFactory(param -> AppContext.getInstance().createInstance(param));
            
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            applyStylesAndTheme(scene, stage);
            stage.setResizable(isResizable);

            // Handle sizing and centering pattern from GstockDz
            if (fxmlPath.contains("login-view") || fxmlPath.contains("mode-selection-view") || fxmlPath.contains("server-config")) {
                stage.sizeToScene();
                stage.centerOnScreen();
            } else {
                // For main views, maximize or set to full visual bounds
                javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
                javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();
                stage.setX(bounds.getMinX());
                stage.setY(bounds.getMinY());
                stage.setWidth(bounds.getWidth());
                stage.setHeight(bounds.getHeight());
            }

            stage.show();
            return loader;
        } catch (IOException e) {
            logger.error("Failed to load scene: " + fxmlPath, e);
            DialogHelper.showError("خطأ", "تعذر تحميل الواجهة المطلوبة.");
            return null;
        }
    }

    public static FXMLLoader openModal(Stage owner, String fxmlPath, String title) {
        try {
            Stage dialog = new Stage();
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(owner);
            dialog.setTitle(title);
            dialog.setResizable(false);

            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            loader.setControllerFactory(param -> AppContext.getInstance().createInstance(param));

            Parent root = loader.load();
            Scene scene = new Scene(root);
            dialog.setScene(scene);
            applyStylesAndTheme(scene, dialog);
            dialog.sizeToScene();
            dialog.centerOnScreen();

            // Premium fade-in animation
            root.setOpacity(0);
            javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(350), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
            fadeIn.play();

            dialog.show();
            return loader;
        } catch (IOException e) {
            logger.error("Failed to open modal: " + fxmlPath, e);
            DialogHelper.showError("خطأ", "تعذر فتح النافذة المطلوبة.");
            return null;
        }
    }

    /**
     * Convenience method to open a modal and set a success callback on a BaseFormController.
     */
    public static void showModal(Stage owner, String fxmlPath, String title, Runnable onSuccess) {
        FXMLLoader loader = openModal(owner, fxmlPath, title);
        if (loader != null) {
            Object controller = loader.getController();
            if (controller instanceof org.marrok.amriirad.controller.BaseFormController baseForm) {
                baseForm.setOnSuccess(onSuccess);
            }
        }
    }

    public static void refresh(Stage stage) {
        if (lastLoadedFxml != null) {
            loadScene(stage, lastLoadedFxml);
        }
    }

    public static void applyStylesAndTheme(Scene scene) {
        if (scene == null) return;
        Stage stage = null;
        if (scene.getWindow() instanceof Stage s) {
            stage = s;
        }
        applyStylesAndTheme(scene, stage);
    }

    public static void applyStylesAndTheme(Scene scene, Stage stage) {
        if (scene == null) return;
        
        // Apply global icon to the stage
        if (stage != null) {
            setAppIcon(stage);
        }

        scene.getStylesheets().clear();
        String css = SceneManager.class.getResource("/org/marrok/amriirad/css/app.css").toExternalForm();
        scene.getStylesheets().add(css);
        
        // Force RTL globally
        scene.getRoot().setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);
        
        // Ensure standard cursor
        scene.setCursor(javafx.scene.Cursor.DEFAULT);
    }

    public static void setAppIcon(Stage stage) {
        try {
            String iconPath = "/org/marrok/amriirad/img/logo.ico";
            var iconStream = SceneManager.class.getResourceAsStream(iconPath);
            if (iconStream != null) {
                stage.getIcons().setAll(new javafx.scene.image.Image(iconStream));
            }
        } catch (Exception e) {
            logger.warn("Failed to load application icon: {}", e.getMessage());
        }
    }
}

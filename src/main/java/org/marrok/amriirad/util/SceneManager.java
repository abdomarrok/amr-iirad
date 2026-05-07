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
            applyStylesAndTheme(scene);
            
            stage.setScene(scene);
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
            applyStylesAndTheme(scene);

            dialog.setScene(scene);
            dialog.sizeToScene();
            dialog.centerOnScreen();
            dialog.show();
            return loader;
        } catch (IOException e) {
            logger.error("Failed to open modal: " + fxmlPath, e);
            DialogHelper.showError("خطأ", "تعذر فتح النافذة المطلوبة.");
            return null;
        }
    }

    public static void refresh(Stage stage) {
        if (lastLoadedFxml != null) {
            loadScene(stage, lastLoadedFxml);
        }
    }

    public static void applyStylesAndTheme(Scene scene) {
        if (scene == null) return;
        scene.getStylesheets().clear();
        String css = SceneManager.class.getResource("/org/marrok/amriirad/css/app.css").toExternalForm();
        scene.getStylesheets().add(css);
        
        // Force RTL globally
        scene.getRoot().setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);
        
        // Ensure standard cursor
        scene.setCursor(javafx.scene.Cursor.DEFAULT);
    }
}

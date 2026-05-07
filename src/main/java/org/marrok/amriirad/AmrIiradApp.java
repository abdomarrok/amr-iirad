package org.marrok.amriirad;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.core.AppContext;
import org.marrok.amriirad.util.*;

public class AmrIiradApp extends Application {

    private static final Logger logger = LogManager.getLogger(AmrIiradApp.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Starting Amr-Iirad application...");

        primaryStage.setTitle("نظام أوامر الإيراد");

        if (!AppSettings.isModeConfigured()) {
            SceneManager.loadScene(primaryStage, "/org/marrok/amriirad/view/mode-selection-view.fxml");
        } else {
            try {
                AppMode mode = AppSettings.getAppMode();
                if (mode == AppMode.SERVER) {
                    DatabaseConnection.configure(
                            AppSettings.getDbHost(),
                            AppSettings.getDbPort(),
                            AppSettings.getDbUser(),
                            ""
                    );
                }
                DatabaseConnection.initialize(mode);
                DatabaseSchemaManager.runMigrations();
                
                // Maximize for dashboard
                primaryStage.setMaximized(true);
                SceneManager.loadScene(primaryStage, "/org/marrok/amriirad/view/dashboard-view.fxml");
            } catch (Exception e) {
                logger.error("Database initialization failed, showing mode selection", e);
                AppSettings.clearAll();
                SceneManager.loadScene(primaryStage, "/org/marrok/amriirad/view/mode-selection-view.fxml");
            }
        }
    }

    @Override
    public void stop() throws Exception {
        logger.info("Shutting down Amr-Iirad application...");
        AppContext.getInstance().dispose();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

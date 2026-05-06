package org.marrok.amriirad;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.util.AppMode;
import org.marrok.amriirad.util.AppSettings;
import org.marrok.amriirad.util.DatabaseConnection;
import org.marrok.amriirad.util.DatabaseSchemaManager;

public class AmrIiradApp extends Application {

    private static final Logger logger = LogManager.getLogger(AmrIiradApp.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Starting Amr-Iirad application...");

        primaryStage.setTitle("نظام أوامر الإيراد");

        if (!AppSettings.isModeConfigured()) {
            // First run → show mode selection (no DB init needed yet)
            showView(primaryStage, "/org/marrok/amriirad/view/mode-selection-view.fxml", false);
        } else {
            // Mode already configured → initialize DB then show dashboard
            try {
                AppMode mode = AppSettings.getAppMode();
                if (mode == AppMode.SERVER) {
                    // Apply saved server config
                    DatabaseConnection.configure(
                            AppSettings.getDbHost(),
                            AppSettings.getDbPort(),
                            AppSettings.getDbUser(),
                            ""
                    );
                }
                DatabaseConnection.initialize(mode);
                DatabaseSchemaManager.runMigrations();
                showView(primaryStage, "/org/marrok/amriirad/view/dashboard-view.fxml", true);
            } catch (Exception e) {
                logger.error("Database initialization failed, showing mode selection", e);
                // Fall back to mode selection if DB fails
                AppSettings.clearAll();
                showView(primaryStage, "/org/marrok/amriirad/view/mode-selection-view.fxml", false);
            }
        }
    }

    private void showView(Stage stage, String fxmlPath, boolean maximize) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(
                getClass().getResource("/org/marrok/amriirad/css/app.css").toExternalForm());
        stage.setScene(scene);
        if (maximize) stage.setMaximized(true);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        logger.info("Shutting down Amr-Iirad application...");
        DatabaseConnection.shutdown();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

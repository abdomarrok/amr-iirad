package org.marrok.amriirad;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.util.AppSettings;
import org.marrok.amriirad.util.DatabaseConnection;
import org.marrok.amriirad.util.DatabaseSchemaManager;

public class AmrIiradApp extends Application {

    private static final Logger logger = LogManager.getLogger(AmrIiradApp.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Starting Amr-Iirad application...");

        // Step 1: Initialize database connection based on saved mode
        DatabaseConnection.initialize(AppSettings.getAppMode());

        // Step 2: Run schema migrations (idempotent)
        DatabaseSchemaManager.runMigrations();

        // Step 3: Load the mode-selection or login screen
        // (If mode was never set, go to mode-selection first)
        String fxml = AppSettings.isModeConfigured()
                ? "/org/marrok/amriirad/view/login-view.fxml"
                : "/org/marrok/amriirad/view/mode-selection-view.fxml";

        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(getClass().getResource("/org/marrok/amriirad/css/app.css").toExternalForm());

        primaryStage.setTitle("نظام أوامر الإيراد");
        primaryStage.setScene(scene);
        primaryStage.show();
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

package org.marrok.amriirad;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.core.AppContext;
import org.marrok.amriirad.util.*;
import javafx.scene.text.Font;

public class AmrIiradApp extends Application {

    private static final Logger logger = LogManager.getLogger(AmrIiradApp.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Starting Amr-Iirad application...");

        // Register a shutdown hook to ensure DB cleanup on hard exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown hook triggered. Cleaning up...");
            DatabaseConnection.shutdown();
        }));

        primaryStage.setTitle("نظام أوامر الإيراد");
        SceneManager.setAppIcon(primaryStage);

        // Load fonts
        try {
            Font.loadFont(getClass().getResourceAsStream("/org/marrok/amriirad/fonts/Cairo-Variable.ttf"), 14);
        } catch (Exception e) {
            logger.warn("Failed to load Cairo font, using fallback system font");
        }

        // Always start with mode selection to allow changing connection settings if needed
        SceneManager.loadScene(primaryStage, "/org/marrok/amriirad/view/settings/mode-selection-view.fxml");
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

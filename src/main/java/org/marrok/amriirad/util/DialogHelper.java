package org.marrok.amriirad.util;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Optional;

/**
 * Utility for standard JavaFX dialogs and alerts with RTL support.
 */
public class DialogHelper {

    public static void showInfo(String title, String content) {
        showAlert(Alert.AlertType.INFORMATION, title, content, true);
    }

    public static void showError(String title, String content) {
        showAlert(Alert.AlertType.ERROR, title, content, false);
    }

    public static void showWarning(String title, String content) {
        showAlert(Alert.AlertType.WARNING, title, content, false);
    }

    public static boolean showConfirmation(String title, String content) {
        Alert alert = createAlert(Alert.AlertType.CONFIRMATION, title, content);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static void showAlert(Alert.AlertType type, String title, String content, boolean autoClose) {
        Alert alert = createAlert(type, title, content);
        
        if (autoClose) {
            Timeline timeline = new Timeline(new KeyFrame(Duration.millis(1500), event -> alert.close()));
            timeline.play();
        }
        
        alert.showAndWait();
    }

    private static Alert createAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.getDialogPane().setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);
        return alert;
    }

    public static void showErrorAndExit(String message) {
        Platform.runLater(() -> {
            showError("خطأ في التشغيل", message);
            Platform.exit();
        });
    }

    public static Stage showLoading(String message) {
        try {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setResizable(false);
            dialog.setTitle("الرجاء الانتظار / Veuillez patienter");
            dialog.initStyle(javafx.stage.StageStyle.UNDECORATED); // Modern look

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(DialogHelper.class.getResource("/org/marrok/amriirad/view/shared/loading-view.fxml"));
            javafx.scene.Parent root = loader.load();
            
            // Set message if label exists
            javafx.scene.control.Label label = (javafx.scene.control.Label) root.lookup("#messageLabel");
            if (label != null) label.setText(message);

            Scene scene = new Scene(root);
            dialog.setScene(scene);
            dialog.centerOnScreen();
            dialog.show();
            return dialog;
        } catch (java.io.IOException e) {
            // Fallback to simple construction if FXML fails
            Stage dialog = new Stage();
            dialog.setTitle("Loading...");
            dialog.setScene(new Scene(new javafx.scene.control.Label(message)));
            dialog.show();
            return dialog;
        }
    }

    public static void showLanguageDialog(java.util.function.Consumer<org.marrok.amriirad.model.PrintLanguage> onSelect) {
        try {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initStyle(javafx.stage.StageStyle.UTILITY);
            dialog.setTitle("لغة الطباعة / Langue d'impression");

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(DialogHelper.class.getResource("/org/marrok/amriirad/view/shared/language-selection-view.fxml"));
            javafx.scene.Parent root = loader.load();

            javafx.scene.control.Button arabicBtn = (javafx.scene.control.Button) root.lookup("#arabicBtn");
            javafx.scene.control.Button frenchBtn = (javafx.scene.control.Button) root.lookup("#frenchBtn");
            javafx.scene.control.Button cancelBtn = (javafx.scene.control.Button) root.lookup("#cancelBtn");

            if (arabicBtn != null) arabicBtn.setOnAction(e -> {
                onSelect.accept(org.marrok.amriirad.model.PrintLanguage.ARABIC);
                dialog.close();
            });
            if (frenchBtn != null) frenchBtn.setOnAction(e -> {
                onSelect.accept(org.marrok.amriirad.model.PrintLanguage.FRENCH);
                dialog.close();
            });
            if (cancelBtn != null) cancelBtn.setOnAction(e -> dialog.close());

            Scene scene = new Scene(root);
            SceneManager.applyStylesAndTheme(scene);
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (java.io.IOException e) {
            org.apache.logging.log4j.LogManager.getLogger(DialogHelper.class).error("Failed to load language selection FXML", e);
            // Fallback to simple alert if FXML fails
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.getButtonTypes().setAll(new ButtonType("AR"), new ButtonType("FR"));
            alert.showAndWait().ifPresent(type -> {
                if (type.getText().contains("AR")) onSelect.accept(org.marrok.amriirad.model.PrintLanguage.ARABIC);
                else onSelect.accept(org.marrok.amriirad.model.PrintLanguage.FRENCH);
            });
        }
    }
}

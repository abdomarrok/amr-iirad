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
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setResizable(false);
        dialog.setTitle("الرجاء الانتظار");

        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-padding: 20; -fx-background-color: white; -fx-border-color: #ccc; -fx-border-width: 1px;");

        javafx.scene.control.ProgressBar bar = new javafx.scene.control.ProgressBar();
        bar.setPrefWidth(200);
        bar.setProgress(-1);

        javafx.scene.control.Label label = new javafx.scene.control.Label(message);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        label.setWrapText(true);
        label.setMaxWidth(220);
        label.setAlignment(Pos.CENTER);

        box.getChildren().addAll(bar, label);

        Scene scene = new Scene(box);
        dialog.setScene(scene);
        dialog.show();
        return dialog;
    }
}

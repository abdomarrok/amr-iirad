package org.marrok.amriirad.util;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.core.AppContext;

import java.io.IOException;
import java.util.Optional;

public class GeneralUtil {
    private static final Logger logger = LogManager.getLogger(GeneralUtil.class);

    public static Optional<ButtonType> showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.getDialogPane().setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);

        if (alertType == Alert.AlertType.INFORMATION) {
            Timeline timeline = new Timeline(new KeyFrame(
                    Duration.millis(1500), // Increased to 1.5s for readability
                    event -> alert.close()));
            timeline.setCycleCount(1);
            timeline.play();
        }

        return alert.showAndWait();
    }

    public static void showAlertWithOutTimelimit(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.getDialogPane().setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);
        alert.showAndWait();
    }

    public static FXMLLoader loadScene(Stage stage, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(GeneralUtil.class.getResource(fxmlPath));
            loader.setControllerFactory(param -> AppContext.getInstance().createInstance(param));
            
            Parent root = loader.load();
            Scene scene = new Scene(root);
            applyStylesAndTheme(scene);
            
            stage.setScene(scene);
            stage.show();
            return loader;
        } catch (IOException e) {
            logger.error("Failed to load scene: " + fxmlPath, e);
            showAlert(Alert.AlertType.ERROR, "خطأ", "تعذر تحميل الواجهة المطلوبة.");
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

            FXMLLoader loader = new FXMLLoader(GeneralUtil.class.getResource(fxmlPath));
            loader.setControllerFactory(param -> AppContext.getInstance().createInstance(param));

            Parent root = loader.load();
            Scene scene = new Scene(root);
            applyStylesAndTheme(scene);

            dialog.setScene(scene);
            dialog.sizeToScene(); // Ensure window fits content
            dialog.show();
            return loader;
        } catch (IOException e) {
            logger.error("Failed to open modal: " + fxmlPath, e);
            showAlert(Alert.AlertType.ERROR, "خطأ", "تعذر فتح النافذة المطلوبة.");
            return null;
        }
    }

    public static boolean showConfirmationDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.getDialogPane().setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public static Scene createScene(Parent root) {
        Scene scene = new Scene(root);
        applyStylesAndTheme(scene);
        return scene;
    }

    public static Stage showLoading(String message) {
        return showLoadingWithProgress(message, null, null);
    }

    public static Stage showLoadingWithProgress(String message, javafx.scene.control.ProgressBar[] progressBar,
            javafx.scene.control.Label[] messageLabel) {
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

        if (progressBar != null && progressBar.length > 0) {
            progressBar[0] = bar;
        }
        if (messageLabel != null && messageLabel.length > 0) {
            messageLabel[0] = label;
        }

        Scene scene = new Scene(box);
        dialog.setScene(scene);
        dialog.show();
        return dialog;
    }

    public static void applyStylesAndTheme(Scene scene) {
        if (scene == null) return;
        scene.getStylesheets().clear();
        scene.getStylesheets().add(
                GeneralUtil.class.getResource("/org/marrok/amriirad/css/app.css").toExternalForm());
    }

    public static void showErrorAndExit(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
            alert.setHeaderText("خطأ في التشغيل");
            alert.getDialogPane().setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);
            alert.showAndWait();
            Platform.exit();
        });
    }
}

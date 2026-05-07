package org.marrok.amriirad.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.core.ConcurrencyManager;

/**
 * Base class for modal form controllers to unify window management,
 * error handling, and common dependencies.
 */
public abstract class BaseFormController {

    protected final ConcurrencyManager concurrencyManager;
    protected Runnable onSuccess;

    @FXML protected Label titleLabel;
    @FXML protected Label errorLabel;
    @FXML protected Button saveBtn;
    @FXML protected Button cancelBtn;

    protected BaseFormController(ConcurrencyManager concurrencyManager) {
        this.concurrencyManager = concurrencyManager;
    }

    protected abstract Logger getLogger();
    protected abstract boolean validateForm();

    @FXML
    protected void handleCancel() {
        closeWindow();
    }

    protected void closeWindow() {
        // Use either saveBtn or cancelBtn to get the stage
        Button refBtn = saveBtn != null ? saveBtn : cancelBtn;
        if (refBtn != null && refBtn.getScene() != null) {
            Stage stage = (Stage) refBtn.getScene().getWindow();
            stage.close();
        }
    }

    protected void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText("❌ " + message);
        }
        if (message != null && !message.isEmpty()) {
            getLogger().error(message);
        }
    }

    protected void clearError() {
        if (errorLabel != null) {
            errorLabel.setText("");
        }
    }

    protected void runOnSuccess() {
        if (onSuccess != null) {
            onSuccess.run();
        }
    }
}

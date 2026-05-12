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

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    @FXML protected Label titleLabel;
    @FXML protected Label errorLabel;
    @FXML protected Button saveBtn;
    @FXML protected Button cancelBtn;

    protected boolean isDirty = false;
    protected boolean isLoading = false;

    protected BaseFormController(ConcurrencyManager concurrencyManager) {
        this.concurrencyManager = concurrencyManager;
    }

    protected abstract Logger getLogger();
    protected abstract boolean validateForm();

    protected void markDirty() {
        this.isDirty = true;
    }

    protected void clearDirty() {
        this.isDirty = false;
    }

    protected void setLoading(boolean loading) {
        this.isLoading = loading;
        if (saveBtn != null) saveBtn.setDisable(loading);
        if (cancelBtn != null) cancelBtn.setDisable(loading);
        // Optional: show a spinner or change cursor
    }

    @FXML
    protected void handleCancel() {
        if (confirmClose()) {
            closeWindow();
        }
    }

    protected boolean confirmClose() {
        if (isDirty) {
            return org.marrok.amriirad.util.DialogHelper.showConfirmation(
                "تغييرات غير محفوظة", 
                "لديك تغييرات غير محفوظة. هل أنت متأكد من الإغلاق؟"
            );
        }
        return true;
    }

    protected void closeWindow() {
        // Try to get stage from any injected node
        javafx.scene.Node refNode = null;
        if (saveBtn != null && saveBtn.getScene() != null) refNode = saveBtn;
        else if (cancelBtn != null && cancelBtn.getScene() != null) refNode = cancelBtn;
        else if (titleLabel != null && titleLabel.getScene() != null) refNode = titleLabel;
        else if (errorLabel != null && errorLabel.getScene() != null) refNode = errorLabel;

        if (refNode != null) {
            Stage stage = (Stage) refNode.getScene().getWindow();
            stage.close();
        } else {
            getLogger().warn("Could not find a reference node to close the window.");
        }
    }

    protected void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText("❌ " + message);
            errorLabel.getStyleClass().add("form-error-text");
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

    protected void setInvalid(javafx.scene.Node node, boolean invalid) {
        if (invalid) {
            node.getStyleClass().add("form-control-invalid");
        } else {
            node.getStyleClass().removeAll("form-control-invalid");
        }
    }

    protected void runOnSuccess() {
        if (onSuccess != null) {
            onSuccess.run();
        }
    }

    /**
     * Standardizes keyboard shortcuts for forms (Enter to Save, Escape to Cancel).
     */
    protected void setupCommonShortcuts(javafx.scene.Node root, Runnable onSaveAction) {
        if (root == null) return;
        root.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                handleCancel();
                event.consume();
            } else if (event.getCode() == javafx.scene.input.KeyCode.ENTER && onSaveAction != null) {
                // Don't trigger if we're in a TextArea or if another button is focused (let it handle its own click)
                if (!(event.getTarget() instanceof javafx.scene.control.TextArea) && 
                    !(event.getTarget() instanceof javafx.scene.control.Button)) {
                     onSaveAction.run();
                     event.consume();
                }
            }
        });
    }
}

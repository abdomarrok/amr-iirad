package org.marrok.amriirad.controller.shared.components;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Reusable controller for a standardized empty state view.
 */
public class EmptyStateController {

    @FXML private javafx.scene.layout.VBox emptyStateRoot;
    @FXML private FontIcon icon;
    @FXML private Label titleLabel;
    @FXML private Label messageLabel;
    @FXML private Button actionBtn;

    private Runnable onActionCallback;

    public void init(String title, String message, String iconLiteral, Runnable action) {
        titleLabel.setText(title);
        messageLabel.setText(message);
        if (iconLiteral != null) icon.setIconLiteral(iconLiteral);
        
        this.onActionCallback = action;
        if (action != null) {
            actionBtn.setVisible(true);
            actionBtn.setManaged(true);
        }
    }

    public void show(boolean visible) {
        if (emptyStateRoot != null) {
            emptyStateRoot.setVisible(visible);
            emptyStateRoot.setManaged(visible);
        }
    }

    @FXML
    private void onAction() {
        if (onActionCallback != null) onActionCallback.run();
    }
}

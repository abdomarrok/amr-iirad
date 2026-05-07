package org.marrok.amriirad.controller.shared;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import org.marrok.amriirad.core.AppContext;
import org.marrok.amriirad.service.AuthService;

import java.net.URL;
import java.util.ResourceBundle;

public class FooterController implements Initializable {

    @FXML private Label statusBar;
    @FXML private Label currentUserLabel;

    private final AuthService authService;

    public FooterController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (authService.getCurrentUser() != null) {
            currentUserLabel.setText("المستخدم: " + authService.getCurrentUser().getFullName());
        }
    }

    public void setStatus(String status) {
        statusBar.setText(status);
    }
}

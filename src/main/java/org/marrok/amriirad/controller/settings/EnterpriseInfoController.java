package org.marrok.amriirad.controller.settings;

import org.marrok.amriirad.controller.shared.TopBarController;
import org.marrok.amriirad.controller.shared.FooterController;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.marrok.amriirad.model.InstitutionInfo;
import org.marrok.amriirad.service.InstitutionService;
import org.marrok.amriirad.util.DialogHelper;
import org.marrok.amriirad.util.SceneManager;

import java.net.URL;
import java.util.ResourceBundle;

public class EnterpriseInfoController implements Initializable {

    @FXML private org.marrok.amriirad.controller.shared.TopBarController topBarController;
    @FXML private TextField ministryArField;
    @FXML private TextField ministryFrField;
    @FXML private TextField nameArField;
    @FXML private TextField nameFrField;
    @FXML private TextField officerArField;
    @FXML private TextField officerFrField;
    @FXML private TextField ordonnateurCodeField;
    @FXML private TextField wilayaArField;
    @FXML private TextField wilayaFrField;
    @FXML private TextField treasuryArField;
    @FXML private TextField treasuryFrField;
    @FXML private TextField treasuryAccountField;
    @FXML private TextField ribField;
    @FXML private TextField addressField;
    @FXML private TextField addressFrField;

    private final InstitutionService institutionService;
    private InstitutionInfo currentInfo;

    public EnterpriseInfoController(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (topBarController != null) {
            topBarController.setBackVisible(true);
        }
        loadData();
    }

    private void loadData() {
        currentInfo = institutionService.getInfo();
        if (currentInfo != null) {
            ministryArField.setText(currentInfo.getMinistryNameAr());
            ministryFrField.setText(currentInfo.getMinistryNameFr());
            nameArField.setText(currentInfo.getNameAr());
            nameFrField.setText(currentInfo.getNameFr());
            officerArField.setText(currentInfo.getAuthorizingOfficerAr());
            officerFrField.setText(currentInfo.getAuthorizingOfficerFr());
            ordonnateurCodeField.setText(currentInfo.getOrdonnateurCode());
            wilayaArField.setText(currentInfo.getWilayaAr());
            wilayaFrField.setText(currentInfo.getWilayaFr());
            treasuryArField.setText(currentInfo.getTreasuryNameAr());
            treasuryFrField.setText(currentInfo.getTreasuryNameFr());
            treasuryAccountField.setText(currentInfo.getTreasuryAccountAr());
            ribField.setText(currentInfo.getRibNumber());
            addressField.setText(currentInfo.getAddressAr());
            addressFrField.setText(currentInfo.getAddressFr());
        }
    }

    @FXML
    private void handleSave() {
        String nameAr = safeTrim(nameArField);
        if (nameAr.isEmpty()) {
            DialogHelper.showError("خطأ", "اسم المؤسسة بالعربية مطلوب.");
            return;
        }

        if (currentInfo == null) currentInfo = new InstitutionInfo();

        currentInfo.setMinistryNameAr(safeTrim(ministryArField));
        currentInfo.setMinistryNameFr(safeTrim(ministryFrField));
        currentInfo.setNameAr(safeTrim(nameArField));
        currentInfo.setNameFr(safeTrim(nameFrField));
        currentInfo.setAuthorizingOfficerAr(safeTrim(officerArField));
        currentInfo.setAuthorizingOfficerFr(safeTrim(officerFrField));
        currentInfo.setOrdonnateurCode(safeTrim(ordonnateurCodeField));
        currentInfo.setWilayaAr(safeTrim(wilayaArField));
        currentInfo.setWilayaFr(safeTrim(wilayaFrField));
        currentInfo.setTreasuryNameAr(safeTrim(treasuryArField));
        currentInfo.setTreasuryNameFr(safeTrim(treasuryFrField));
        currentInfo.setTreasuryAccountAr(safeTrim(treasuryAccountField));
        currentInfo.setRibNumber(safeTrim(ribField));
        currentInfo.setAddressAr(safeTrim(addressField));
        currentInfo.setAddressFr(safeTrim(addressFrField));

        try {
            institutionService.updateInfo(currentInfo);
            DialogHelper.showInfo("نجاح", "تم حفظ معلومات المؤسسة بنجاح.");
            close();
        } catch (Exception e) {
            DialogHelper.showError("خطأ", "فشل حفظ البيانات: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) nameArField.getScene().getWindow();
        org.marrok.amriirad.service.AuthService auth = org.marrok.amriirad.core.AppContext.getInstance().getAuthService();
        
        if (auth.getCurrentUser() != null) {
            SceneManager.loadScene(stage, "/org/marrok/amriirad/view/dashboard/dashboard-view.fxml");
        } else {
            SceneManager.loadScene(stage, "/org/marrok/amriirad/view/login/login-view.fxml");
        }
    }
    private String safeTrim(TextField field) {
        if (field == null || field.getText() == null) return "";
        return field.getText().trim();
    }
}

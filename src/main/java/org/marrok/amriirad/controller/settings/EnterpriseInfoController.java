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

import java.net.URL;
import java.util.ResourceBundle;

public class EnterpriseInfoController implements Initializable {

    @FXML private TextField nameArField;
    @FXML private TextField nameFrField;
    @FXML private TextField officerArField;
    @FXML private TextField wilayaArField;
    @FXML private TextField treasuryAccountField;
    @FXML private TextField ribField;
    @FXML private TextField addressField;

    private final InstitutionService institutionService;
    private InstitutionInfo currentInfo;

    public EnterpriseInfoController(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadData();
    }

    private void loadData() {
        currentInfo = institutionService.getInfo();
        if (currentInfo != null) {
            nameArField.setText(currentInfo.getNameAr());
            nameFrField.setText(currentInfo.getNameFr());
            officerArField.setText(currentInfo.getAuthorizingOfficerAr());
            wilayaArField.setText(currentInfo.getWilayaAr());
            treasuryAccountField.setText(currentInfo.getTreasuryAccountAr());
            ribField.setText(currentInfo.getRibNumber());
            addressField.setText(currentInfo.getAddressAr());
        }
    }

    @FXML
    private void handleSave() {
        if (nameArField.getText().trim().isEmpty()) {
            DialogHelper.showError("خطأ", "اسم المؤسسة بالعربية مطلوب.");
            return;
        }

        if (currentInfo == null) currentInfo = new InstitutionInfo();

        currentInfo.setNameAr(nameArField.getText().trim());
        currentInfo.setNameFr(nameFrField.getText().trim());
        currentInfo.setAuthorizingOfficerAr(officerArField.getText().trim());
        currentInfo.setWilayaAr(wilayaArField.getText().trim());
        currentInfo.setTreasuryAccountAr(treasuryAccountField.getText().trim());
        currentInfo.setRibNumber(ribField.getText().trim());
        currentInfo.setAddressAr(addressField.getText().trim());

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
        ((Stage) nameArField.getScene().getWindow()).close();
    }
}

package org.marrok.amriirad.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.core.ConcurrencyManager;
import org.marrok.amriirad.model.Debtor;
import org.marrok.amriirad.model.DebtorType;
import org.marrok.amriirad.repository.DebtorRepository;

import java.net.URL;
import java.util.ResourceBundle;

public class DebtorFormController implements Initializable {

    private static final Logger logger = LogManager.getLogger(DebtorFormController.class);

    @FXML private Label titleLabel;
    @FXML private ComboBox<DebtorType> typeCombo;
    @FXML private TextField fullNameField;
    @FXML private TextField idNumberField;
    @FXML private TextField addressField;
    @FXML private TextField phoneField;
    @FXML private Label errorLabel;
    @FXML private Button saveBtn;

    private final DebtorRepository debtorRepo;
    private Debtor currentDebtor;
    private Runnable onSuccess;

    public DebtorFormController(DebtorRepository debtorRepo) {
        this.debtorRepo = debtorRepo;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        typeCombo.setItems(FXCollections.observableArrayList(DebtorType.values()));
        typeCombo.setValue(DebtorType.INDIVIDUAL);
    }

    public void initForCreate(Runnable onSuccess) {
        this.currentDebtor = new Debtor();
        this.onSuccess = onSuccess;
        titleLabel.setText("إضافة مدين جديد");
    }

    public void initForEdit(Debtor debtor, Runnable onSuccess) {
        this.currentDebtor = debtor;
        this.onSuccess = onSuccess;
        titleLabel.setText("تعديل بيانات مدين");

        typeCombo.setValue(debtor.getDebtorType());
        fullNameField.setText(debtor.getFullName());
        idNumberField.setText(debtor.getIdNumber());
        addressField.setText(debtor.getAddress());
        phoneField.setText(debtor.getPhone());
    }

    @FXML
    private void handleSave() {
        if (!validateForm()) return;

        currentDebtor.setDebtorType(typeCombo.getValue());
        currentDebtor.setFullName(fullNameField.getText().trim());
        currentDebtor.setIdNumber(idNumberField.getText().trim());
        currentDebtor.setAddress(addressField.getText().trim());
        currentDebtor.setPhone(phoneField.getText().trim());

        ConcurrencyManager.getInstance().runAsync(
            () -> {
                if (currentDebtor.getId() == 0) {
                    debtorRepo.save(currentDebtor);
                } else {
                    debtorRepo.update(currentDebtor);
                }
                return true;
            },
            res -> {
                closeWindow();
                if (onSuccess != null) onSuccess.run();
            },
            err -> {
                logger.error("Failed to save debtor", err);
                errorLabel.setText("❌ " + err.getMessage());
            }
        );
    }

    private boolean validateForm() {
        errorLabel.setText("");
        if (fullNameField.getText() == null || fullNameField.getText().trim().isEmpty()) {
            errorLabel.setText("❌ الاسم الكامل مطلوب.");
            return false;
        }
        return true;
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) saveBtn.getScene().getWindow();
        stage.close();
    }
}

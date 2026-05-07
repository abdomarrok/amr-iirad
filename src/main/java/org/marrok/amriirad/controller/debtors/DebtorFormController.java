package org.marrok.amriirad.controller.debtors;

import org.marrok.amriirad.controller.BaseFormController;
import org.marrok.amriirad.controller.shared.TopBarController;
import org.marrok.amriirad.controller.shared.FooterController;
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

public class DebtorFormController extends BaseFormController implements Initializable {

    private static final Logger logger = LogManager.getLogger(DebtorFormController.class);

    @FXML private Label titleLabel;
    @FXML private ComboBox<DebtorType> typeCombo;
    @FXML private TextField fullNameField;
    @FXML private TextField idNumberField;
    @FXML private TextArea addressField;
    @FXML private TextField phoneField;
    @FXML private TextField bankAccountField;
    @FXML private TextField cnasNumberField;
    @FXML private TextField nifNumberField;
    @FXML private Label errorLabel;
    
    private final DebtorRepository debtorRepo;
    private Debtor currentDebtor;

    public DebtorFormController(DebtorRepository debtorRepo, ConcurrencyManager concurrencyManager) {
        super(concurrencyManager);
        this.debtorRepo = debtorRepo;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        typeCombo.setItems(FXCollections.observableArrayList(DebtorType.values()));

        // Display Arabic labels instead of raw enum names
        typeCombo.setConverter(new javafx.util.StringConverter<DebtorType>() {
            @Override
            public String toString(DebtorType type) {
                return type != null ? type.getArabicLabel() : "";
            }

            @Override
            public DebtorType fromString(String string) {
                // Not needed for non-editable ComboBox
                return null;
            }
        });

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
        if (bankAccountField != null) bankAccountField.setText(debtor.getBankAccount());
        if (cnasNumberField != null) cnasNumberField.setText(debtor.getCnasNumber());
        if (nifNumberField != null) nifNumberField.setText(debtor.getNifNumber());
    }

    @FXML
    private void handleSave() {
        if (!validateForm()) return;
        clearError();

        currentDebtor.setDebtorType(typeCombo.getValue());
        currentDebtor.setFullName(fullNameField.getText() != null ? fullNameField.getText().trim() : "");
        currentDebtor.setIdNumber(idNumberField.getText() != null ? idNumberField.getText().trim() : "");
        currentDebtor.setAddress(addressField.getText() != null ? addressField.getText().trim() : "");
        currentDebtor.setPhone(phoneField.getText() != null ? phoneField.getText().trim() : "");
        currentDebtor.setBankAccount(bankAccountField != null && bankAccountField.getText() != null ? bankAccountField.getText().trim() : "");
        currentDebtor.setCnasNumber(cnasNumberField != null && cnasNumberField.getText() != null ? cnasNumberField.getText().trim() : "");
        currentDebtor.setNifNumber(nifNumberField != null && nifNumberField.getText() != null ? nifNumberField.getText().trim() : "");

        concurrencyManager.runAsync(
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
                runOnSuccess();
            },
            err -> showError(err.getMessage())
        );
    }

    @Override
    protected boolean validateForm() {
        clearError();
        if (fullNameField.getText() == null || fullNameField.getText().trim().isEmpty()) {
            showError("الاسم الكامل مطلوب.");
            return false;
        }
        return true;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

}

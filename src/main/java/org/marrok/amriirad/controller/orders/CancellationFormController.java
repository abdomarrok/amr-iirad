package org.marrok.amriirad.controller.orders;

import org.marrok.amriirad.controller.BaseFormController;
import org.marrok.amriirad.controller.shared.TopBarController;
import org.marrok.amriirad.controller.shared.FooterController;
import org.marrok.amriirad.core.AppContext;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.core.ConcurrencyManager;
import org.marrok.amriirad.model.RevenueOrder;
import org.marrok.amriirad.model.RevenueOrderCancellation;
import org.marrok.amriirad.model.CancellationType;
import org.marrok.amriirad.service.CancellationOrderService;
import org.marrok.amriirad.util.ReportParamBuilder;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class CancellationFormController extends BaseFormController implements Initializable {

    private static final Logger logger = LogManager.getLogger(CancellationFormController.class);

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private ComboBox<CancellationType> typeCombo;
    @FXML private TextField cancellationNumField;
    @FXML private DatePicker cancellationDatePicker;
    @FXML private Label reducedAmountLabel;
    @FXML private TextField reducedAmountField;
    @FXML private TextArea reasonField;
    @FXML private TextArea reasonFrField;
    @FXML private Label errorLabel;
    @FXML private Button saveBtn;

    private RevenueOrder targetOrder;
    private final CancellationOrderService cancellationService;
    private final org.marrok.amriirad.service.ReportService reportService;
    private final org.marrok.amriirad.service.TafqeetService tafqeetService;

    public CancellationFormController(CancellationOrderService cancellationService,
                                     org.marrok.amriirad.service.ReportService reportService,
                                     org.marrok.amriirad.service.TafqeetService tafqeetService,
                                     org.marrok.amriirad.core.ConcurrencyManager concurrencyManager) {
        super(concurrencyManager);
        this.cancellationService = cancellationService;
        this.reportService = reportService;
        this.tafqeetService = tafqeetService;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cancellationDatePicker.setValue(LocalDate.now());

        typeCombo.setItems(FXCollections.observableArrayList(CancellationType.values()));
        typeCombo.setValue(CancellationType.FULL_CANCEL);

        typeCombo.valueProperty().addListener((obs, oldV, newV) -> {
            boolean showAmount = (newV == CancellationType.REDUCTION || newV == CancellationType.INCREASE);
            reducedAmountLabel.setVisible(showAmount);
            reducedAmountLabel.setManaged(showAmount);
            reducedAmountField.setVisible(showAmount);
            reducedAmountField.setManaged(showAmount);
            
            if (newV == CancellationType.INCREASE) {
                reducedAmountLabel.setText("مبلغ الزيادة / Augmenté :");
                titleLabel.setText("زيادة أمر إيراد | Augmentation");
            } else if (newV == CancellationType.REDUCTION) {
                reducedAmountLabel.setText("المبلغ المخفض / Réduit :");
                titleLabel.setText("تخفيض أمر إيراد | Réduction");
            } else {
                titleLabel.setText("إلغاء أمر إيراد | Annulation");
            }
        });
    }

    public void initData(RevenueOrder order, CancellationType defaultType, Runnable onSuccess) {
        this.targetOrder = order;
        this.onSuccess = onSuccess;
        subtitleLabel.setText("أمر الإيراد المستهدف: " + order.getOrderNumber() + " (المبلغ الأصلي: " + order.getAmount() + " د.ج)");
        
        if (defaultType != null) {
            typeCombo.setValue(defaultType);
        }
    }

    @FXML
    private void handleSave() {
        if (!validateForm()) return;

        RevenueOrderCancellation cancellation = new RevenueOrderCancellation();
        cancellation.setOriginalOrder(targetOrder);
        cancellation.setCancellationType(typeCombo.getValue());
        cancellation.setCancellationDate(cancellationDatePicker.getValue());
        cancellation.setReasonAr(reasonField.getText().trim());
        cancellation.setReasonFr(reasonFrField.getText().trim());
        cancellation.setCreatedBy(AppContext.getInstance().getCurrentUser());
        
        String providedNum = cancellationNumField.getText().trim();
        if (!providedNum.isEmpty()) {
            cancellation.setCancellationNumber(providedNum);
        }

        if (typeCombo.getValue() == CancellationType.REDUCTION || typeCombo.getValue() == CancellationType.INCREASE) {
            cancellation.setReducedAmount(new BigDecimal(reducedAmountField.getText().trim()));
        }

        concurrencyManager.runAsync(
            () -> {
                if (cancellation.getCancellationType() == CancellationType.REDUCTION) {
                    cancellationService.reduceOrder(cancellation);
                } else if (cancellation.getCancellationType() == CancellationType.INCREASE) {
                    cancellationService.increaseOrder(cancellation);
                } else {
                    cancellationService.cancelOrder(cancellation);
                }
                return true;
            },
            res -> {
                org.marrok.amriirad.util.DialogHelper.showLanguageDialog(lang -> printAnnexe(cancellation, lang));
                org.marrok.amriirad.util.DialogHelper.showInfo("نجاح", "تم حفظ الإلغاء بنجاح.");
                closeWindow();
                runOnSuccess();
            },
            err -> showError(err.getMessage())
        );
    }

    private void printAnnexe(RevenueOrderCancellation cancellation, org.marrok.amriirad.model.PrintLanguage lang) {
        java.util.Map<String, Object> params = ReportParamBuilder.create(tafqeetService)
            .withLanguage(lang)
            .withCancellation(cancellation)
            .build();
        
        String reportBasePath = (cancellation.getCancellationType() == CancellationType.REDUCTION || 
                                 cancellation.getCancellationType() == CancellationType.INCREASE) ? 
            "/org/marrok/amriirad/report/annexe4_reduction" : 
            "/org/marrok/amriirad/report/annexe3_full_cancel";
        
        String reportPath = reportBasePath + "_" + lang.getCode() + ".jrxml";
        
        reportService.showReportWithParamsOnly(reportPath, params);
    }

    @Override
    protected boolean validateForm() {
        clearError();
        if (targetOrder == null) {
            showError("خطأ داخلي: لا يوجد أمر إيراد مستهدف.");
            return false;
        }
        if (reasonField.getText() == null || reasonField.getText().trim().isEmpty()) {
            showError("يجب إدخال سبب الإلغاء/التخفيض.");
            return false;
        }
        if (typeCombo.getValue() == CancellationType.REDUCTION || typeCombo.getValue() == CancellationType.INCREASE) {
            try {
                BigDecimal amount = new BigDecimal(reducedAmountField.getText().trim());
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    showError("المبلغ يجب أن يكون أكبر من صفر.");
                    return false;
                }
                if (typeCombo.getValue() == CancellationType.REDUCTION && amount.compareTo(targetOrder.getAmount()) >= 0) {
                    showError("المبلغ المخفض يجب أن يكون أقل من المبلغ الأصلي (" + targetOrder.getAmount() + ").");
                    return false;
                }
            } catch (Exception e) {
                showError("قيمة المبلغ غير صالحة.");
                return false;
            }
        }
        return true;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

}

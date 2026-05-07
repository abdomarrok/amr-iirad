package org.marrok.amriirad.controller;

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
    @FXML private Label errorLabel;
    @FXML private Button saveBtn;

    private RevenueOrder targetOrder;
    private Runnable onSuccess;
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
            boolean isReduction = (newV == CancellationType.REDUCTION);
            reducedAmountLabel.setVisible(isReduction);
            reducedAmountLabel.setManaged(isReduction);
            reducedAmountField.setVisible(isReduction);
            reducedAmountField.setManaged(isReduction);
        });
    }

    public void initData(RevenueOrder order, Runnable onSuccess) {
        this.targetOrder = order;
        this.onSuccess = onSuccess;
        subtitleLabel.setText("أمر الإيراد المستهدف: " + order.getOrderNumber() + " (المبلغ الأصلي: " + order.getAmount() + " د.ج)");
    }

    @FXML
    private void handleSave() {
        if (!validateForm()) return;

        RevenueOrderCancellation cancellation = new RevenueOrderCancellation();
        cancellation.setOriginalOrder(targetOrder);
        cancellation.setCancellationType(typeCombo.getValue());
        cancellation.setCancellationDate(cancellationDatePicker.getValue());
        cancellation.setReasonAr(reasonField.getText().trim());
        cancellation.setCreatedBy("admin"); // TODO: Use real user
        
        String providedNum = cancellationNumField.getText().trim();
        if (!providedNum.isEmpty()) {
            cancellation.setCancellationNumber(providedNum);
        }

        if (typeCombo.getValue() == CancellationType.REDUCTION) {
            cancellation.setReducedAmount(new BigDecimal(reducedAmountField.getText().trim()));
        }

        concurrencyManager.runAsync(
            () -> {
                if (cancellation.getCancellationType() == CancellationType.REDUCTION) {
                    cancellationService.reduceOrder(cancellation);
                } else {
                    cancellationService.cancelOrder(cancellation);
                }
                return true;
            },
            res -> {
                printAnnexe(cancellation);
                closeWindow();
                runOnSuccess();
            },
            err -> showError(err.getMessage())
        );
    }

    private void printAnnexe(RevenueOrderCancellation cancellation) {
        java.util.Map<String, Object> params = ReportParamBuilder.create(tafqeetService)
            .withCancellation(cancellation)
            .build();
        
        String reportPath = cancellation.getCancellationType() == CancellationType.REDUCTION ? 
            "/org/marrok/amriirad/report/annexe4_reduction.jrxml" : 
            "/org/marrok/amriirad/report/annexe3_full_cancel.jrxml";
        
        reportService.showReportWithParamsOnly(reportPath, params);
    }

    private boolean validateForm() {
        clearError();
        if (targetOrder == null) {
            showError("خطأ داخلي: لا يوجد أمر إيراد مستهدف.");
            return false;
        }
        if (reasonField.getText() == null || reasonField.getText().trim().isEmpty()) {
            showError("يجب إدخال سبب الإلغاء/التخفيض.");
            return false;
        }
        if (typeCombo.getValue() == CancellationType.REDUCTION) {
            try {
                BigDecimal reduced = new BigDecimal(reducedAmountField.getText().trim());
                if (reduced.compareTo(BigDecimal.ZERO) <= 0) {
                    showError("المبلغ المخفض يجب أن يكون أكبر من صفر.");
                    return false;
                }
                if (reduced.compareTo(targetOrder.getAmount()) >= 0) {
                    showError("المبلغ المخفض يجب أن يكون أقل من المبلغ الأصلي (" + targetOrder.getAmount() + ").");
                    return false;
                }
            } catch (Exception e) {
                showError("قيمة المبلغ المخفض غير صالحة.");
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

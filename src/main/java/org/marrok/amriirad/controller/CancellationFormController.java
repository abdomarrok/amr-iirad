package org.marrok.amriirad.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.core.ConcurrencyManager;
import org.marrok.amriirad.model.RevenueOrderCancellation;
import org.marrok.amriirad.model.CancellationType;
import org.marrok.amriirad.model.RevenueOrder;
import org.marrok.amriirad.service.CancellationOrderService;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class CancellationFormController implements Initializable {

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
                                     org.marrok.amriirad.service.TafqeetService tafqeetService) {
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

        ConcurrencyManager.getInstance().runAsync(
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
                if (onSuccess != null) onSuccess.run();
            },
            err -> {
                logger.error("Failed to process cancellation", err);
                errorLabel.setText("❌ " + err.getMessage());
            }
        );
    }

    private void printAnnexe(RevenueOrderCancellation cancellation) {
        try {
            
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("CANCEL_NUMBER", cancellation.getCancellationNumber() != null ? cancellation.getCancellationNumber() : "");
            params.put("ORDER_NUMBER", targetOrder.getOrderNumber() != null ? targetOrder.getOrderNumber() : "");
            params.put("DEBTOR_NAME", targetOrder.getDebtor() != null ? targetOrder.getDebtor().getFullName() : "");
            params.put("DATE", cancellation.getCancellationDate() != null ? cancellation.getCancellationDate().toString() : "");
            params.put("REASON", cancellation.getReasonAr() != null ? cancellation.getReasonAr() : "");
            
            String reportPath;
            if (cancellation.getCancellationType() == CancellationType.REDUCTION) {
                params.put("AMOUNT", cancellation.getReducedAmount() != null ? cancellation.getReducedAmount().toString() : "");
                params.put("AMOUNT_WORDS", cancellation.getReducedAmount() != null ? tafqeetService.toArabicWords(cancellation.getReducedAmount()) : "");
                reportPath = "/org/marrok/amriirad/report/annexe4_reduction.jrxml";
            } else {
                params.put("AMOUNT", targetOrder.getAmount() != null ? targetOrder.getAmount().toString() : "");
                params.put("AMOUNT_WORDS", targetOrder.getAmount() != null ? tafqeetService.toArabicWords(targetOrder.getAmount()) : "");
                reportPath = "/org/marrok/amriirad/report/annexe3_full_cancel.jrxml";
            }
            
            reportService.showReportWithParamsOnly(reportPath, params);
        } catch (Exception e) {
            logger.error("Failed to auto-print cancellation report", e);
        }
    }

    private boolean validateForm() {
        errorLabel.setText("");
        if (targetOrder == null) {
            errorLabel.setText("❌ خطأ داخلي: لا يوجد أمر إيراد مستهدف.");
            return false;
        }
        if (reasonField.getText() == null || reasonField.getText().trim().isEmpty()) {
            errorLabel.setText("❌ يجب إدخال سبب الإلغاء/التخفيض.");
            return false;
        }
        if (typeCombo.getValue() == CancellationType.REDUCTION) {
            try {
                BigDecimal reduced = new BigDecimal(reducedAmountField.getText().trim());
                if (reduced.compareTo(BigDecimal.ZERO) <= 0) {
                    errorLabel.setText("❌ المبلغ المخفض يجب أن يكون أكبر من صفر.");
                    return false;
                }
                if (reduced.compareTo(targetOrder.getAmount()) >= 0) {
                    errorLabel.setText("❌ المبلغ المخفض يجب أن يكون أقل من المبلغ الأصلي (" + targetOrder.getAmount() + ").");
                    return false;
                }
            } catch (Exception e) {
                errorLabel.setText("❌ قيمة المبلغ المخفض غير صالحة.");
                return false;
            }
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

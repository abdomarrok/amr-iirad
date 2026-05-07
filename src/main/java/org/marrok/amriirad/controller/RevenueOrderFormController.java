package org.marrok.amriirad.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.core.ConcurrencyManager;
import org.marrok.amriirad.model.BudgetChapter;
import org.marrok.amriirad.model.Debtor;
import org.marrok.amriirad.model.FiscalYear;
import org.marrok.amriirad.model.RevenueOrder;
import org.marrok.amriirad.model.OrderStatus;
import org.marrok.amriirad.repository.BudgetChapterRepository;
import org.marrok.amriirad.repository.DebtorRepository;
import org.marrok.amriirad.repository.FiscalYearRepository;
import org.marrok.amriirad.service.RevenueOrderService;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ResourceBundle;

public class RevenueOrderFormController implements Initializable {

    private static final Logger logger = LogManager.getLogger(RevenueOrderFormController.class);

    @FXML private Label titleLabel;
    @FXML private TextField fiscalYearField;
    @FXML private DatePicker issueDatePicker;
    @FXML private ComboBox<Debtor> debtorCombo;
    @FXML private ComboBox<BudgetChapter> budgetChapterCombo;
    @FXML private TextField amountField;
    @FXML private TextArea objectField;
    @FXML private Label errorLabel;
    @FXML private Button saveBtn;
    @FXML private Button issueBtn;

    @FXML private Button printAdminBtn;
    @FXML private Button printDebtorBtn;

    private final FiscalYearRepository fyRepo;
    private final DebtorRepository debtorRepo;
    private final BudgetChapterRepository chapterRepo;
    private final RevenueOrderService orderService;
    private final org.marrok.amriirad.service.ReportService reportService;
    private final org.marrok.amriirad.service.TafqeetService tafqeetService;
    private final org.marrok.amriirad.core.ConcurrencyManager concurrencyManager;

    public RevenueOrderFormController(FiscalYearRepository fyRepo, 
                                     DebtorRepository debtorRepo, 
                                     BudgetChapterRepository chapterRepo, 
                                     RevenueOrderService orderService,
                                     org.marrok.amriirad.service.ReportService reportService,
                                     org.marrok.amriirad.service.TafqeetService tafqeetService,
                                     org.marrok.amriirad.core.ConcurrencyManager concurrencyManager) {
        this.fyRepo = fyRepo;
        this.debtorRepo = debtorRepo;
        this.chapterRepo = chapterRepo;
        this.orderService = orderService;
        this.reportService = reportService;
        this.tafqeetService = tafqeetService;
        this.concurrencyManager = concurrencyManager;
    }

    private FiscalYear activeYear;
    private RevenueOrder currentOrder;
    private Runnable onSuccess;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        issueDatePicker.setValue(LocalDate.now());
        loadDropdownData();
    }

    public void initForCreate(Runnable onSuccessCallback) {
        this.onSuccess = onSuccessCallback;
        this.currentOrder = new RevenueOrder();
        titleLabel.setText("إنشاء أمر إيراد جديد");
    }

    public void initForEdit(RevenueOrder order, Runnable onSuccessCallback) {
        this.onSuccess = onSuccessCallback;
        this.currentOrder = order;
        titleLabel.setText("تعديل أمر إيراد: " + order.getOrderNumber());

        issueDatePicker.setValue(order.getIssueDate());
        amountField.setText(order.getAmount() != null ? order.getAmount().toString() : "");
        objectField.setText(order.getObjectAr());
        
        if (order.getStatus() == OrderStatus.DRAFT) {
            issueBtn.setVisible(true);
            issueBtn.setManaged(true);
        } else {
            // Lock fields if not draft
            saveBtn.setDisable(true);
            issueBtn.setDisable(true);
            amountField.setDisable(true);
            objectField.setDisable(true);
            debtorCombo.setDisable(true);
            budgetChapterCombo.setDisable(true);
            issueDatePicker.setDisable(true);

            // Show print buttons since it's issued
            printAdminBtn.setVisible(true);
            printAdminBtn.setManaged(true);
            printDebtorBtn.setVisible(true);
            printDebtorBtn.setManaged(true);
        }
    }

    private void loadDropdownData() {
        concurrencyManager.runAsync(
            () -> {
                Optional<FiscalYear> activeFy = fyRepo.findActive();
                activeFy.ifPresent(fy -> activeYear = fy);
                var debtors = debtorRepo.findAll();
                var chapters = chapterRepo.findAll();
                return new Object[]{debtors, chapters};
            },
            result -> {
                if (activeYear != null) {
                    fiscalYearField.setText(activeYear.getYearLabel());
                    currentOrder.setFiscalYear(activeYear);
                } else {
                    errorLabel.setText("❌ لا توجد سنة مالية مفعّلة.");
                    saveBtn.setDisable(true);
                }

                Object[] arr = (Object[]) result;
                debtorCombo.setItems(FXCollections.observableArrayList((java.util.List<Debtor>) arr[0]));
                budgetChapterCombo.setItems(FXCollections.observableArrayList((java.util.List<BudgetChapter>) arr[1]));

                if (currentOrder.getDebtor() != null) {
                    debtorCombo.getItems().stream()
                        .filter(d -> d.getId() == currentOrder.getDebtor().getId())
                        .findFirst().ifPresent(debtorCombo.getSelectionModel()::select);
                }
                if (currentOrder.getBudgetChapter() != null) {
                    budgetChapterCombo.getItems().stream()
                        .filter(c -> c.getId() == currentOrder.getBudgetChapter().getId())
                        .findFirst().ifPresent(budgetChapterCombo.getSelectionModel()::select);
                }
            },
            err -> {
                logger.error("Failed to load form data", err);
                errorLabel.setText("❌ خطأ في تحميل البيانات الأساسية.");
            }
        );
    }

    @FXML
    private void handleNewDebtor() {
        Stage owner = (Stage) saveBtn.getScene().getWindow();
        javafx.fxml.FXMLLoader loader = org.marrok.amriirad.util.GeneralUtil.openModal(owner, "/org/marrok/amriirad/view/debtor-form-view.fxml", "إضافة مدين جديد");
        if (loader != null) {
            DebtorFormController controller = loader.getController();
            controller.initForCreate(() -> loadDropdownData());
        }
    }

    @FXML
    private void handleSave() {
        if (!validateForm()) return;
        populateOrder();

        concurrencyManager.runAsync(
            () -> {
                if (currentOrder.getId() == 0) {
                    currentOrder.setCreatedBy("admin"); // TODO: Use real user
                    orderService.createOrder(currentOrder);
                } else {
                    orderService.updateOrder(currentOrder);
                }
                return true;
            },
            res -> {
                closeWindow();
                if (onSuccess != null) onSuccess.run();
            },
            err -> {
                logger.error("Failed to save order", err);
                errorLabel.setText("❌ " + err.getMessage());
            }
        );
    }

    @FXML
    private void handleIssue() {
        if (!validateForm()) return;
        populateOrder();

        concurrencyManager.runAsync(
            () -> {
                orderService.updateOrder(currentOrder); // Save changes first
                orderService.issueOrder(currentOrder.getId(), "admin"); // Then issue
                return true;
            },
            res -> {
                closeWindow();
                if (onSuccess != null) onSuccess.run();
            },
            err -> {
                logger.error("Failed to issue order", err);
                errorLabel.setText("❌ " + err.getMessage());
            }
        );
    }

    private boolean validateForm() {
        errorLabel.setText("");
        if (debtorCombo.getValue() == null) {
            errorLabel.setText("❌ يجب اختيار المدين");
            return false;
        }
        if (budgetChapterCombo.getValue() == null) {
            errorLabel.setText("❌ يجب اختيار محور الميزانية");
            return false;
        }
        try {
            BigDecimal amt = new BigDecimal(amountField.getText().trim());
            if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                errorLabel.setText("❌ المبلغ يجب أن يكون أكبر من صفر");
                return false;
            }
        } catch (Exception e) {
            errorLabel.setText("❌ المبلغ غير صالح");
            return false;
        }
        if (objectField.getText() == null || objectField.getText().trim().isEmpty()) {
            errorLabel.setText("❌ يجب إدخال موضوع الإيراد (الأسباب)");
            return false;
        }
        return true;
    }

    private void populateOrder() {
        currentOrder.setIssueDate(issueDatePicker.getValue());
        currentOrder.setDebtor(debtorCombo.getValue());
        currentOrder.setBudgetChapter(budgetChapterCombo.getValue());
        currentOrder.setAmount(new BigDecimal(amountField.getText().trim()));
        currentOrder.setObjectAr(objectField.getText().trim());
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) saveBtn.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handlePrintAdmin() {
        printAnnexe("/org/marrok/amriirad/report/annexe1_order.jrxml");
    }

    @FXML
    private void handlePrintDebtor() {
        printAnnexe("/org/marrok/amriirad/report/annexe2_debtor_copy.jrxml");
    }

    private void printAnnexe(String reportPath) {
        if (currentOrder == null || currentOrder.getId() == 0) return;
        
        concurrencyManager.runAsync(
            () -> {
                java.util.Map<String, Object> params = new java.util.HashMap<>();
                
                // Order & Fiscal Year
                params.put("ORDER_NUMBER", currentOrder.getOrderNumber() != null ? currentOrder.getOrderNumber() : "");
                params.put("FISCAL_YEAR", currentOrder.getFiscalYear() != null ? 
                    String.valueOf(currentOrder.getFiscalYear().getYearLabel()) : "");
                params.put("ISSUE_DATE", currentOrder.getIssueDate() != null ? 
                    currentOrder.getIssueDate().toString() : "");
                
                // Debtor Information (from Debtor entity)
                org.marrok.amriirad.model.Debtor debtor = currentOrder.getDebtor();
                params.put("DEBTOR_NAME", debtor != null ? debtor.getFullName() : "");
                params.put("DEBTOR_ADDRESS", debtor != null ? debtor.getAddress() : "");
                params.put("DEBTOR_ACCOUNT", debtor != null ? (debtor.getBankAccount() != null ? debtor.getBankAccount() : "") : "");
                params.put("DEBTOR_CNAS", debtor != null ? (debtor.getCnasNumber() != null ? debtor.getCnasNumber() : "") : "");
                params.put("DEBTOR_NIF", debtor != null ? (debtor.getNifNumber() != null ? debtor.getNifNumber() : "") : "");
                
                // Budget Information
                params.put("BUDGET_CHAPTER", currentOrder.getBudgetChapter() != null ? 
                    currentOrder.getBudgetChapter().getCode() : "");
                
                // Financial Information
                params.put("AMOUNT", currentOrder.getAmount() != null ? 
                    String.format("%,.2f", currentOrder.getAmount()) : "0.00");
                params.put("AMOUNT_WORDS", currentOrder.getAmount() != null ? 
                    tafqeetService.toArabicWords(currentOrder.getAmount()) : "");
                
                // Content Fields
                params.put("REASON_AR", currentOrder.getObjectAr() != null ? 
                    currentOrder.getObjectAr() : "");
                params.put("LIQUIDATION_BASIS", currentOrder.getObjectAr() != null ? 
                    currentOrder.getObjectAr() : "");
                params.put("TREASURY_REF", "1980000034/55"); // TODO: Make configurable via AppSettings
                
                reportService.showReportWithParamsOnly(reportPath, params);
                return true;
            },
            res -> logger.info("Print triggered for {}", reportPath),
            err -> {
                logger.error("Print failed", err);
                errorLabel.setText("❌ خطأ في الطباعة: " + err.getMessage());
            }
        );
    }
}

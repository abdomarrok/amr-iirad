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
import org.marrok.amriirad.util.ReportParamBuilder;
import org.marrok.amriirad.util.SceneManager;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.Optional;
import java.util.ResourceBundle;

public class RevenueOrderFormController extends BaseFormController implements Initializable {

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
    private final org.marrok.amriirad.service.InstitutionService institutionService;

    public RevenueOrderFormController(FiscalYearRepository fyRepo, 
                                     DebtorRepository debtorRepo, 
                                     BudgetChapterRepository chapterRepo, 
                                     RevenueOrderService orderService,
                                     org.marrok.amriirad.service.ReportService reportService,
                                     org.marrok.amriirad.service.TafqeetService tafqeetService,
                                     org.marrok.amriirad.service.InstitutionService institutionService,
                                     org.marrok.amriirad.core.ConcurrencyManager concurrencyManager) {
        super(concurrencyManager);
        this.fyRepo = fyRepo;
        this.debtorRepo = debtorRepo;
        this.chapterRepo = chapterRepo;
        this.orderService = orderService;
        this.reportService = reportService;
        this.tafqeetService = tafqeetService;
        this.institutionService = institutionService;
    }

    private FiscalYear activeYear;
    private RevenueOrder currentOrder;

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
                    showError("لا توجد سنة مالية مفعّلة.");
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
                showError("خطأ في تحميل البيانات الأساسية: " + err.getMessage());
            }
        );
    }

    @FXML
    private void handleNewDebtor() {
        Stage owner = (Stage) saveBtn.getScene().getWindow();
        javafx.fxml.FXMLLoader loader = SceneManager.openModal(owner, "/org/marrok/amriirad/view/debtor-form-view.fxml", "إضافة مدين جديد");
        if (loader != null) {
            DebtorFormController controller = loader.getController();
            controller.initForCreate(() -> loadDropdownData());
        }
    }

    @FXML
    private void handleSave() {
        if (!validateForm()) return;
        clearError();
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
                runOnSuccess();
            },
            err -> showError(err.getMessage())
        );
    }

    @FXML
    private void handleIssue() {
        if (!validateForm()) return;
        clearError();
        populateOrder();

        concurrencyManager.runAsync(
            () -> {
                orderService.updateOrder(currentOrder); // Save changes first
                orderService.issueOrder(currentOrder.getId(), "admin"); // Then issue
                return true;
            },
            res -> {
                closeWindow();
                runOnSuccess();
            },
            err -> showError(err.getMessage())
        );
    }

    private boolean validateForm() {
        clearError();
        if (debtorCombo.getValue() == null) {
            showError("يجب اختيار المدين");
            return false;
        }
        if (budgetChapterCombo.getValue() == null) {
            showError("يجب اختيار محور الميزانية");
            return false;
        }
        try {
            BigDecimal amt = new BigDecimal(amountField.getText().trim());
            if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                showError("المبلغ يجب أن يكون أكبر من صفر");
                return false;
            }
        } catch (Exception e) {
            showError("المبلغ غير صالح");
            return false;
        }
        if (objectField.getText() == null || objectField.getText().trim().isEmpty()) {
            showError("يجب إدخال موضوع الإيراد (الأسباب)");
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

    @Override
    protected Logger getLogger() {
        return logger;
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
                java.util.Map<String, Object> params = ReportParamBuilder.create(tafqeetService)
                    .withInstitution(institutionService.getInfo())
                    .withOrder(currentOrder)
                    .build();
                
                reportService.showReportWithParamsOnly(reportPath, params);
                return true;
            },
            res -> logger.info("Print triggered for {}", reportPath),
            err -> showError("خطأ في الطباعة: " + err.getMessage())
        );
    }
}

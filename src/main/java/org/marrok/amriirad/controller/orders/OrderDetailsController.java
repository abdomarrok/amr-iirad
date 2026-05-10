package org.marrok.amriirad.controller.orders;

import org.marrok.amriirad.controller.BaseFormController;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kordamp.ikonli.javafx.FontIcon;
import org.marrok.amriirad.core.ConcurrencyManager;
import org.marrok.amriirad.model.RevenueOrder;
import org.marrok.amriirad.model.OrderStatus;
import org.marrok.amriirad.model.CancellationType;
import org.marrok.amriirad.service.ReportService;
import org.marrok.amriirad.service.TafqeetService;
import org.marrok.amriirad.service.InstitutionService;
import org.marrok.amriirad.service.CancellationOrderService;
import org.marrok.amriirad.util.ReportParamBuilder;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.ResourceBundle;

import org.marrok.amriirad.util.ReportParamBuilder;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.Map;

public class OrderDetailsController extends BaseFormController implements Initializable {

    private static final Logger logger = LogManager.getLogger(OrderDetailsController.class);

    @FXML private Label titleLabel;
    @FXML private Label orderNumberLabel;
    @FXML private Label issueDateLabel;
    @FXML private Label fiscalYearLabel;
    @FXML private Label statusLabel;
    @FXML private Label debtorNameLabel;
    @FXML private Label debtorTypeLabel;
    @FXML private Label debtorPhoneLabel;
    @FXML private Label amountLabel;
    @FXML private Label amountInWordsLabel;
    @FXML private Label chapterLabel;
    @FXML private Label objectLabel;
    @FXML private VBox timelineContainer;
    @FXML private Button printAdminBtn;
    @FXML private Button printDebtorBtn;
    @FXML private Button printCancelBtn;
    @FXML private Button printReduceBtn;
    @FXML private Button cancelActionBtn;
    @FXML private Button reduceActionBtn;

    private final ReportService reportService;
    private final TafqeetService tafqeetService;
    private final InstitutionService institutionService;
    private final CancellationOrderService cancellationService;
    private final org.marrok.amriirad.service.AuthService authService;
    
    private RevenueOrder currentOrder;

    public OrderDetailsController(ReportService reportService, TafqeetService tafqeetService, 
                                 InstitutionService institutionService, 
                                 CancellationOrderService cancellationService,
                                 org.marrok.amriirad.service.AuthService authService,
                                 ConcurrencyManager concurrencyManager) {
        super(concurrencyManager);
        this.reportService = reportService;
        this.tafqeetService = tafqeetService;
        this.institutionService = institutionService;
        this.cancellationService = cancellationService;
        this.authService = authService;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Nothing to do on initialization — wait for initForView()
    }

    public void initForView(RevenueOrder order, Runnable onRefresh) {
        this.currentOrder = order;
        this.onSuccess = onRefresh;
        loadOrderDetails();
    }

    private void loadOrderDetails() {
        if (currentOrder == null) {
            showError("No order data available");
            return;
        }

        // Set basic info
        titleLabel.setText("تفاصيل أمر الإيراد: " + currentOrder.getOrderNumber());
        orderNumberLabel.setText(currentOrder.getOrderNumber() != null ? currentOrder.getOrderNumber() : "-");
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        issueDateLabel.setText(currentOrder.getIssueDate() != null ? currentOrder.getIssueDate().format(dateFormatter) : "-");
        
        fiscalYearLabel.setText(currentOrder.getFiscalYear() != null ? currentOrder.getFiscalYear().getYearLabel() : "-");
        
        // Set status with appropriate styling
        if (currentOrder.getStatus() != null) {
            statusLabel.setText(getStatusArabicLabel(currentOrder.getStatus()));
            statusLabel.setStyle(getStatusColor(currentOrder.getStatus()));
        } else {
            statusLabel.setText("-");
        }

        // Set debtor info
        if (currentOrder.getDebtor() != null) {
            debtorNameLabel.setText(currentOrder.getDebtor().getFullName());
            debtorTypeLabel.setText(currentOrder.getDebtor().getDebtorType() != null ? 
                currentOrder.getDebtor().getDebtorType().getArabicLabel() : "-");
            debtorPhoneLabel.setText(currentOrder.getDebtor().getPhone() != null ? 
                currentOrder.getDebtor().getPhone() : "-");
        }

        // Set financial info
        amountLabel.setText(currentOrder.getAmount() != null ? currentOrder.getAmount().toPlainString() : "0.00");
        amountInWordsLabel.setText(currentOrder.getAmountInWordsAr() != null ? 
            currentOrder.getAmountInWordsAr() : "-");
        
        if (currentOrder.getBudgetChapter() != null) {
            chapterLabel.setText(currentOrder.getBudgetChapter().getCode() + " - " + 
                currentOrder.getBudgetChapter().getLabelAr());
        } else {
            chapterLabel.setText("-");
        }

        // Set object
        objectLabel.setText(currentOrder.getObjectAr() != null ? currentOrder.getObjectAr() : "-");

        // Visibility logic based on status
        updateButtonVisibility();

        // Build timeline
        buildStatusTimeline();
    }

    private void updateButtonVisibility() {
        boolean isDraft = currentOrder.getStatus() == OrderStatus.DRAFT;
        boolean isIssued = currentOrder.getStatus() == OrderStatus.ISSUED;
        boolean isCancelled = currentOrder.getStatus() == OrderStatus.CANCELLED;
        boolean isReduced = currentOrder.getStatus() == OrderStatus.REDUCED;
        boolean isDispatched = currentOrder.getStatus() == OrderStatus.DISPATCHED;

        // Common print buttons (Annex 1 & 2) shown for anything but draft
        setBtnVisible(printAdminBtn, !isDraft);
        setBtnVisible(printDebtorBtn, !isDraft);

        // Cancellation/Reduction specific buttons
        setBtnVisible(printCancelBtn, isCancelled);
        setBtnVisible(printReduceBtn, isReduced);

        // Actions: can cancel/reduce if ISSUED (and not dispatched)
        boolean canManage = authService.canDo("orders.edit");
        setBtnVisible(cancelActionBtn, isIssued && canManage);
        setBtnVisible(reduceActionBtn, isIssued && canManage);
    }

    private void setBtnVisible(Button btn, boolean visible) {
        if (btn != null) {
            btn.setVisible(visible);
            btn.setManaged(visible);
        }
    }

    private void buildStatusTimeline() {
        timelineContainer.getChildren().clear();

        // Create timeline items based on order status
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // DRAFT status (always present)
        addTimelineItem("مسودة (DRAFT)", currentOrder.getCreatedAt(), "تم إنشاء الأمر في نظام", true, "fas-file-invoice");

        // ISSUED status (if applicable)
        if (currentOrder.getStatus().ordinal() >= OrderStatus.ISSUED.ordinal() || isCancelledOrReduced()) {
            addTimelineItem("مُصدّر (ISSUED)", currentOrder.getUpdatedAt(), "تم إصدار الأمر رسمياً", 
                currentOrder.getStatus() == OrderStatus.ISSUED, "fas-check-circle");
        }

        // DISPATCHED status (if applicable)
        if (currentOrder.getStatus() == OrderStatus.DISPATCHED) {
            addTimelineItem("مُرسل (DISPATCHED)", currentOrder.getUpdatedAt(), "تم إرسال الأمر للخزينة", true, "fas-truck");
        }

        // CANCELLED status (if applicable)
        if (currentOrder.getStatus() == OrderStatus.CANCELLED) {
            addTimelineItem("ملغى (CANCELLED)", currentOrder.getUpdatedAt(), "تم إلغاء الأمر كلياً (ملحق 3)", true, "fas-ban");
        }

        // REDUCED status (if applicable)
        if (currentOrder.getStatus() == OrderStatus.REDUCED) {
            addTimelineItem("مُختزل (REDUCED)", currentOrder.getUpdatedAt(), "تم تخفيض مبلغ الأمر (ملحق 4)", true, "fas-minus-circle");
        }
    }

    private boolean isCancelledOrReduced() {
        return currentOrder.getStatus() == OrderStatus.CANCELLED || currentOrder.getStatus() == OrderStatus.REDUCED;
    }

    private void addTimelineItem(String statusName, Object timestamp, String description, boolean isActive, String icon) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/org/marrok/amriirad/view/orders/timeline-item.fxml"));
            VBox item = loader.load();
            
            FontIcon statusIcon = (FontIcon) item.lookup("#statusIcon");
            Label statusLabel = (Label) item.lookup("#statusLabel");
            Label descLabel = (Label) item.lookup("#descriptionLabel");
            Label timestampLabel = (Label) item.lookup("#timestampLabel");
            
            if (statusIcon != null) {
                statusIcon.setIconLiteral(icon);
                statusIcon.getStyleClass().add(isActive ? "icon-primary" : "icon-secondary");
            }
            if (statusLabel != null) statusLabel.setText(statusName);
            if (descLabel != null) descLabel.setText(description);
            if (timestampLabel != null) timestampLabel.setText(timestamp != null ? timestamp.toString() : "N/A");
            
            timelineContainer.getChildren().add(item);
        } catch (java.io.IOException e) {
            logger.error("Failed to load timeline item FXML", e);
        }
    }

    private String getStatusArabicLabel(OrderStatus status) {
        return switch (status) {
            case DRAFT -> "مسودة";
            case ISSUED -> "مُصدّر";
            case DISPATCHED -> "مُرسل";
            case CANCELLED -> "ملغى";
            case REDUCED -> "مُختزل";
            default -> status.name();
        };
    }

    private String getStatusColor(OrderStatus status) {
        return switch (status) {
            case DRAFT -> "-fx-text-fill: -fx-theme-warning;";
            case ISSUED -> "-fx-text-fill: -fx-theme-success;";
            case DISPATCHED -> "-fx-text-fill: -fx-theme-info;";
            case CANCELLED -> "-fx-text-fill: -fx-theme-danger;";
            case REDUCED -> "-fx-text-fill: -fx-theme-purple;";
        };
    }

    @FXML
    private void handlePrintAdmin() {
        org.marrok.amriirad.util.DialogHelper.showLanguageDialog(lang -> printAnnexe("/org/marrok/amriirad/report/annexe1_order", lang));
    }

    @FXML
    private void handlePrintDebtor() {
        org.marrok.amriirad.util.DialogHelper.showLanguageDialog(lang -> printAnnexe("/org/marrok/amriirad/report/annexe2_debtor_copy", lang));
    }

    @FXML
    private void handlePrintCancel() {
        org.marrok.amriirad.util.DialogHelper.showLanguageDialog(lang -> fetchCancellationAndPrint("/org/marrok/amriirad/report/annexe3_full_cancel", lang));
    }

    @FXML
    private void handlePrintReduce() {
        org.marrok.amriirad.util.DialogHelper.showLanguageDialog(lang -> fetchCancellationAndPrint("/org/marrok/amriirad/report/annexe4_reduction", lang));
    }

    private void fetchCancellationAndPrint(String baseReportPath, org.marrok.amriirad.model.PrintLanguage lang) {
        String fullPath = baseReportPath + "_" + lang.getCode() + ".jrxml";
        concurrencyManager.runAsync(
            () -> cancellationService.findByOrderId(currentOrder.getId()),
            opt -> {
                opt.ifPresentOrElse(
                    c -> {
                        Map<String, Object> params = ReportParamBuilder.create(tafqeetService)
                            .withLanguage(lang)
                            .withInstitution(institutionService.getInfo())
                            .withCancellation(c)
                            .build();
                        reportService.showReportWithParamsOnly(fullPath, params);
                    },
                    () -> showError("تعذر العثور على سجل الإلغاء/التخفيض.")
                );
            },
            err -> showError("خطأ في جلب بيانات الإلغاء: " + err.getMessage())
        );
    }

    private void printAnnexe(String baseReportPath, org.marrok.amriirad.model.PrintLanguage lang) {
        if (currentOrder == null) {
            showError("No order data available");
            return;
        }
        String fullPath = baseReportPath + "_" + lang.getCode() + ".jrxml";
        concurrencyManager.runAsync(
            () -> {
                Map<String, Object> params = ReportParamBuilder.create(tafqeetService)
                    .withLanguage(lang)
                    .withInstitution(institutionService.getInfo())
                    .withOrder(currentOrder)
                    .build();
                
                reportService.showReportWithParamsOnly(fullPath, params);
                return true;
            },
            res -> logger.info("Print triggered for {} in {}", fullPath, lang),
            err -> showError("خطأ في الطباعة: " + err.getMessage())
        );
    }

    @FXML
    private void handleCancelAction() {
        openCancellationForm(CancellationType.FULL_CANCEL);
    }

    @FXML
    private void handleReduceAction() {
        openCancellationForm(CancellationType.REDUCTION);
    }

    private void openCancellationForm(CancellationType defaultType) {
        Stage stage = (Stage) titleLabel.getScene().getWindow();
        javafx.fxml.FXMLLoader loader = org.marrok.amriirad.util.SceneManager.openModal(
            stage, "/org/marrok/amriirad/view/orders/cancellation-form-view.fxml", "إلغاء / تخفيض أمر الإيراد");
        
        if (loader != null) {
            CancellationFormController controller = loader.getController();
            controller.initData(currentOrder, defaultType, () -> {
                // Refresh order data and UI
                refreshOrder();
            });
        }
    }

    private void refreshOrder() {
        // Close details and trigger parent refresh
        closeWindow();
        runOnSuccess();
    }

    @FXML
    private void handleClose() {
        closeWindow();
    }

    @Override
    protected boolean validateForm() {
        return true; // Read-only view
    }

    @Override
    protected void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("خطأ");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    @Override
    protected void clearError() {
        // No error label in this view
    }
}

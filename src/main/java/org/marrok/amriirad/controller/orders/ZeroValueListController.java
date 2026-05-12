package org.marrok.amriirad.controller.orders;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.marrok.amriirad.service.ExportService;

import java.io.File;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.controller.BaseController;
import org.marrok.amriirad.core.ConcurrencyManager;
import org.marrok.amriirad.model.ZeroValueDecision;
import org.marrok.amriirad.service.ZeroValueService;
import org.marrok.amriirad.repository.FiscalYearRepository;
import org.marrok.amriirad.util.SceneManager;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.Map;
import java.util.ResourceBundle;
import org.kordamp.ikonli.javafx.FontIcon;
import org.marrok.amriirad.util.ReportParamBuilder;

public class ZeroValueListController extends BaseController implements Initializable {
    private static final Logger logger = LogManager.getLogger(ZeroValueListController.class);

    @FXML private TableView<ZeroValueDecision> decisionTable;
    @FXML private TableColumn<ZeroValueDecision, String> numberCol;
    @FXML private TableColumn<ZeroValueDecision, LocalDate> dateCol;
    @FXML private TableColumn<ZeroValueDecision, BigDecimal> amountCol;
    @FXML private TableColumn<ZeroValueDecision, Void> actionsCol;
    @FXML private Button exportBtn;

    private final ZeroValueService zeroService;
    private final FiscalYearRepository fiscalRepo;
    private final ExportService exportService;

    public ZeroValueListController(ZeroValueService zeroService, 
                                 FiscalYearRepository fiscalRepo,
                                 ExportService exportService,
                                 ConcurrencyManager concurrencyManager) {
        super(concurrencyManager);
        this.zeroService = zeroService;
        this.fiscalRepo = fiscalRepo;
        this.exportService = exportService;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
        loadData();
    }

    private void setupTable() {
        numberCol.setCellValueFactory(new PropertyValueFactory<>("decisionNumber"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("decisionDate"));
        amountCol.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));

        // Format amount
        amountCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(String.format("%,.2f د.ج", item));
            }
        });

        // Setup actions
        actionsCol.setCellFactory(column -> new TableCell<>() {
            private final Button printBtn = new Button();
            {
                printBtn.setGraphic(new FontIcon("fas-print"));
                printBtn.getStyleClass().addAll("btn-outline", "btn-small");
                printBtn.setOnAction(event -> handlePrint(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(printBtn);
            }
        });
    }

    @FXML
    private void handleExport() {
        if (decisionTable.getItems().isEmpty()) {
            org.marrok.amriirad.util.DialogHelper.showError("تنبيه", "لا توجد بيانات لتصديرها.");
            return;
        }

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("تصدير البيانات");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        fileChooser.setInitialFileName("zero_value_decisions_" + java.time.LocalDate.now() + ".csv");

        File file = fileChooser.showSaveDialog(decisionTable.getScene().getWindow());
        if (file != null) {
            concurrencyManager.runAsync(
                () -> {
                    exportService.exportZeroValueDecisionsToCSV(decisionTable.getItems(), file);
                    return true;
                },
                res -> org.marrok.amriirad.util.DialogHelper.showInfo("نجاح", "تم تصدير البيانات بنجاح إلى:\n" + file.getName()),
                err -> {
                    logger.error("Export failed", err);
                    org.marrok.amriirad.util.DialogHelper.showError("خطأ", "فشل تصدير البيانات: " + err.getMessage());
                }
            );
        }
    }

    private void handlePrint(ZeroValueDecision decision) {
        org.marrok.amriirad.util.DialogHelper.showLanguageDialog(lang -> {
            concurrencyManager.runAsync(
                () -> {
                    var instService = org.marrok.amriirad.core.AppContext.getInstance().getInstitutionService();
                    var tafqeet = org.marrok.amriirad.core.AppContext.getInstance().getTafqeetService();
                    
                    // Load details before printing (lazy loading)
                    zeroService.loadDetails(decision);

                    Map<String, Object> params = ReportParamBuilder.create(tafqeet)
                        .withLanguage(lang)
                        .withInstitution(instService.getInfo())
                        .withZeroValueDecision(decision)
                        .build();
                    
                    params.put("REPORT_DATA_SOURCE", new net.sf.jasperreports.engine.data.JRBeanCollectionDataSource(decision.getDetails()));
                    return params;
                },
                params -> {
                    var reportService = org.marrok.amriirad.core.AppContext.getInstance().getReportService();
                    String reportPath = "/org/marrok/amriirad/report/annexe6_zero_values_" + lang.getCode() + ".jrxml";
                    var dataSource = new net.sf.jasperreports.engine.data.JRBeanCollectionDataSource(decision.getDetails());
                    reportService.showReport(reportPath, params, dataSource);
                },
                err -> logger.error("Print failed", err)
            );
        });
    }

    private void loadData() {
        concurrencyManager.runAsync(
            () -> {
                int activeYear = fiscalRepo.findActive().map(org.marrok.amriirad.model.FiscalYear::getId).orElse(0);
                return zeroService.listByYear(activeYear);
            },
            res -> decisionTable.setItems(FXCollections.observableArrayList(res)),
            err -> logger.error("Failed to load zero value decisions", err)
        );
    }

    @FXML
    private void handleNewDecision() {
        Stage owner = (Stage) decisionTable.getScene().getWindow();
        SceneManager.showModal(owner, "/org/marrok/amriirad/view/orders/zero-value-form-view.fxml", "مقرر قيم منعدمة جديد", this::loadData);
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}

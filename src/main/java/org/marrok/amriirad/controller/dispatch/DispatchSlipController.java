package org.marrok.amriirad.controller.dispatch;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.util.SceneManager;
import org.marrok.amriirad.core.ConcurrencyManager;
import org.marrok.amriirad.model.DispatchSlip;
import org.marrok.amriirad.model.RevenueOrder;
import org.marrok.amriirad.repository.DispatchSlipRepository;
import org.marrok.amriirad.util.DialogHelper;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;
import org.marrok.amriirad.ui.AsyncTableLoader;

public class DispatchSlipController implements Initializable {

    private static final Logger logger = LogManager.getLogger(DispatchSlipController.class);

    @FXML private org.marrok.amriirad.controller.shared.TopBarController topBarController;
    @FXML private org.marrok.amriirad.controller.shared.FooterController footerController;

    // Left pane (Slips)
    @FXML private TableView<DispatchSlip> slipsTable;
    @FXML private TableColumn<DispatchSlip, String> colSlipNumber;
    @FXML private TableColumn<DispatchSlip, String> colSlipDate;
    @FXML private TableColumn<DispatchSlip, String> colSlipTotal;
    @FXML private TextField searchField;
    @FXML private ProgressIndicator loadingIndicator;

    // Right pane (Details)
    @FXML private Label detailTitleLabel;
    @FXML private TextField detailNumberField;
    @FXML private DatePicker detailDatePicker;
    @FXML private Button printBtn;
    @FXML private Label totalAmountLabel;
    
    // Right pane (Orders table)
    @FXML private TableView<RevenueOrder> ordersTable;
    @FXML private TableColumn<RevenueOrder, String> colOrderNum;
    @FXML private TableColumn<RevenueOrder, String> colOrderDebtor;
    @FXML private TableColumn<RevenueOrder, String> colOrderAmount;

    private AsyncTableLoader<DispatchSlip> tableLoader;

    private final DispatchSlipRepository slipRepo;
    private final org.marrok.amriirad.repository.FiscalYearRepository fyRepo;
    private final org.marrok.amriirad.service.ReportService reportService;
    private final org.marrok.amriirad.service.TafqeetService tafqeetService;
    private final org.marrok.amriirad.service.InstitutionService institutionService;
    private final org.marrok.amriirad.service.AuthService authService;
    private final org.marrok.amriirad.core.ConcurrencyManager concurrencyManager;

    public DispatchSlipController(DispatchSlipRepository slipRepo, 
                                  org.marrok.amriirad.repository.FiscalYearRepository fyRepo,
                                  org.marrok.amriirad.service.ReportService reportService,
                                  org.marrok.amriirad.service.TafqeetService tafqeetService,
                                  org.marrok.amriirad.service.InstitutionService institutionService,
                                  org.marrok.amriirad.service.AuthService authService,
                                  org.marrok.amriirad.core.ConcurrencyManager concurrencyManager) {
        this.slipRepo = slipRepo;
        this.fyRepo = fyRepo;
        this.reportService = reportService;
        this.tafqeetService = tafqeetService;
        this.institutionService = institutionService;
        this.authService = authService;
        this.concurrencyManager = concurrencyManager;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (topBarController != null) {
            topBarController.setBackVisible(true);
        }
        tableLoader = new AsyncTableLoader<>(concurrencyManager, slipsTable, loadingIndicator);
        initSlipsTable();
        initOrdersTable();
        setupSearchFilter();
        setupSlipsSelection();
        loadSlipsAsync();
    }

    private void initSlipsTable() {
        colSlipNumber.setCellValueFactory(new PropertyValueFactory<>("slipNumber"));
        colSlipDate.setCellValueFactory(cell -> {
            var date = cell.getValue().getDispatchDate();
            return new SimpleStringProperty(date != null ? date.toString() : "-");
        });
        colSlipTotal.setCellValueFactory(cell -> {
            var total = cell.getValue().getTotalAmount();
            return new SimpleStringProperty(total != null ? String.format("%,.2f", total) : "0.00");
        });
    }

    private void initOrdersTable() {
        colOrderNum.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));
        colOrderDebtor.setCellValueFactory(cell -> {
            var debtor = cell.getValue().getDebtor();
            return new SimpleStringProperty(debtor != null ? debtor.getFullName() : "-");
        });
        colOrderAmount.setCellValueFactory(cell -> {
            var amt = cell.getValue().getAmount();
            return new SimpleStringProperty(amt != null ? String.format("%,.2f", amt) : "0.00");
        });
    }

    private void setupSearchFilter() {
        searchField.textProperty().addListener((obs, old, newVal) -> {
            FilteredList<DispatchSlip> filteredSlipsList = tableLoader.getFilteredList();
            if (filteredSlipsList == null) return;
            String search = newVal == null ? "" : newVal.toLowerCase();
            filteredSlipsList.setPredicate(slip -> {
                if (search.isEmpty()) return true;
                return slip.getSlipNumber().toLowerCase().contains(search);
            });
        });
    }

    private void setupSlipsSelection() {
        slipsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSlip, newSlip) -> {
            if (newSlip != null) {
                showSlipDetails(newSlip);
            } else {
                clearSlipDetails();
            }
        });
    }

    private void showSlipDetails(DispatchSlip slip) {
        detailTitleLabel.setText("تفاصيل البوردرو: " + slip.getSlipNumber());
        detailNumberField.setText(slip.getSlipNumber());
        detailDatePicker.setValue(slip.getDispatchDate());
        totalAmountLabel.setText(String.format("%,.2f", slip.getTotalAmount()));
        printBtn.setDisable(false);

        ordersTable.setItems(FXCollections.observableArrayList(slip.getOrders()));
    }

    private void clearSlipDetails() {
        detailTitleLabel.setText("تفاصيل البوردرو");
        detailNumberField.clear();
        detailDatePicker.setValue(null);
        totalAmountLabel.setText("0.00");
        printBtn.setDisable(true);
        ordersTable.setItems(FXCollections.observableArrayList());
    }

    private void loadSlipsAsync() {
        tableLoader.load(() -> {
            var fy = fyRepo.findActive();
            if (fy.isPresent()) {
                return slipRepo.findAll(fy.get().getId());
            }
            return java.util.List.<DispatchSlip>of();
        });
    }

    @FXML
    private void handleNewSlip() {
        if (!authService.canDo("dispatch.create")) {
            DialogHelper.showError("خطأ", "ليس لديك صلاحية إنشاء بوردرو إرسال جديد.");
            return;
        }
        Stage stage = (Stage) slipsTable.getScene().getWindow();
        FXMLLoader loader = SceneManager.openModal(
            stage, 
            "/org/marrok/amriirad/view/dispatch/dispatch-slip-form-view.fxml", 
            "إنشاء بوردرو إرسال جديد"
        );
        
        if (loader != null) {
            DispatchSlipFormController controller = loader.getController();
            controller.initData(this::loadSlipsAsync);
        }
    }

    @FXML
    private void handlePrint() {
        if (!authService.canDo("dispatch.print")) {
            DialogHelper.showError("خطأ", "ليس لديك صلاحية طباعة بوردروات الإرسال.");
            return;
        }
        DispatchSlip selected = slipsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            DialogHelper.showLanguageDialog(lang -> printSlip(selected, lang));
        }
    }

    private void printSlip(DispatchSlip selected, org.marrok.amriirad.model.PrintLanguage lang) {
        logger.info("Printing Annexe 5 for slip: {} in {}", selected.getSlipNumber(), lang);
        String reportPath = "/org/marrok/amriirad/report/annexe5_dispatch_" + lang.getCode() + ".jrxml";
        
        concurrencyManager.runAsync(
            () -> {
                String totalWords = "";
                if (selected.getTotalAmount() != null) {
                    totalWords = (lang == org.marrok.amriirad.model.PrintLanguage.FRENCH) ?
                        tafqeetService.toFrenchWords(selected.getTotalAmount()) :
                        tafqeetService.toArabicWords(selected.getTotalAmount());
                }

                java.util.Map<String, Object> params = org.marrok.amriirad.util.ReportParamBuilder.create(tafqeetService)
                    .withLanguage(lang)
                    .withInstitution(institutionService.getInfo())
                    .put("SLIP_NUMBER", selected.getSlipNumber() != null ? selected.getSlipNumber() : "")
                    .put("TOTAL_AMOUNT", selected.getTotalAmount() != null ? String.format("%,.2f", selected.getTotalAmount()) : "0.00")
                    .put("TOTAL_WORDS", totalWords)
                    .put("DATE", selected.getDispatchDate() != null ? selected.getDispatchDate().toString() : "")
                    .build();
                
                java.util.List<org.marrok.amriirad.dto.SlipOrderDTO> dataSourceList = new java.util.ArrayList<>();
                if (selected.getOrders() != null) {
                    for (RevenueOrder order : selected.getOrders()) {
                        dataSourceList.add(new org.marrok.amriirad.dto.SlipOrderDTO(
                            order.getOrderNumber(),
                            order.getDebtor() != null ? order.getDebtor().getFullName() : "",
                            order.getAmount(),
                            order.getIssueDate() != null ? order.getIssueDate().toString() : ""
                        ));
                    }
                }
                
                net.sf.jasperreports.engine.data.JRBeanCollectionDataSource dataSource = 
                    new net.sf.jasperreports.engine.data.JRBeanCollectionDataSource(dataSourceList);
                
                reportService.showReport(reportPath, params, dataSource);
                return true;
            },
            res -> logger.info("Print triggered for slip: {}", selected.getSlipNumber()),
            err -> DialogHelper.showError("خطأ", "فشل الطباعة: " + err.getMessage())
        );
    }
}

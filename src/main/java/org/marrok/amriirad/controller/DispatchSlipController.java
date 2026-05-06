package org.marrok.amriirad.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.core.ConcurrencyManager;
import org.marrok.amriirad.model.DispatchSlip;
import org.marrok.amriirad.model.RevenueOrder;
import org.marrok.amriirad.repository.DispatchSlipRepository;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

public class DispatchSlipController implements Initializable {

    private static final Logger logger = LogManager.getLogger(DispatchSlipController.class);

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

    private ObservableList<DispatchSlip> masterSlipsList;
    private FilteredList<DispatchSlip> filteredSlipsList;

    private final DispatchSlipRepository slipRepo = new DispatchSlipRepository();
    private final org.marrok.amriirad.repository.FiscalYearRepository fyRepo = new org.marrok.amriirad.repository.FiscalYearRepository();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
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
        loadingIndicator.setVisible(true);
        loadingIndicator.setManaged(true);

        ConcurrencyManager.getInstance().runAsync(
            () -> {
                var fy = fyRepo.findActive();
                if (fy.isPresent()) {
                    return slipRepo.findAll(fy.get().getId());
                }
                return java.util.List.<DispatchSlip>of();
            },
            slips -> {
                masterSlipsList = FXCollections.observableArrayList(slips);
                filteredSlipsList = new FilteredList<>(masterSlipsList, p -> true);
                SortedList<DispatchSlip> sortedList = new SortedList<>(filteredSlipsList);
                sortedList.comparatorProperty().bind(slipsTable.comparatorProperty());
                slipsTable.setItems(sortedList);
                
                loadingIndicator.setVisible(false);
                loadingIndicator.setManaged(false);
            },
            err -> {
                logger.error("Failed to load dispatch slips", err);
                loadingIndicator.setVisible(false);
                loadingIndicator.setManaged(false);
            }
        );
    }

    @FXML
    private void handleNewSlip() {
        // TODO: Navigate to slip creation form (selecting available ISSUED orders)
        logger.info("Opening new dispatch slip creation modal...");
    }

    @FXML
    private void handlePrint() {
        DispatchSlip selected = slipsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            logger.info("Printing Annexe 5 for slip: {}", selected.getSlipNumber());
            
            try {
                org.marrok.amriirad.service.ReportService reportService = new org.marrok.amriirad.service.ReportService();
                org.marrok.amriirad.service.TafqeetService tafqeetService = new org.marrok.amriirad.service.TafqeetService();
                
                java.util.Map<String, Object> params = new java.util.HashMap<>();
                params.put("SLIP_NUMBER", selected.getSlipNumber() != null ? selected.getSlipNumber() : "");
                params.put("TOTAL_AMOUNT", selected.getTotalAmount() != null ? selected.getTotalAmount().toString() : "0.00");
                params.put("TOTAL_WORDS", selected.getTotalAmount() != null ? tafqeetService.toArabicWords(selected.getTotalAmount()) : "");
                params.put("DATE", selected.getDispatchDate() != null ? selected.getDispatchDate().toString() : "");
                
                // Create a list of beans for the table in Annexe 5
                java.util.List<SlipOrderDTO> dataSourceList = new java.util.ArrayList<>();
                if (selected.getOrders() != null) {
                    for (RevenueOrder order : selected.getOrders()) {
                        dataSourceList.add(new SlipOrderDTO(
                            order.getOrderNumber(),
                            order.getDebtor() != null ? order.getDebtor().getFullName() : "",
                            order.getAmount()
                        ));
                    }
                }
                
                net.sf.jasperreports.engine.data.JRBeanCollectionDataSource dataSource = 
                    new net.sf.jasperreports.engine.data.JRBeanCollectionDataSource(dataSourceList);
                
                reportService.showReport("/org/marrok/amriirad/report/annexe5_dispatch.jrxml", params, dataSource);
            } catch (Exception e) {
                logger.error("Failed to print dispatch slip", e);
            }
        }
    }

    /**
     * DTO for JRBeanCollectionDataSource for Annexe 5 table.
     */
    public static class SlipOrderDTO {
        private String orderNumber;
        private String debtorName;
        private BigDecimal amount;

        public SlipOrderDTO(String orderNumber, String debtorName, BigDecimal amount) {
            this.orderNumber = orderNumber;
            this.debtorName = debtorName;
            this.amount = amount;
        }

        public String getOrderNumber() { return orderNumber; }
        public String getDebtorName() { return debtorName; }
        public BigDecimal getAmount() { return amount; }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/marrok/amriirad/view/dashboard-view.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/org/marrok/amriirad/css/app.css").toExternalForm());
            Stage stage = (Stage) slipsTable.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception ex) {
            logger.error("Failed to load dashboard", ex);
        }
    }
}

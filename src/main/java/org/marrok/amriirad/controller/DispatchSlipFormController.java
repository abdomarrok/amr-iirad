package org.marrok.amriirad.controller;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.core.ConcurrencyManager;
import org.marrok.amriirad.model.DispatchSlip;
import org.marrok.amriirad.model.OrderStatus;
import org.marrok.amriirad.model.RevenueOrder;
import org.marrok.amriirad.repository.FiscalYearRepository;
import org.marrok.amriirad.repository.RevenueOrderRepository;
import org.marrok.amriirad.service.DispatchSlipService;
import org.marrok.amriirad.service.ReportService;
import org.marrok.amriirad.service.TafqeetService;
import org.marrok.amriirad.util.GeneralUtil;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

public class DispatchSlipFormController implements javafx.fxml.Initializable {

    private static final Logger logger = LogManager.getLogger(DispatchSlipFormController.class);

    // Orders Table
    @FXML private TableView<OrderWrapper> ordersTable;
    @FXML private TableColumn<OrderWrapper, Boolean> colSelect;
    @FXML private TableColumn<OrderWrapper, String> colOrderNumber;
    @FXML private TableColumn<OrderWrapper, String> colDebtor;
    @FXML private TableColumn<OrderWrapper, String> colAmount;
    @FXML private TableColumn<OrderWrapper, String> colIssueDate;

    // Dispatch Slip Details
    @FXML private DatePicker dispatchDatePicker;
    @FXML private TextField treasuryRefField;
    @FXML private Label totalAmountLabel;
    @FXML private Label selectedCountLabel;

    // Misc
    @FXML private TextField searchField;
    @FXML private Label errorLabel;
    @FXML private Button saveBtn;
    @FXML private ProgressIndicator loadingIndicator;

    // Dependencies
    private final FiscalYearRepository fyRepo;
    private final RevenueOrderRepository orderRepo;
    private final DispatchSlipService slipService;
    private final ReportService reportService;
    private final TafqeetService tafqeetService;

    // Data
    private ObservableList<OrderWrapper> allOrders;
    private FilteredList<OrderWrapper> filteredOrders;
    private Runnable onSuccess;

    public DispatchSlipFormController(FiscalYearRepository fyRepo,
                                      RevenueOrderRepository orderRepo,
                                      DispatchSlipService slipService,
                                      ReportService reportService,
                                      TafqeetService tafqeetService) {
        this.fyRepo = fyRepo;
        this.orderRepo = orderRepo;
        this.slipService = slipService;
        this.reportService = reportService;
        this.tafqeetService = tafqeetService;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dispatchDatePicker.setValue(LocalDate.now());
        initOrdersTable();
        setupSearch();
        setupSelectionListener();
        loadIssuedOrdersAsync();
    }

    /**
     * Initialize table with selection checkboxes and order data.
     */
    private void initOrdersTable() {
        // Checkbox column
        colSelect.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        colSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colSelect));

        // Other columns
        colOrderNumber.setCellValueFactory(cellData -> new SimpleStringProperty(
            cellData.getValue().getOrder().getOrderNumber()));

        colDebtor.setCellValueFactory(cellData -> new SimpleStringProperty(
            cellData.getValue().getOrder().getDebtor() != null
                ? cellData.getValue().getOrder().getDebtor().getFullName()
                : "-"));

        colAmount.setCellValueFactory(cellData -> new SimpleStringProperty(
            cellData.getValue().getOrder().getAmount() != null
                ? String.format("%,.2f", cellData.getValue().getOrder().getAmount())
                : "0.00"));

        colIssueDate.setCellValueFactory(cellData -> new SimpleStringProperty(
            cellData.getValue().getOrder().getIssueDate() != null
                ? cellData.getValue().getOrder().getIssueDate().toString()
                : "-"));
    }

    /**
     * Setup search filter.
     */
    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldV, newV) -> updatePredicate());
    }

    /**
     * Update table predicate based on search text.
     */
    private void updatePredicate() {
        if (filteredOrders == null) return;
        
        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        
        filteredOrders.setPredicate(wrapper -> {
            RevenueOrder order = wrapper.getOrder();
            if (search.isEmpty()) return true;
            
            String orderNum = order.getOrderNumber() != null ? order.getOrderNumber().toLowerCase() : "";
            String debtorName = order.getDebtor() != null ? order.getDebtor().getFullName().toLowerCase() : "";
            
            return orderNum.contains(search) || debtorName.contains(search);
        });
    }

    /**
     * Listen to selection changes and update total amount.
     */
    private void setupSelectionListener() {
        // This will be called whenever any item's selection changes
        // We'll rely on the property change listener in the wrapper
    }

    /**
     * Load all ISSUED orders for the active fiscal year.
     */
    private void loadIssuedOrdersAsync() {
        loadingIndicator.setVisible(true);
        loadingIndicator.setManaged(true);
        errorLabel.setText("");

        ConcurrencyManager.getInstance().runAsync(
            () -> {
                var activeFy = fyRepo.findActive();
                if (activeFy.isEmpty()) {
                    throw new RuntimeException("لا توجد سنة مالية نشطة.");
                }
                int fyId = activeFy.get().getId();
                return orderRepo.findByStatus(fyId, OrderStatus.ISSUED);
            },
            orders -> {
                allOrders = FXCollections.observableArrayList();
                for (RevenueOrder order : orders) {
                    OrderWrapper wrapper = new OrderWrapper(order);
                    // Add listener to wrapper's selection property
                    wrapper.selectedProperty().addListener((obs, oldV, newV) -> updateTotalAmount());
                    allOrders.add(wrapper);
                }

                filteredOrders = new FilteredList<>(allOrders, p -> true);
                ordersTable.setItems(filteredOrders);

                loadingIndicator.setVisible(false);
                loadingIndicator.setManaged(false);

                if (orders.isEmpty()) {
                    errorLabel.setText("⚠️ لا توجد أوامر إيراد في حالة ISSUED.");
                }
            },
            err -> {
                logger.error("Failed to load issued orders", err);
                loadingIndicator.setVisible(false);
                loadingIndicator.setManaged(false);
                errorLabel.setText("❌ " + err.getMessage());
            }
        );
    }

    /**
     * Recalculate total amount based on selected orders.
     */
    private void updateTotalAmount() {
        BigDecimal total = BigDecimal.ZERO;
        int selectedCount = 0;

        for (OrderWrapper wrapper : allOrders) {
            if (wrapper.isSelected()) {
                selectedCount++;
                BigDecimal amt = wrapper.getOrder().getAmount();
                if (amt != null) {
                    total = total.add(amt);
                }
            }
        }

        totalAmountLabel.setText(String.format("%,.2f د.ج", total));
        selectedCountLabel.setText(String.format("تم تحديد %d أمر", selectedCount));
    }

    @FXML
    private void handleSelectAll() {
        for (OrderWrapper wrapper : filteredOrders) {
            wrapper.setSelected(true);
        }
    }

    @FXML
    private void handleDeselectAll() {
        for (OrderWrapper wrapper : filteredOrders) {
            wrapper.setSelected(false);
        }
    }

    @FXML
    private void handleSave() throws SQLException {
        if (!validateForm()) {
            return;
        }

        List<RevenueOrder> selectedOrders = new ArrayList<>();
        for (OrderWrapper wrapper : allOrders) {
            if (wrapper.isSelected()) {
                selectedOrders.add(wrapper.getOrder());
            }
        }

        // Create dispatch slip
        DispatchSlip slip = new DispatchSlip();
        slip.setDispatchDate(dispatchDatePicker.getValue());
        slip.setTreasuryRef(treasuryRefField.getText().trim());
        slip.setOrders(selectedOrders);
        slip.setCreatedBy("admin"); // TODO: Use real user
        slip.recalculateTotal();

        // Get active fiscal year
        var activeFy = fyRepo.findActive();
        if (activeFy.isEmpty()) {
            errorLabel.setText("❌ لا توجد سنة مالية نشطة.");
            return;
        }
        slip.setFiscalYear(activeFy.get());

        // Save asynchronously
        ConcurrencyManager.getInstance().runAsync(
            () -> {
                try {
                    slipService.createSlip(slip);
                } catch (java.sql.SQLException e) {
                    throw new RuntimeException("خطأ في حفظ البوردرو: " + e.getMessage(), e);
                }
                return slip;
            },
            createdSlip -> {
                printAnnexe5(createdSlip);
                closeWindow();
                if (onSuccess != null) {
                    onSuccess.run();
                }
            },
            err -> {
                logger.error("Failed to create dispatch slip", err);
                errorLabel.setText("❌ " + err.getMessage());
            }
        );
    }

    /**
     * Print Annexe 5 (dispatch slip report).
     */
    private void printAnnexe5(DispatchSlip slip) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("SLIP_NUMBER", slip.getSlipNumber() != null ? slip.getSlipNumber() : "");
            params.put("DISPATCH_DATE", slip.getDispatchDate() != null ? slip.getDispatchDate().toString() : "");
            params.put("TREASURY_REF", slip.getTreasuryRef() != null ? slip.getTreasuryRef() : "");
            params.put("TOTAL_AMOUNT", slip.getTotalAmount() != null ? slip.getTotalAmount().toString() : "0");
            params.put("TOTAL_AMOUNT_WORDS", slip.getTotalAmount() != null 
                ? tafqeetService.toArabicWords(slip.getTotalAmount()) 
                : "صفر");

            // Create a data source from the orders
            List<Map<String, Object>> ordersList = new ArrayList<>();
            for (RevenueOrder order : slip.getOrders()) {
                Map<String, Object> orderData = new HashMap<>();
                orderData.put("ORDER_NUMBER", order.getOrderNumber());
                orderData.put("DEBTOR_NAME", order.getDebtor() != null ? order.getDebtor().getFullName() : "");
                orderData.put("AMOUNT", order.getAmount() != null ? String.format("%,.2f", order.getAmount()) : "0.00");
                ordersList.add(orderData);
            }

            // Use bean collection data source
            net.sf.jasperreports.engine.data.JRBeanCollectionDataSource dataSource =
                new net.sf.jasperreports.engine.data.JRBeanCollectionDataSource(ordersList);

            reportService.showReport(
                "/org/marrok/amriirad/report/annexe5_dispatch.jrxml",
                params,
                dataSource
            );
        } catch (Exception e) {
            logger.error("Failed to print Annexe 5", e);
            GeneralUtil.showAlert(Alert.AlertType.ERROR, "خطأ في الطباعة",
                "تعذر طباعة الملحق 5: " + e.getMessage());
        }
    }

    private boolean validateForm() {
        errorLabel.setText("");

        // Check if at least one order is selected
        int selectedCount = (int) allOrders.stream().filter(OrderWrapper::isSelected).count();
        if (selectedCount == 0) {
            errorLabel.setText("❌ يجب تحديد أمر إيراد واحد على الأقل.");
            return false;
        }

        // Check if dispatch date is set
        if (dispatchDatePicker.getValue() == null) {
            errorLabel.setText("❌ يجب تحديد تاريخ الإرسال.");
            return false;
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

    /**
     * Initialize the form with a success callback.
     */
    public void initData(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    /**
     * Wrapper class to add a selection property to RevenueOrder.
     */
    public static class OrderWrapper {
        private final RevenueOrder order;
        private final SimpleBooleanProperty selected = new SimpleBooleanProperty(false);

        public OrderWrapper(RevenueOrder order) {
            this.order = order;
        }

        public RevenueOrder getOrder() {
            return order;
        }

        public boolean isSelected() {
            return selected.get();
        }

        public void setSelected(boolean value) {
            selected.set(value);
        }

        public SimpleBooleanProperty selectedProperty() {
            return selected;
        }
    }
}

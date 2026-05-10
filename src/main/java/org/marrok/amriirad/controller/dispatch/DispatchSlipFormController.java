package org.marrok.amriirad.controller.dispatch;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.Stage; // Unused import, will be removed by IDE
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.controller.BaseFormController;
import org.marrok.amriirad.core.ConcurrencyManager; // Unused import, will be removed by IDE
import org.marrok.amriirad.model.DispatchSlip;
import org.marrok.amriirad.model.OrderStatus;
import org.marrok.amriirad.model.RevenueOrder;
import org.marrok.amriirad.repository.FiscalYearRepository;
import org.marrok.amriirad.repository.RevenueOrderRepository;
import org.marrok.amriirad.service.DispatchSlipService;
import org.marrok.amriirad.service.ReportService;
import org.marrok.amriirad.service.TafqeetService;
import org.marrok.amriirad.util.DialogHelper;
import org.marrok.amriirad.util.ReportParamBuilder; // Unused import, will be removed by IDE

import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class DispatchSlipFormController extends BaseFormController implements javafx.fxml.Initializable {

    private static final Logger logger = LogManager.getLogger(DispatchSlipFormController.class);

    // Orders Table
    @FXML private TableView<OrderWrapper> ordersTable;
    @FXML private TableColumn<OrderWrapper, Boolean> colSelect;
    @FXML private TableColumn<OrderWrapper, String> colOrderNumber;
    @FXML private TableColumn<OrderWrapper, String> colDebtor;
    @FXML private TableColumn<OrderWrapper, String> colAmount;
    @FXML private TableColumn<OrderWrapper, String> colIssueDate;
    @FXML private CheckBox selectAllCheckBox; // Added for header checkbox

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
    private final org.marrok.amriirad.service.AuthService authService;

    // Data
    private ObservableList<OrderWrapper> allOrders;
    private FilteredList<OrderWrapper> filteredOrders;
    private Runnable onSuccess;
    private int lastSelectedIndex = -1; // For Shift+Click

    public DispatchSlipFormController(FiscalYearRepository fyRepo,
                                      RevenueOrderRepository orderRepo,
                                      DispatchSlipService slipService,
                                      ReportService reportService,
                                      TafqeetService tafqeetService,
                                      org.marrok.amriirad.service.AuthService authService,
                                      org.marrok.amriirad.core.ConcurrencyManager concurrencyManager) {
        super(concurrencyManager);
        this.fyRepo = fyRepo;
        this.orderRepo = orderRepo;
        this.slipService = slipService;
        this.reportService = reportService;
        this.tafqeetService = tafqeetService;
        this.authService = authService;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dispatchDatePicker.setValue(LocalDate.now());
        initOrdersTable();
        setupSearch();
        // setupSelectionListener() is removed as its logic is now integrated
        loadIssuedOrdersAsync();
    }

    /**
     * Initialize table with selection checkboxes and order data.
     */
    private void initOrdersTable() {
        // Checkbox column
        colSelect.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        // Use the simple forTableColumn() as setCellValueFactory already provides the ObservableValue<Boolean>
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

        // Handle header checkbox action
        selectAllCheckBox.setOnAction(event -> {
            boolean select = selectAllCheckBox.isSelected();
            filteredOrders.forEach(wrapper -> wrapper.setSelected(select));
            lastSelectedIndex = -1; // Reset last selected index after bulk action
            updateTotalAmountAndHeaderCheckbox(); // Update UI after bulk selection
        });

        // Add mouse click listener for Shift+Click and Ctrl+Click on the table rows
        ordersTable.setRowFactory(tv -> {
            TableRow<OrderWrapper> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem != null) {
                    // Listener for the individual OrderWrapper's selected property
                    // This ensures the row style is updated when the checkbox is clicked
                    newItem.selectedProperty().addListener((sObs, sOldVal, sNewVal) -> {
                        if (sNewVal) {
                            row.getStyleClass().add("selected-row");
                        } else {
                            row.getStyleClass().remove("selected-row");
                        }
                    });
                    // Set initial style
                    if (newItem.isSelected()) {
                        row.getStyleClass().add("selected-row");
                    } else {
                        row.getStyleClass().remove("selected-row");
                    }
                } else {
                    row.getStyleClass().remove("selected-row");
                }
            });

            row.setOnMouseClicked(event -> {
                OrderWrapper clickedItem = row.getItem();
                if (clickedItem == null || row.isEmpty()) {
                    return;
                }

                // If the click is on the checkbox itself, let the CheckBoxTableCell handle it
                // This check is a bit tricky as the event target might be the cell or the checkbox graphic
                // For simplicity, we'll assume if the row is clicked, we handle selection.
                // The CheckBoxTableCell will also trigger the selectedProperty listener.

                int clickedIndex = ordersTable.getItems().indexOf(clickedItem);

                if (event.isShiftDown() && lastSelectedIndex != -1 && filteredOrders.size() > 0) {
                    // Shift+Click for range selection
                    int start = Math.min(clickedIndex, lastSelectedIndex);
                    int end = Math.max(clickedIndex, lastSelectedIndex);

                    // Determine the selection state based on the first item in the range or the clicked item
                    boolean targetSelectionState = clickedItem.isSelected();
                    if (lastSelectedIndex >= 0 && lastSelectedIndex < filteredOrders.size()) {
                        targetSelectionState = filteredOrders.get(lastSelectedIndex).isSelected();
                    }


                    for (int i = start; i <= end; i++) {
                        if (i >= 0 && i < filteredOrders.size()) {
                            filteredOrders.get(i).setSelected(targetSelectionState);
                        }
                    }
                } else if (event.isControlDown()) {
                    // Ctrl+Click for toggling individual selection
                    clickedItem.setSelected(!clickedItem.isSelected());
                } else {
                    // Regular click: deselect all others and select only this one
                    boolean wasSelected = clickedItem.isSelected();
                    filteredOrders.forEach(wrapper -> wrapper.setSelected(false));
                    clickedItem.setSelected(!wasSelected); // Toggle the clicked item
                }
                lastSelectedIndex = clickedIndex;
                // No need to clear table's internal selection model if we manage selection via OrderWrapper
                updateTotalAmountAndHeaderCheckbox();
            });
            return row;
        });
    }

    /**
     * Setup search filter.
     */
    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            updatePredicate();
            lastSelectedIndex = -1; // Reset last selected index on search
        });
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
        updateTotalAmountAndHeaderCheckbox(); // Update counts and header checkbox after filter changes
    }

    /**
     * Load all ISSUED orders for the active fiscal year.
     */
    private void loadIssuedOrdersAsync() {
        loadingIndicator.setVisible(true);
        loadingIndicator.setManaged(true);
        errorLabel.setText("");

        concurrencyManager.runAsync(
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
                        // This listener will trigger updateTotalAmountAndHeaderCheckbox()
                        wrapper.selectedProperty().addListener((obs, oldV, newV) -> updateTotalAmountAndHeaderCheckbox());
                        allOrders.add(wrapper);
                    }

                    filteredOrders = new FilteredList<>(allOrders, p -> true);
                    ordersTable.setItems(filteredOrders);

                    loadingIndicator.setVisible(false);
                    loadingIndicator.setManaged(false);

                    if (orders.isEmpty()) {
                        errorLabel.setText("⚠️ لا توجد أوامر إيراد في حالة ISSUED.");
                    }
                    updateTotalAmountAndHeaderCheckbox(); // Initial update
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
     * Recalculate total amount based on selected orders and update header checkbox.
     */
    private void updateTotalAmountAndHeaderCheckbox() {
        BigDecimal total = BigDecimal.ZERO;
        long selectedCount = filteredOrders.stream().filter(OrderWrapper::isSelected).count();
        long totalFilteredCount = filteredOrders.size();

        for (OrderWrapper wrapper : filteredOrders) {
            if (wrapper.isSelected()) {
                BigDecimal amt = wrapper.getOrder().getAmount();
                if (amt != null) {
                    total = total.add(amt);
                }
            }
        }

        totalAmountLabel.setText(String.format("%,.2f د.ج", total));
        selectedCountLabel.setText(String.format("تم تحديد %d أمر", selectedCount));

        // Update header checkbox state
        if (totalFilteredCount == 0) {
            selectAllCheckBox.setIndeterminate(false);
            selectAllCheckBox.setSelected(false);
        } else if (selectedCount == 0) {
            selectAllCheckBox.setIndeterminate(false);
            selectAllCheckBox.setSelected(false);
        } else if (selectedCount == totalFilteredCount) {
            selectAllCheckBox.setIndeterminate(false);
            selectAllCheckBox.setSelected(true);
        } else {
            selectAllCheckBox.setIndeterminate(true);
            selectAllCheckBox.setSelected(false);
        }
        ordersTable.refresh(); // Refresh table to update row styles
    }

    @FXML
    private void handleSave() throws SQLException {
        if (!authService.canDo("dispatch.create")) {
            DialogHelper.showError("خطأ", "ليس لديك صلاحية إنشاء بوردرو إرسال.");
            return;
        }
        if (!validateForm()) {
            return;
        }

        List<RevenueOrder> selectedOrders = allOrders.stream()
                .filter(OrderWrapper::isSelected)
                .map(OrderWrapper::getOrder)
                .collect(Collectors.toList());

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
        concurrencyManager.runAsync(
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
                    runOnSuccess();
                },
                err -> showError(err.getMessage())
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
            showError("خطأ في الطباعة: " + e.getMessage());
        }
    }

    @Override
    protected boolean validateForm() {
        clearError();

        // Check if at least one order is selected
        long selectedCount = allOrders.stream().filter(OrderWrapper::isSelected).count();
        if (selectedCount == 0) {
            showError("يجب تحديد أمر إيراد واحد على الأقل.");
            return false;
        }

        // Check if dispatch date is set
        if (dispatchDatePicker.getValue() == null) {
            showError("يجب تحديد تاريخ الإرسال.");
            return false;
        }

        return true;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

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
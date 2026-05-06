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
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.core.ConcurrencyManager;
import org.marrok.amriirad.model.FiscalYear;
import org.marrok.amriirad.model.OrderStatus;
import org.marrok.amriirad.model.RevenueOrder;
import org.marrok.amriirad.repository.FiscalYearRepository;
import org.marrok.amriirad.repository.RevenueOrderRepository;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class RevenueOrderListController implements Initializable {

    private static final Logger logger = LogManager.getLogger(RevenueOrderListController.class);

    @FXML private TableView<RevenueOrder> tableView;
    @FXML private TableColumn<RevenueOrder, String> colOrderNumber;
    @FXML private TableColumn<RevenueOrder, String> colIssueDate;
    @FXML private TableColumn<RevenueOrder, String> colDebtor;
    @FXML private TableColumn<RevenueOrder, String> colBudgetChapter;
    @FXML private TableColumn<RevenueOrder, String> colAmount;
    @FXML private TableColumn<RevenueOrder, String> colStatus;

    @FXML private TextField searchField;
    @FXML private ComboBox<OrderStatus> statusFilterCombo;
    @FXML private ProgressIndicator loadingIndicator;

    private ObservableList<RevenueOrder> masterList;
    private FilteredList<RevenueOrder> filteredList;

    private final RevenueOrderRepository orderRepo = new RevenueOrderRepository();
    private final FiscalYearRepository fyRepo = new FiscalYearRepository();

    private FiscalYear activeYear;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initColumns();
        setupFilters();
        setupTableInteraction();
        loadDataAsync();
    }

    private void initColumns() {
        colOrderNumber.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));
        
        colIssueDate.setCellValueFactory(cell -> {
            var date = cell.getValue().getIssueDate();
            return new SimpleStringProperty(date != null ? date.toString() : "-");
        });
        
        colDebtor.setCellValueFactory(cell -> {
            var debtor = cell.getValue().getDebtor();
            return new SimpleStringProperty(debtor != null ? debtor.getFullName() : "-");
        });
        
        colBudgetChapter.setCellValueFactory(cell -> {
            var bc = cell.getValue().getBudgetChapter();
            return new SimpleStringProperty(bc != null ? bc.getCode() : "-");
        });
        
        colAmount.setCellValueFactory(cell -> {
            var amt = cell.getValue().getAmount();
            return new SimpleStringProperty(amt != null ? String.format("%,.2f", amt) : "0.00");
        });
        
        colStatus.setCellValueFactory(cell -> {
            var status = cell.getValue().getStatus();
            String arStatus = "-";
            switch(status) {
                case DRAFT: arStatus = "مسودة"; break;
                case ISSUED: arStatus = "مُصدر"; break;
                case DISPATCHED: arStatus = "مُرسل"; break;
                case CANCELLED: arStatus = "مُلغى"; break;
                case REDUCED: arStatus = "مُخفض"; break;
            }
            return new SimpleStringProperty(arStatus);
        });

        // Basic styling for status column cells could be added here via CellFactory
    }

    private void setupFilters() {
        statusFilterCombo.getItems().addAll(OrderStatus.values());
        statusFilterCombo.getItems().add(0, null); // "All" option

        // Bind filter to input changes
        searchField.textProperty().addListener((obs, oldV, newV) -> updatePredicate());
        statusFilterCombo.valueProperty().addListener((obs, oldV, newV) -> updatePredicate());
    }

    private void updatePredicate() {
        if (filteredList == null) return;
        
        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        OrderStatus status = statusFilterCombo.getValue();

        filteredList.setPredicate(order -> {
            // Check status filter
            if (status != null && order.getStatus() != status) {
                return false;
            }
            // Check text search
            if (search.isEmpty()) return true;

            String orderNum = order.getOrderNumber() != null ? order.getOrderNumber().toLowerCase() : "";
            String debtorName = order.getDebtor() != null ? order.getDebtor().getFullName().toLowerCase() : "";
            String objText = order.getObjectAr() != null ? order.getObjectAr().toLowerCase() : "";

            return orderNum.contains(search) || debtorName.contains(search) || objText.contains(search);
        });
    }

    private void setupTableInteraction() {
        tableView.setOnMouseClicked((MouseEvent event) -> {
            if (event.getClickCount() == 2 && tableView.getSelectionModel().getSelectedItem() != null) {
                RevenueOrder selected = tableView.getSelectionModel().getSelectedItem();
                openFormModal(selected);
            }
        });
    }

    private void loadDataAsync() {
        loadingIndicator.setVisible(true);
        loadingIndicator.setManaged(true);
        
        ConcurrencyManager.getInstance().runAsync(
            () -> {
                Optional<FiscalYear> activeFy = fyRepo.findActive();
                if (activeFy.isPresent()) {
                    activeYear = activeFy.get();
                    return orderRepo.findAll(activeYear.getId());
                }
                return List.<RevenueOrder>of(); 
            },
            orders -> {
                masterList = FXCollections.observableArrayList(orders);
                filteredList = new FilteredList<>(masterList, p -> true);
                SortedList<RevenueOrder> sortedList = new SortedList<>(filteredList);
                sortedList.comparatorProperty().bind(tableView.comparatorProperty());
                tableView.setItems(sortedList);
                
                loadingIndicator.setVisible(false);
                loadingIndicator.setManaged(false);
            },
            err -> {
                logger.error("Failed to load revenue orders", err);
                loadingIndicator.setVisible(false);
                loadingIndicator.setManaged(false);
            }
        );
    }

    @FXML
    private void handleNewOrder() {
        openFormModal(null);
    }

    private void openFormModal(RevenueOrder order) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/marrok/amriirad/view/order-form-view.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/org/marrok/amriirad/css/app.css").toExternalForm());
            
            RevenueOrderFormController controller = loader.getController();
            if (order == null) {
                controller.initForCreate(this::loadDataAsync);
            } else {
                controller.initForEdit(order, this::loadDataAsync);
            }

            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle(order == null ? "إنشاء أمر إيراد" : "تعديل أمر إيراد");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initOwner(tableView.getScene().getWindow());
            stage.setResizable(false);
            stage.show();
        } catch (Exception e) {
            logger.error("Failed to open order form modal", e);
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/marrok/amriirad/view/dashboard-view.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/org/marrok/amriirad/css/app.css").toExternalForm());
            Stage stage = (Stage) tableView.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception ex) {
            logger.error("Failed to load dashboard", ex);
        }
    }
}

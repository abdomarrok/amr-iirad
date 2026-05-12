package org.marrok.amriirad.controller.dashboard;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.marrok.amriirad.controller.orders.RevenueOrderFormController;
import org.marrok.amriirad.controller.shared.TopBarController;
import org.marrok.amriirad.controller.shared.FooterController;
import org.marrok.amriirad.util.DialogHelper;
import org.marrok.amriirad.util.SceneManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.model.FiscalYear;
import org.marrok.amriirad.model.OrderStatus;
import org.marrok.amriirad.model.RevenueOrder;
import org.marrok.amriirad.repository.FiscalYearRepository;
import org.marrok.amriirad.repository.RevenueOrderRepository;

import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Simplified Dashboard Controller focusing on stats and recent activity.
 */
public class DashboardController implements Initializable {

    private static final Logger logger = LogManager.getLogger(DashboardController.class);

    @FXML private TopBarController topBarController;
    @FXML private FooterController footerController;

    @FXML private Label totalOrdersLabel;
    @FXML private Label issuedOrdersLabel;
    @FXML private Label dispatchedOrdersLabel;
    @FXML private Label totalAmountLabel;

    @FXML private TableView<RevenueOrder> recentOrdersTable;
    @FXML private TableColumn<RevenueOrder, String> colRecentNumber;
    @FXML private TableColumn<RevenueOrder, String> colRecentDebtor;
    @FXML private TableColumn<RevenueOrder, BigDecimal> colRecentAmount;

    @FXML private javafx.scene.control.Button newOrderBtn;
    @FXML private javafx.scene.control.Button orderListBtn;
    @FXML private javafx.scene.control.Button debtorsBtn;
    @FXML private javafx.scene.control.Button dispatchBtn;
    @FXML private javafx.scene.control.Button budgetChaptersBtn;

    private final FiscalYearRepository fyRepo;
    private final RevenueOrderRepository orderRepo;
    private final org.marrok.amriirad.service.AuthService authService;

    public DashboardController(FiscalYearRepository fyRepo, 
                               RevenueOrderRepository orderRepo,
                               org.marrok.amriirad.service.AuthService authService) {
        this.fyRepo = fyRepo;
        this.orderRepo = orderRepo;
        this.authService = authService;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        topBarController.setBackVisible(false);
        setupRecentTable();

        topBarController.getFiscalYearCombo().getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                refreshStats();
            }
        });

        checkPermissions();
        refreshStats();
    }

    private void setupRecentTable() {
        if (recentOrdersTable == null) return;
        colRecentNumber.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));
        colRecentDebtor.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                cell.getValue().getDebtor() != null ? cell.getValue().getDebtor().getFullName() : "---"));
        colRecentAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
    }

    private void checkPermissions() {
        setBtnVisible(newOrderBtn, authService.canView("orders"));
        setBtnVisible(orderListBtn, authService.canView("orders"));
        setBtnVisible(debtorsBtn, authService.canView("debtors"));
        setBtnVisible(dispatchBtn, authService.canView("orders"));
        setBtnVisible(budgetChaptersBtn, authService.canDo("budget_chapter.manage"));
    }

    private void setBtnVisible(javafx.scene.control.Button btn, boolean visible) {
        if (btn != null) {
            btn.setVisible(visible);
            btn.setManaged(visible);
        }
    }

    private void refreshStats() {
        FiscalYear selected = topBarController.getFiscalYearCombo().getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            List<RevenueOrder> allOrders = orderRepo.findAll(selected.getId());

            // 1. Scalar Stats
            long totalCount = allOrders.size();
            long issuedCount = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.ISSUED).count();
            long dispatchedCount = allOrders.stream().filter(o -> o.getStatus() == OrderStatus.DISPATCHED).count();
            BigDecimal totalAmount = allOrders.stream()
                    .map(RevenueOrder::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalOrdersLabel.setText(String.valueOf(totalCount));
            issuedOrdersLabel.setText(String.valueOf(issuedCount));
            dispatchedOrdersLabel.setText(String.valueOf(dispatchedCount));
            totalAmountLabel.setText(String.format("%,.2f", totalAmount));

            // 2. Recent Orders Table
            if (recentOrdersTable != null) {
                List<RevenueOrder> recent = allOrders.stream()
                        .limit(8)
                        .collect(Collectors.toList());
                recentOrdersTable.setItems(FXCollections.observableArrayList(recent));
            }
            
            footerController.setStatus("تم تحديث البيانات — " + selected.getYearLabel());

        } catch (Exception e) {
            logger.error("Failed to refresh dashboard stats", e);
            footerController.setStatus("❌ خطأ في تحميل البيانات");
        }
    }

    @FXML
    private void handleNewOrder() {
        if (!authService.canDo("revenue_order.create")) {
            DialogHelper.showError("خطأ", "ليس لديك صلاحية إنشاء أمر إيراد جديد.");
            return;
        }
        Stage stage = (Stage) totalOrdersLabel.getScene().getWindow();
        FXMLLoader loader = SceneManager.openModal(stage, "/org/marrok/amriirad/view/orders/order-form-view.fxml", "إنشاء أمر إيراد جديد");
        if (loader != null) {
            RevenueOrderFormController controller = loader.getController();
            controller.initForCreate(this::refreshStats);
        }
    }

    @FXML
    private void handleViewOrders() {
        Stage stage = (Stage) totalOrdersLabel.getScene().getWindow();
        SceneManager.loadScene(stage, "/org/marrok/amriirad/view/orders/order-list-view.fxml");
    }

    @FXML
    private void handleViewDebtors() {
        Stage stage = (Stage) totalOrdersLabel.getScene().getWindow();
        SceneManager.loadScene(stage, "/org/marrok/amriirad/view/debtors/debtor-list-view.fxml");
    }

    @FXML
    private void handleViewSlips() {
        Stage stage = (Stage) totalOrdersLabel.getScene().getWindow();
        SceneManager.loadScene(stage, "/org/marrok/amriirad/view/dispatch/dispatch-slip-view.fxml");
    }

    @FXML
    private void handleViewChapters() {
        Stage stage = (Stage) totalOrdersLabel.getScene().getWindow();
        SceneManager.loadScene(stage, "/org/marrok/amriirad/view/budget/budget-chapter-list-view.fxml");
    }
}

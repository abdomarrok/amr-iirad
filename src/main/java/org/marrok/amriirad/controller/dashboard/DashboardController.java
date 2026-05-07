package org.marrok.amriirad.controller.dashboard;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
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
import java.util.ResourceBundle;

/**
 * Main dashboard controller.
 * Displays summary stats for the active fiscal year and provides quick-action navigation.
 */
public class DashboardController implements Initializable {

    private static final Logger logger = LogManager.getLogger(DashboardController.class);

    @FXML private org.marrok.amriirad.controller.shared.TopBarController topBarController;
    @FXML private org.marrok.amriirad.controller.shared.FooterController footerController;

    @FXML private Label totalOrdersLabel;
    @FXML private Label issuedOrdersLabel;
    @FXML private Label dispatchedOrdersLabel;
    @FXML private Label totalAmountLabel;

    @FXML private javafx.scene.control.Button newOrderBtn;
    @FXML private javafx.scene.control.Button orderListBtn;
    @FXML private javafx.scene.control.Button debtorsBtn;
    @FXML private javafx.scene.control.Button dispatchBtn;

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
        topBarController.setBackVisible(false); // No back on dashboard
        
        // Listen to fiscal year changes from the centralized top bar
        topBarController.getFiscalYearCombo().getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                refreshStats();
            }
        });

        checkPermissions();
        refreshStats();
    }

    private void checkPermissions() {
        org.marrok.amriirad.service.AuthService auth = org.marrok.amriirad.core.AppContext.getInstance().getAuthService();
        
        // Local action buttons
        setBtnVisible(newOrderBtn, auth.canView("orders"));
        setBtnVisible(orderListBtn, auth.canView("orders"));
        setBtnVisible(debtorsBtn, auth.canView("debtors"));
        setBtnVisible(dispatchBtn, auth.canView("orders"));
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

            long totalCount = allOrders.size();
            long issuedCount = allOrders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.ISSUED).count();
            long dispatchedCount = allOrders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.DISPATCHED).count();
            BigDecimal totalAmount = allOrders.stream()
                    .map(RevenueOrder::getAmount)
                    .filter(a -> a != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalOrdersLabel.setText(String.valueOf(totalCount));
            issuedOrdersLabel.setText(String.valueOf(issuedCount));
            dispatchedOrdersLabel.setText(String.valueOf(dispatchedCount));
            totalAmountLabel.setText(String.format("%,.2f", totalAmount));
            
            footerController.setStatus("تم تحديث البيانات — " + selected.getYearLabel());

        } catch (Exception e) {
            logger.error("Failed to refresh stats for year {}", selected.getYearLabel(), e);
            footerController.setStatus("❌ خطأ في تحميل الإحصائيات");
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
}

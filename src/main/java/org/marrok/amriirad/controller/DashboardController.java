package org.marrok.amriirad.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
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

    @FXML private ComboBox<FiscalYear> fiscalYearCombo;
    @FXML private Label totalOrdersLabel;
    @FXML private Label issuedOrdersLabel;
    @FXML private Label dispatchedOrdersLabel;
    @FXML private Label totalAmountLabel;
    @FXML private Label statusBar;

    private final FiscalYearRepository fyRepo = new FiscalYearRepository();
    private final RevenueOrderRepository orderRepo = new RevenueOrderRepository();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadFiscalYears();
        fiscalYearCombo.setOnAction(e -> refreshStats());
    }

    private void loadFiscalYears() {
        try {
            List<FiscalYear> years = fyRepo.findAll();
            fiscalYearCombo.setItems(FXCollections.observableArrayList(years));

            // Select the active year by default
            fyRepo.findActive().ifPresent(active -> {
                fiscalYearCombo.getSelectionModel().select(active);
                refreshStats();
            });

            if (fiscalYearCombo.getSelectionModel().isEmpty() && !years.isEmpty()) {
                fiscalYearCombo.getSelectionModel().selectFirst();
                refreshStats();
            }
        } catch (Exception e) {
            logger.error("Failed to load fiscal years", e);
            statusBar.setText("❌ خطأ في تحميل السنوات المالية");
        }
    }

    private void refreshStats() {
        FiscalYear selected = fiscalYearCombo.getSelectionModel().getSelectedItem();
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
            statusBar.setText("تم تحديث البيانات — " + selected.getYearLabel());

        } catch (Exception e) {
            logger.error("Failed to refresh stats for year {}", selected.getYearLabel(), e);
            statusBar.setText("❌ خطأ في تحميل الإحصائيات");
        }
    }

    // ===================== ACTION HANDLERS =====================

    @FXML
    private void handleAddFiscalYear() {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(String.valueOf(java.time.LocalDate.now().getYear()));
        dialog.setTitle("إضافة سنة مالية");
        dialog.setHeaderText("إضافة سنة مالية جديدة");
        dialog.setContentText("أدخل السنة (مثال: 2024):");

        dialog.showAndWait().ifPresent(yearStr -> {
            try {
                fyRepo.save(yearStr.trim());
                loadFiscalYears();
                statusBar.setText("✅ تم إضافة السنة المالية " + yearStr);
            } catch (Exception e) {
                logger.error("Failed to save fiscal year", e);
                statusBar.setText("❌ فشل الحفظ: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleNewOrder() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/marrok/amriirad/view/order-form-view.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/org/marrok/amriirad/css/app.css").toExternalForm());
            
            RevenueOrderFormController controller = loader.getController();
            controller.initForCreate(() -> refreshStats());

            javafx.stage.Stage stage = (javafx.stage.Stage) totalOrdersLabel.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception ex) {
            logger.error("Failed to load order form view", ex);
            statusBar.setText("❌ خطأ في فتح نموذج الطلب");
        }
    }

    @FXML
    private void handleOrderList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/marrok/amriirad/view/order-list-view.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/org/marrok/amriirad/css/app.css").toExternalForm());
            javafx.stage.Stage stage = (javafx.stage.Stage) totalOrdersLabel.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception ex) {
            logger.error("Failed to load order list view", ex);
            statusBar.setText("❌ خطأ في فتح القائمة");
        }
    }

    @FXML
    private void handleDebtors() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/marrok/amriirad/view/debtor-list-view.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/org/marrok/amriirad/css/app.css").toExternalForm());
            javafx.stage.Stage stage = (javafx.stage.Stage) totalOrdersLabel.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception ex) {
            logger.error("Failed to load debtor list view", ex);
            statusBar.setText("❌ خطأ في فتح إدارة المدينين");
        }
    }

    @FXML
    private void handleDispatch() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/marrok/amriirad/view/dispatch-slip-view.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/org/marrok/amriirad/css/app.css").toExternalForm());
            javafx.stage.Stage stage = (javafx.stage.Stage) totalOrdersLabel.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception ex) {
            logger.error("Failed to load dispatch slip view", ex);
            statusBar.setText("❌ خطأ في فتح بوردرو الإرسال");
        }
    }
}

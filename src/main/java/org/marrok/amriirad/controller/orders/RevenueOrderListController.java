package org.marrok.amriirad.controller.orders;

import org.marrok.amriirad.controller.shared.TopBarController;
import org.marrok.amriirad.controller.shared.FooterController;

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
import org.marrok.amriirad.service.RevenueOrderService;
import org.marrok.amriirad.service.ExportService;
import org.marrok.amriirad.util.SceneManager;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import org.marrok.amriirad.ui.AsyncTableLoader;

public class RevenueOrderListController implements Initializable {

    private static final Logger logger = LogManager.getLogger(RevenueOrderListController.class);

    @FXML private org.marrok.amriirad.controller.shared.TopBarController topBarController;
    @FXML private org.marrok.amriirad.controller.shared.FooterController footerController;

    @FXML private TableView<RevenueOrder> tableView;
    @FXML private TableColumn<RevenueOrder, String> colOrderNumber;
    @FXML private TableColumn<RevenueOrder, String> colIssueDate;
    @FXML private TableColumn<RevenueOrder, String> colDebtor;
    @FXML private TableColumn<RevenueOrder, String> colBudgetChapter;
    @FXML private TableColumn<RevenueOrder, String> colAmount;
    @FXML private TableColumn<RevenueOrder, String> colStatus;

    @FXML private org.marrok.amriirad.controller.shared.components.FilterBarController filterBarController;
    @FXML private org.marrok.amriirad.controller.shared.components.ActionToolbarController actionToolbarController;
    @FXML private org.marrok.amriirad.controller.shared.components.EmptyStateController emptyStateController;
    @FXML private ProgressIndicator loadingIndicator;

    private ComboBox<OrderStatus> statusFilterCombo;

    private AsyncTableLoader<RevenueOrder> tableLoader;

    private final RevenueOrderService orderService;
    private final RevenueOrderRepository orderRepo;
    private final FiscalYearRepository fyRepo;
    private final org.marrok.amriirad.service.AuthService authService;
    private final org.marrok.amriirad.service.ExportService exportService;
    private final ConcurrencyManager concurrencyManager;

    public RevenueOrderListController(RevenueOrderService orderService,
                                     RevenueOrderRepository orderRepo,
                                     FiscalYearRepository fyRepo,
                                     org.marrok.amriirad.service.AuthService authService,
                                     org.marrok.amriirad.service.ExportService exportService,
                                     ConcurrencyManager concurrencyManager) {
        this.orderService = orderService;
        this.orderRepo = orderRepo;
        this.fyRepo = fyRepo;
        this.authService = authService;
        this.exportService = exportService;
        this.concurrencyManager = concurrencyManager;
    }

    private FiscalYear activeYear;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (topBarController != null) {
            topBarController.setBackVisible(true);
        }
        
        tableLoader = new AsyncTableLoader<>(concurrencyManager, tableView, loadingIndicator);
        
        initColumns();
        setupFilters();
        setupToolbar();
        setupEmptyState();
        setupTableInteraction();
        
        loadDataAsync();
    }

    private void setupToolbar() {
        actionToolbarController.init(
            this::handleNewOrder,
            this::handleEditOrder,
            this::handleDeleteOrder,
            this::loadDataAsync,
            this::handleExport
        );
        actionToolbarController.setAddText("أمر جديد");
        
        // Apply permission-based visibility
        actionToolbarController.setAddVisible(authService.canDo("orders.create"));
        actionToolbarController.setEditVisible(authService.canDo("orders.edit"));
        actionToolbarController.setDeleteVisible(authService.canDo("orders.delete"));
    }

    private void setupEmptyState() {
        emptyStateController.init(
            "لا توجد أوامر إيراد",
            "لم يتم العثور على أي أوامر إيراد مسجلة لهذه السنة المالية.",
            "fas-file-invoice",
            this::handleNewOrder
        );
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
            return new SimpleStringProperty(status != null ? status.getArabicLabel() : "-");
        });

        // Basic styling for status column cells could be added here via CellFactory
    }

    private void setupFilters() {
        statusFilterCombo = new ComboBox<>();
        statusFilterCombo.setPromptText("تصفية بالحالة");
        statusFilterCombo.setPrefWidth(150.0);
        statusFilterCombo.getItems().addAll(OrderStatus.values());
        statusFilterCombo.getItems().add(0, null); // "All" option

        statusFilterCombo.setConverter(new javafx.util.StringConverter<OrderStatus>() {
            @Override
            public String toString(OrderStatus status) {
                return status != null ? status.getArabicLabel() : "الكل";
            }

            @Override
            public OrderStatus fromString(String string) {
                return null;
            }
        });

        filterBarController.setSearchPrompt("بحث عن أمر...");
        filterBarController.addFilter(statusFilterCombo);

        // Bind filter to input changes
        filterBarController.getSearchField().textProperty().addListener((obs, oldV, newV) -> updatePredicate());
        statusFilterCombo.valueProperty().addListener((obs, oldV, newV) -> updatePredicate());
    }

    private void updatePredicate() {
        FilteredList<RevenueOrder> filteredList = tableLoader.getFilteredList();
        if (filteredList == null) return;
        
        String search = filterBarController.getSearchField().getText() == null ? "" : filterBarController.getSearchField().getText().toLowerCase();
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
        org.marrok.amriirad.util.TableHelper.setupActionContextMenu(tableView, 
            this::handleEditOrder, 
            this::handleDeleteOrder
        );
    }

    private void handleEditOrder() {
        RevenueOrder selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (selected.getStatus() == org.marrok.amriirad.model.OrderStatus.DRAFT) {
                openFormModal(selected);
            } else {
                openDetailsModal(selected, this::loadDataAsync);
            }
        }
    }

    private void handleDeleteOrder() {
        RevenueOrder selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        if (!authService.canDo("orders.delete")) {
            org.marrok.amriirad.util.DialogHelper.showError("خطأ", "ليس لديك صلاحية حذف أوامر الإيرادات.");
            return;
        }

        if (selected.getStatus() != OrderStatus.DRAFT) {
            org.marrok.amriirad.util.DialogHelper.showError("تنبيه", "لا يمكن حذف أمر إيراد تم إصداره بالفعل.");
            return;
        }

        if (org.marrok.amriirad.util.DialogHelper.showConfirmation("تأكيد الحذف", "هل أنت متأكد من حذف أمر الإيراد رقم: " + selected.getOrderNumber() + "؟")) {
            concurrencyManager.runAsync(
                () -> {
                    orderRepo.delete(selected.getId(), authService.getCurrentUser().getUsername());
                    return true;
                },
                res -> {
                    org.marrok.amriirad.util.DialogHelper.showInfo("نجاح", "تم حذف أمر الإيراد بنجاح.");
                    loadDataAsync();
                },
                err -> org.marrok.amriirad.util.DialogHelper.showError("خطأ", "فشل في حذف أمر الإيراد: " + err.getMessage())
            );
        }
    }

    private void openDetailsModal(RevenueOrder order, Runnable onRefresh) {
        Stage stage = (Stage) tableView.getScene().getWindow();
        FXMLLoader loader = SceneManager.openModal(stage, "/org/marrok/amriirad/view/orders/order-details-view.fxml", "تفاصيل أمر الإيراد");
        if (loader != null) {
            OrderDetailsController controller = loader.getController();
            controller.initForView(order, onRefresh);
        }
    }


    private void loadDataAsync() {
        tableLoader.load(() -> {
            Optional<FiscalYear> activeFy = fyRepo.findActive();
            if (activeFy.isPresent()) {
                activeYear = activeFy.get();
                return orderRepo.findAll(activeYear.getId());
            }
            return List.<RevenueOrder>of(); 
        }, orders -> {
            emptyStateController.show(orders.isEmpty());
            tableView.setVisible(!orders.isEmpty());
        });
    }

    @FXML
    private void handleNewOrder() {
        openFormModal(null);
    }

    @FXML
    private void handleExport() {
        if (tableView.getItems().isEmpty()) {
            org.marrok.amriirad.util.DialogHelper.showError("تنبيه", "لا توجد بيانات لتصديرها.");
            return;
        }

        java.io.File file = exportService.chooseCSVFile(tableView.getScene().getWindow(), "revenue_orders");
        if (file != null) {
            concurrencyManager.runAsync(
                () -> {
                    exportService.exportOrdersToCSV(tableView.getItems(), file);
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

    private void openFormModal(RevenueOrder order) {
        String perm = order == null ? "revenue_order.create" : "revenue_order.edit";
        if (!authService.canDo(perm)) {
            org.marrok.amriirad.util.DialogHelper.showError("خطأ", 
                order == null ? "ليس لديك صلاحية إنشاء أمر إيراد جديد." : "ليس لديك صلاحية تعديل أوامر الإيرادات.");
            return;
        }

        Stage stage = (Stage) tableView.getScene().getWindow();
        String title = order == null ? "إنشاء أمر إيراد جديد" : "تعديل أمر إيراد";
        
        FXMLLoader loader = SceneManager.openModal(stage, "/org/marrok/amriirad/view/orders/order-form-view.fxml", title);
        if (loader != null) {
            RevenueOrderFormController controller = loader.getController();
            if (order == null) {
                controller.initForCreate(this::loadDataAsync);
            } else {
                controller.initForEdit(order, this::loadDataAsync);
            }
        }
    }

    @FXML
    private void handleZeroValues() {
        SceneManager.loadScene((Stage) tableView.getScene().getWindow(), "/org/marrok/amriirad/view/orders/zero-value-list-view.fxml");
    }
}

package org.marrok.amriirad.controller.debtors;

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
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.core.ConcurrencyManager;
import org.marrok.amriirad.model.Debtor;
import org.marrok.amriirad.model.DebtorType;
import org.marrok.amriirad.repository.DebtorRepository;

import java.net.URL;
import java.util.ResourceBundle;
import org.marrok.amriirad.ui.AsyncTableLoader;
import org.marrok.amriirad.service.ExportService;
import org.marrok.amriirad.util.SceneManager;

public class DebtorListController implements Initializable {

    private static final Logger logger = LogManager.getLogger(DebtorListController.class);

    @FXML private org.marrok.amriirad.controller.shared.TopBarController topBarController;
    @FXML private org.marrok.amriirad.controller.shared.FooterController footerController;

    @FXML private TableView<Debtor> tableView;
    @FXML private TableColumn<Debtor, Integer> colId;
    @FXML private TableColumn<Debtor, String> colFullName;
    @FXML private TableColumn<Debtor, String> colIdNumber;
    @FXML private TableColumn<Debtor, String> colType;
    @FXML private TableColumn<Debtor, String> colPhone;
    @FXML private TableColumn<Debtor, String> colAddress;

    @FXML private org.marrok.amriirad.controller.shared.components.FilterBarController filterBarController;
    @FXML private org.marrok.amriirad.controller.shared.components.ActionToolbarController actionToolbarController;
    @FXML private org.marrok.amriirad.controller.shared.components.EmptyStateController emptyStateController;
    @FXML private ProgressIndicator loadingIndicator;

    private ComboBox<DebtorType> typeFilterCombo;

    private AsyncTableLoader<Debtor> tableLoader;

    private final DebtorRepository debtorRepo;
    private final org.marrok.amriirad.service.AuthService authService;
    private final ExportService exportService;
    private final ConcurrencyManager concurrencyManager;

    public DebtorListController(DebtorRepository debtorRepo, 
                                org.marrok.amriirad.service.AuthService authService,
                                ExportService exportService,
                                ConcurrencyManager concurrencyManager) {
        this.debtorRepo = debtorRepo;
        this.authService = authService;
        this.exportService = exportService;
        this.concurrencyManager = concurrencyManager;
    }

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
            this::handleAddDebtor,
            this::handleEditDebtor,
            this::handleDeleteDebtor,
            this::loadDataAsync,
            this::handleExport
        );
        actionToolbarController.setAddText("مدين جديد");
        
        actionToolbarController.setAddVisible(authService.canDo("debtors.create"));
        actionToolbarController.setEditVisible(authService.canDo("debtors.edit"));
        actionToolbarController.setDeleteVisible(authService.canDo("debtors.delete"));
    }

    private void setupEmptyState() {
        emptyStateController.init(
            "لا يوجد مدينين",
            "لم يتم العثور على أي مدينين مسجلين في النظام.",
            "fas-users-slash",
            this::handleAddDebtor
        );
    }


    private void initColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colIdNumber.setCellValueFactory(new PropertyValueFactory<>("idNumber"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        
        colType.setCellValueFactory(cell -> {
            var type = cell.getValue().getDebtorType();
            return new SimpleStringProperty(type != null ? type.getArabicLabel() : "-");
        });
    }

    private void setupFilters() {
        typeFilterCombo = new ComboBox<>();
        typeFilterCombo.setPromptText("نوع المدين");
        typeFilterCombo.setPrefWidth(150.0);
        typeFilterCombo.getItems().addAll(DebtorType.values());
        typeFilterCombo.getItems().add(0, null); // "All"

        typeFilterCombo.setConverter(new javafx.util.StringConverter<DebtorType>() {
            @Override
            public String toString(DebtorType type) {
                return type != null ? type.getArabicLabel() : "الكل";
            }

            @Override
            public DebtorType fromString(String string) {
                return null;
            }
        });

        filterBarController.setSearchPrompt("بحث عن مدين...");
        filterBarController.addFilter(typeFilterCombo);

        // Bind filter to input changes
        filterBarController.getSearchField().textProperty().addListener((obs, oldV, newV) -> updatePredicate());
        typeFilterCombo.valueProperty().addListener((obs, oldV, newV) -> updatePredicate());
    }

    private void updatePredicate() {
        FilteredList<Debtor> filteredList = tableLoader.getFilteredList();
        if (filteredList == null) return;
        
        String search = filterBarController.getSearchField().getText() == null ? "" : filterBarController.getSearchField().getText().toLowerCase();
        DebtorType type = typeFilterCombo.getValue();

        filteredList.setPredicate(debtor -> {
            if (type != null && debtor.getDebtorType() != type) {
                return false;
            }
            if (search.isEmpty()) return true;

            String name = debtor.getFullName() != null ? debtor.getFullName().toLowerCase() : "";
            String idNum = debtor.getIdNumber() != null ? debtor.getIdNumber().toLowerCase() : "";
            String phone = debtor.getPhone() != null ? debtor.getPhone().toLowerCase() : "";

            return name.contains(search) || idNum.contains(search) || phone.contains(search);
        });
    }

    private void setupTableInteraction() {
        org.marrok.amriirad.util.TableHelper.setupActionContextMenu(tableView, 
            this::handleEditDebtor, 
            this::handleDeleteDebtor
        );
    }

    private void handleDeleteDebtor() {
        Debtor selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (!authService.canDo("debtors.delete")) {
            org.marrok.amriirad.util.DialogHelper.showError("خطأ", "ليس لديك صلاحية حذف المدينين.");
            return;
        }

        if (org.marrok.amriirad.util.DialogHelper.showConfirmation("تأكيد الحذف", "هل أنت متأكد من حذف المدين: " + selected.getFullName() + "؟\nملاحظة: لا يمكن حذف مدين له أوامر إيراد مسجلة.")) {
            concurrencyManager.runAsync(
                () -> {
                    debtorRepo.delete(selected.getId());
                    return true;
                },
                res -> {
                    org.marrok.amriirad.util.DialogHelper.showInfo("نجاح", "تم حذف المدين بنجاح.");
                    loadDataAsync();
                },
                err -> {
                    logger.error("Failed to delete debtor", err);
                    org.marrok.amriirad.util.DialogHelper.showError("خطأ", "تعذر حذف المدين. قد يكون مرتبطاً ببيانات أخرى.");
                }
            );
        }
    }

    @FXML
    private void handleAddDebtor() {
        if (!authService.canDo("debtor.create")) {
            org.marrok.amriirad.util.DialogHelper.showError("خطأ", "ليس لديك صلاحية إضافة مدين جديد.");
            return;
        }
        Stage owner = (Stage) tableView.getScene().getWindow();
        javafx.fxml.FXMLLoader loader = SceneManager.openModal(owner, "/org/marrok/amriirad/view/debtors/debtor-form-view.fxml", "إضافة مدين جديد");
        if (loader != null) {
            org.marrok.amriirad.controller.debtors.DebtorFormController controller = loader.getController();
            controller.initForCreate(() -> loadDataAsync());
        }
    }

    private void handleEditDebtor() {
        Debtor selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            org.marrok.amriirad.util.DialogHelper.showWarning("تنبيه", "يرجى اختيار مدين للتعديل.");
            return;
        }

        if (!authService.canDo("debtor.edit")) {
            org.marrok.amriirad.util.DialogHelper.showError("خطأ", "ليس لديك صلاحية تعديل بيانات المدين.");
            return;
        }
        Stage owner = (Stage) tableView.getScene().getWindow();
        javafx.fxml.FXMLLoader loader = SceneManager.openModal(owner, "/org/marrok/amriirad/view/debtors/debtor-form-view.fxml", "تعديل بيانات المدين");
        if (loader != null) {
            org.marrok.amriirad.controller.debtors.DebtorFormController controller = loader.getController();
            controller.initForEdit(selected, () -> loadDataAsync());
        }
    }

    private void loadDataAsync() {
        tableLoader.load(() -> debtorRepo.findAll(), debtors -> {
            emptyStateController.show(debtors.isEmpty());
            tableView.setVisible(!debtors.isEmpty());
        });
    }

    @FXML
    private void handleRefresh() {
        loadDataAsync();
    }

    @FXML
    private void handleExport() {
        if (tableView.getItems().isEmpty()) {
            org.marrok.amriirad.util.DialogHelper.showError("تنبيه", "لا توجد بيانات لتصديرها.");
            return;
        }

        java.io.File file = exportService.chooseCSVFile(tableView.getScene().getWindow(), "debtors");
        if (file != null) {
            concurrencyManager.runAsync(
                () -> {
                    exportService.exportDebtorsToCSV(tableView.getItems(), file);
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
}

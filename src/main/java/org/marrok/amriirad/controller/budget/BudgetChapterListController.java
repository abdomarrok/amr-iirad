package org.marrok.amriirad.controller.budget;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.controller.orders.BudgetChapterFormController;
import org.marrok.amriirad.controller.shared.TopBarController;
import org.marrok.amriirad.controller.shared.FooterController;
import org.marrok.amriirad.core.ConcurrencyManager;
import org.marrok.amriirad.model.BudgetChapter;
import org.marrok.amriirad.repository.BudgetChapterRepository;
import org.marrok.amriirad.service.AuthService;
import org.marrok.amriirad.ui.AsyncTableLoader;
import org.marrok.amriirad.service.ExportService;
import org.marrok.amriirad.util.DialogHelper;
import org.marrok.amriirad.util.SceneManager;

import java.net.URL;
import java.util.ResourceBundle;

public class BudgetChapterListController implements Initializable {

    private static final Logger logger = LogManager.getLogger(BudgetChapterListController.class);

    @FXML
    private TopBarController topBarController;
    @FXML
    private FooterController footerController;

    @FXML
    private TableView<BudgetChapter> tableView;
    @FXML
    private TableColumn<BudgetChapter, String> colCode;
    @FXML
    private TableColumn<BudgetChapter, String> colLabelAr;
    @FXML
    private TableColumn<BudgetChapter, String> colLabelFr;
    @FXML
    private TableColumn<BudgetChapter, String> colLevel;
    @FXML
    private TableColumn<BudgetChapter, String> colParent;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<Integer> levelFilterCombo;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private Button addChapterBtn;

    private AsyncTableLoader<BudgetChapter> tableLoader;

    private final BudgetChapterRepository chapterRepo;
    private final AuthService authService;
    private final ExportService exportService;
    private final ConcurrencyManager concurrencyManager;

    public BudgetChapterListController(BudgetChapterRepository chapterRepo,
            AuthService authService,
            ExportService exportService,
            ConcurrencyManager concurrencyManager) {
        this.chapterRepo = chapterRepo;
        this.authService = authService;
        this.exportService = exportService;
        this.concurrencyManager = concurrencyManager;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (topBarController != null) {
            topBarController.setBackVisible(true);
            // Reload data when fiscal year changes in the top bar
            topBarController.getFiscalYearCombo().valueProperty().addListener((obs, oldV, newV) -> {
                if (newV != null) loadDataAsync();
            });
        }
        checkPermissions();
        tableLoader = new AsyncTableLoader<>(concurrencyManager, tableView, loadingIndicator);
        initColumns();
        setupFilters();
        setupTableInteraction();
        loadDataAsync();
    }

    private void checkPermissions() {
        if (addChapterBtn != null) {
            boolean canAdd = authService.canDo("budget_chapter.manage");
            addChapterBtn.setVisible(canAdd);
            addChapterBtn.setManaged(canAdd);
        }
    }

    private void initColumns() {
        colCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        colLabelAr.setCellValueFactory(new PropertyValueFactory<>("labelAr"));
        colLabelFr.setCellValueFactory(new PropertyValueFactory<>("labelFr"));

        colLevel.setCellValueFactory(cell -> {
            int level = cell.getValue().getLevel();
            return new SimpleStringProperty(switch (level) {
                case 1 -> "عنوان (Titre)";
                case 2 -> "فصل (Chapitre)";
                case 3 -> "مادة (Article)";
                case 4 -> "فقرة (Paragraphe)";
                default -> String.valueOf(level);
            });
        });

        colParent.setCellValueFactory(cell -> {
            Integer parentId = cell.getValue().getParentId();
            return new SimpleStringProperty(parentId != null ? "ID: " + parentId : "-");
        });
    }

    private void setupFilters() {
        levelFilterCombo.setItems(FXCollections.observableArrayList(null, 1, 2, 3, 4));
        levelFilterCombo.setConverter(new javafx.util.StringConverter<Integer>() {
            @Override
            public String toString(Integer level) {
                if (level == null)
                    return "كل المستويات";
                return switch (level) {
                    case 1 -> "1 - Titre";
                    case 2 -> "2 - Chapitre";
                    case 3 -> "3 - Article";
                    case 4 -> "4 - Paragraphe";
                    default -> String.valueOf(level);
                };
            }

            @Override
            public Integer fromString(String string) {
                return null;
            }
        });

        searchField.textProperty().addListener((obs, oldV, newV) -> updatePredicate());
        levelFilterCombo.valueProperty().addListener((obs, oldV, newV) -> updatePredicate());
    }

    private void updatePredicate() {
        FilteredList<BudgetChapter> filteredList = tableLoader.getFilteredList();
        if (filteredList == null)
            return;

        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        Integer level = levelFilterCombo.getValue();

        filteredList.setPredicate(bc -> {
            if (level != null && bc.getLevel() != level)
                return false;
            if (search.isEmpty())
                return true;

            String code = bc.getCode() != null ? bc.getCode().toLowerCase() : "";
            String ar = bc.getLabelAr() != null ? bc.getLabelAr().toLowerCase() : "";
            String fr = bc.getLabelFr() != null ? bc.getLabelFr().toLowerCase() : "";

            return code.contains(search) || ar.contains(search) || fr.contains(search);
        });
    }

    private void setupTableInteraction() {
        tableView.setOnMouseClicked((MouseEvent event) -> {
            if (event.getClickCount() == 2 && tableView.getSelectionModel().getSelectedItem() != null) {
                BudgetChapter selected = tableView.getSelectionModel().getSelectedItem();
                handleEditChapter(selected);
            }
        });
    }

    @FXML
    private void handleAddChapter() {
        if (!authService.canDo("budget_chapter.manage")) {
            DialogHelper.showError("خطأ", "ليس لديك صلاحية إدارة بنود الميزانية.");
            return;
        }
        var selectedYear = topBarController.getFiscalYearCombo().getValue();
        if (selectedYear == null) {
            DialogHelper.showError("تنبيه", "يرجى اختيار سنة مالية أولاً.");
            return;
        }

        Stage owner = (Stage) tableView.getScene().getWindow();
        javafx.fxml.FXMLLoader loader = SceneManager.openModal(owner,
                "/org/marrok/amriirad/view/orders/budget-chapter-form-view.fxml", "إضافة بند ميزانية جديد");
        if (loader != null) {
            BudgetChapterFormController controller = loader.getController();
            controller.initForCreate(selectedYear.getId(), this::loadDataAsync);
        }
    }

    private void handleEditChapter(BudgetChapter bc) {
        if (!authService.canDo("budget_chapter.manage")) {
            DialogHelper.showError("خطأ", "ليس لديك صلاحية إدارة بنود الميزانية.");
            return;
        }
        Stage owner = (Stage) tableView.getScene().getWindow();
        javafx.fxml.FXMLLoader loader = SceneManager.openModal(owner,
                "/org/marrok/amriirad/view/orders/budget-chapter-form-view.fxml", "تعديل بند ميزانية");
        if (loader != null) {
            BudgetChapterFormController controller = loader.getController();
            controller.initForEdit(bc, this::loadDataAsync);
        }
    }

    private void loadDataAsync() {
        var selectedYear = topBarController.getFiscalYearCombo().getValue();
        if (selectedYear != null) {
            tableLoader.load(() -> chapterRepo.findAll(selectedYear.getId()));
        }
    }

    @FXML
    private void handleRefresh() {
        loadDataAsync();
    }

    @FXML
    private void handleExport() {
        if (tableView.getItems().isEmpty()) {
            DialogHelper.showError("تنبيه", "لا توجد بيانات لتصديرها.");
            return;
        }

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("تصدير البيانات");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        fileChooser.setInitialFileName("budget_chapters_" + java.time.LocalDate.now() + ".csv");

        java.io.File file = fileChooser.showSaveDialog(tableView.getScene().getWindow());
        if (file != null) {
            concurrencyManager.runAsync(
                () -> {
                    exportService.exportBudgetChaptersToCSV(tableView.getItems(), file);
                    return true;
                },
                res -> DialogHelper.showInfo("نجاح", "تم تصدير البيانات بنجاح إلى:\n" + file.getName()),
                err -> {
                    logger.error("Export failed", err);
                    DialogHelper.showError("خطأ", "فشل تصدير البيانات: " + err.getMessage());
                }
            );
        }
    }
}

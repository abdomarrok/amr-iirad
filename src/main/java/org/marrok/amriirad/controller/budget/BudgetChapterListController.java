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
import org.marrok.amriirad.repository.RevenueOrderRepository;
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

    @FXML private org.marrok.amriirad.controller.shared.components.FilterBarController filterBarController;
    @FXML private org.marrok.amriirad.controller.shared.components.ActionToolbarController actionToolbarController;
    @FXML private org.marrok.amriirad.controller.shared.components.EmptyStateController emptyStateController;
    @FXML private ProgressIndicator loadingIndicator;

    private ComboBox<Integer> levelFilterCombo;

    private AsyncTableLoader<BudgetChapter> tableLoader;

    private final BudgetChapterRepository chapterRepo;
    private final RevenueOrderRepository revenueOrderRepo;
    private final AuthService authService;
    private final ExportService exportService;
    private final ConcurrencyManager concurrencyManager;

    public BudgetChapterListController(BudgetChapterRepository chapterRepo,
            RevenueOrderRepository revenueOrderRepo,
            AuthService authService,
            ExportService exportService,
            ConcurrencyManager concurrencyManager) {
        this.chapterRepo = chapterRepo;
        this.revenueOrderRepo = revenueOrderRepo;
        this.authService = authService;
        this.exportService = exportService;
        this.concurrencyManager = concurrencyManager;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (topBarController != null) {
            topBarController.setBackVisible(true);
            topBarController.getFiscalYearCombo().valueProperty().addListener((obs, oldV, newV) -> {
                if (newV != null) loadDataAsync();
            });
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
            this::handleAddChapter,
            this::handleEditChapter,
            this::handleDeleteChapter,
            this::loadDataAsync,
            this::handleExport
        );
        actionToolbarController.setAddText("بند جديد");
        
        // Apply permission-based visibility
        actionToolbarController.setAddVisible(authService.canDo("budget_chapter.manage"));
        actionToolbarController.setEditVisible(authService.canDo("budget_chapter.manage"));
        actionToolbarController.setDeleteVisible(authService.canDo("budget_chapter.manage"));
    }

    private void setupEmptyState() {
        emptyStateController.init(
            "لا توجد محاور ميزانية",
            "لم يتم العثور على أي محاور ميزانية مسجلة لهذه السنة المالية.",
            "fas-folder-open",
            this::handleAddChapter
        );
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
        levelFilterCombo = new ComboBox<>();
        levelFilterCombo.setPromptText("المستوى");
        levelFilterCombo.setPrefWidth(150.0);
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

        filterBarController.setSearchPrompt("بحث عن بند...");
        filterBarController.addFilter(levelFilterCombo);

        filterBarController.getSearchField().textProperty().addListener((obs, oldV, newV) -> updatePredicate());
        levelFilterCombo.valueProperty().addListener((obs, oldV, newV) -> updatePredicate());
    }

    private void updatePredicate() {
        FilteredList<BudgetChapter> filteredList = tableLoader.getFilteredList();
        if (filteredList == null)
            return;

        String search = filterBarController.getSearchField().getText() == null ? "" : filterBarController.getSearchField().getText().toLowerCase();
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
        org.marrok.amriirad.util.TableHelper.setupActionContextMenu(tableView, 
            this::handleEditChapter, 
            this::handleDeleteChapter
        );
    }

    private void handleDeleteChapter() {
        BudgetChapter selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (!authService.canDo("budget_chapter.manage")) {
            DialogHelper.showError("خطأ", "ليس لديك صلاحية حذف بنود الميزانية.");
            return;
        }

        if (DialogHelper.showConfirmation("تأكيد الحذف", "هل أنت متأكد من حذف البند: " + selected.getCode() + "؟")) {
            concurrencyManager.runAsync(
                () -> {
                    // Pre-check: Is it used by any orders?
                    if (revenueOrderRepo.isChapterInUse(selected.getId())) {
                        throw new RuntimeException("ChapterInUse");
                    }
                    chapterRepo.delete(selected.getId());
                    return true;
                },
                res -> {
                    DialogHelper.showInfo("نجاح", "تم حذف البند بنجاح.");
                    loadDataAsync();
                },
                err -> {
                    if ("ChapterInUse".equals(err.getMessage())) {
                        DialogHelper.showError("خطأ", "لا يمكن حذف هذا البند لأنه مرتبط بأوامر إيراد مسجلة.");
                    } else {
                        logger.error("Failed to delete chapter", err);
                        String msg = err.getMessage();
                        if (msg != null && msg.contains("foreign key")) {
                            DialogHelper.showError("خطأ", "لا يمكن حذف هذا البند لأنه مرتبط ببيانات أخرى.");
                        } else {
                            DialogHelper.showError("خطأ", "تعذر حذف البند: " + msg);
                        }
                    }
                }
            );
        }
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

    private void handleEditChapter() {
        BudgetChapter selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogHelper.showWarning("تنبيه", "يرجى اختيار بند للتعديل.");
            return;
        }

        if (!authService.canDo("budget_chapter.manage")) {
            DialogHelper.showError("خطأ", "ليس لديك صلاحية إدارة بنود الميزانية.");
            return;
        }
        Stage owner = (Stage) tableView.getScene().getWindow();
        javafx.fxml.FXMLLoader loader = SceneManager.openModal(owner,
                "/org/marrok/amriirad/view/orders/budget-chapter-form-view.fxml", "تعديل بند ميزانية");
        if (loader != null) {
            BudgetChapterFormController controller = loader.getController();
            controller.initForEdit(selected, this::loadDataAsync);
        }
    }

    private void loadDataAsync() {
        var selectedYear = topBarController.getFiscalYearCombo().getValue();
        if (selectedYear != null) {
            tableLoader.load(() -> chapterRepo.findAll(selectedYear.getId()), chapters -> {
                emptyStateController.show(chapters.isEmpty());
                tableView.setVisible(!chapters.isEmpty());
            });
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

        java.io.File file = exportService.chooseCSVFile(tableView.getScene().getWindow(), "budget_chapters");
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

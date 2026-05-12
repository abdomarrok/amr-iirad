package org.marrok.amriirad.controller.orders;

import org.marrok.amriirad.controller.BaseFormController;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.core.ConcurrencyManager;
import org.marrok.amriirad.model.BudgetChapter;
import org.marrok.amriirad.repository.BudgetChapterRepository;

import java.net.URL;
import java.util.ResourceBundle;

public class BudgetChapterFormController extends BaseFormController implements Initializable {

    private static final Logger logger = LogManager.getLogger(BudgetChapterFormController.class);

    @FXML private javafx.scene.layout.VBox root;
    @FXML private ComboBox<Integer> levelCombo;
    @FXML private TextField codeField;
    @FXML private TextField labelArField;
    @FXML private TextField labelFrField;
    @FXML private ComboBox<BudgetChapter> parentCombo;

    private final BudgetChapterRepository chapterRepo;
    private BudgetChapter currentChapter;

    public BudgetChapterFormController(BudgetChapterRepository chapterRepo, ConcurrencyManager concurrencyManager) {
        super(concurrencyManager);
        this.chapterRepo = chapterRepo;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Setup level combo with options: 1=Titre, 2=Chapitre, 3=Article, 4=Paragraphe
        levelCombo.setItems(FXCollections.observableArrayList(1, 2, 3, 4));
        levelCombo.setConverter(new javafx.util.StringConverter<Integer>() {
            @Override
            public String toString(Integer level) {
                if (level == null)
                    return "";
                return switch (level) {
                    case 1 -> "1 - العنوان (Titre)";
                    case 2 -> "2 - الفصل (Chapitre)";
                    case 3 -> "3 - المادة (Article)";
                    case 4 -> "4 - الفقرة (Paragraphe)";
                    default -> "";
                };
            }

            @Override
            public Integer fromString(String string) {
                return null;
            }
        });

        levelCombo.setValue(2); // Default to Chapitre

        parentCombo.setConverter(new javafx.util.StringConverter<BudgetChapter>() {
            @Override
            public String toString(BudgetChapter chapter) {
                if (chapter == null) return "";
                return chapter.getCode() + " - " + chapter.getLabelAr();
            }
            @Override
            public BudgetChapter fromString(String string) { return null; }
        });

        // When level changes, update parent combo
        levelCombo.valueProperty().addListener((obs, oldV, newV) -> {
            markDirty();
            if (newV != null && newV > 1) {
                loadParentChapters(null);
            } else {
                parentCombo.getItems().clear();
            }
        });
        
        setupDirtyTracking();
        setupCommonShortcuts(root, this::handleSave);
    }

    private void setupDirtyTracking() {
        codeField.textProperty().addListener((o, ov, nv) -> markDirty());
        labelArField.textProperty().addListener((o, ov, nv) -> markDirty());
        labelFrField.textProperty().addListener((o, ov, nv) -> markDirty());
        parentCombo.valueProperty().addListener((o, ov, nv) -> markDirty());
    }

    private void loadParentChapters(Integer idToSelect) {
        if (currentChapter == null) return;
        
        concurrencyManager.runAsync(
            () -> {
                Integer level = levelCombo.getValue();
                if (level == null || level <= 1) {
                    return chapterRepo.findAll(currentChapter.getFiscalYearId());
                }
                return chapterRepo.findByLevel(currentChapter.getFiscalYearId(), level - 1);
            },
            result -> {
                java.util.List<BudgetChapter> parents = (java.util.List<BudgetChapter>) result;
                parentCombo.setItems(FXCollections.observableArrayList(parents));
                if (idToSelect != null) {
                    parents.stream()
                        .filter(p -> p.getId() == idToSelect)
                        .findFirst()
                        .ifPresent(parentCombo.getSelectionModel()::select);
                }
            },
            err -> showError("خطأ في تحميل البنود الأب: " + err.getMessage())
        );
    }

    public void initForCreate(int fiscalYearId, Runnable onSuccess) {
        this.currentChapter = new BudgetChapter();
        this.currentChapter.setFiscalYearId(fiscalYearId);
        this.onSuccess = onSuccess;
        titleLabel.setText("إضافة بند/محور جديد");
        
        loadParentChapters(null);
        javafx.application.Platform.runLater(this::clearDirty);
    }

    public void initForEdit(BudgetChapter chapter, Runnable onSuccess) {
        this.currentChapter = chapter;
        this.onSuccess = onSuccess;
        titleLabel.setText("تعديل بند/محور: " + chapter.getCode());

        levelCombo.setValue(chapter.getLevel());
        codeField.setText(chapter.getCode());
        labelArField.setText(chapter.getLabelAr());
        labelFrField.setText(chapter.getLabelFr());

        loadParentChapters(chapter.getParentId());
        
        javafx.application.Platform.runLater(this::clearDirty);
    }

    @FXML
    private void handleSave() {
        if (!validateForm())
            return;
        clearError();
        setLoading(true);

        currentChapter.setLevel(levelCombo.getValue());
        currentChapter.setCode(codeField.getText().trim());
        currentChapter.setLabelAr(labelArField.getText().trim());
        currentChapter.setLabelFr(labelFrField.getText().trim());

        if (parentCombo.getValue() != null) {
            currentChapter.setParentId(parentCombo.getValue().getId());
        }

        concurrencyManager.runAsync(
                () -> chapterRepo.save(currentChapter),
                result -> {
                    setLoading(false);
                    clearDirty();
                    logger.info("Saved budget chapter: {}", result.getCode());
                    org.marrok.amriirad.util.DialogHelper.showInfo("نجاح", "تم حفظ بند الميزانية بنجاح.");
                    closeWindow();
                    runOnSuccess();
                },
                err -> {
                    setLoading(false);
                    String msg = err.getMessage();
                    if (msg.contains("Duplicate entry")) {
                        showError("هذا الرمز موجود بالفعل في هذه السنة المالية.");
                    } else {
                        showError("خطأ في حفظ البند: " + msg);
                    }
                });
    }

    @Override
    protected boolean validateForm() {
        clearError();
        setInvalid(codeField, false);
        setInvalid(labelArField, false);
        setInvalid(labelFrField, false);
        setInvalid(levelCombo, false);
        setInvalid(parentCombo, false);

        String code = codeField.getText() == null ? "" : codeField.getText().trim();
        String labelAr = labelArField.getText() == null ? "" : labelArField.getText().trim();
        String labelFr = labelFrField.getText() == null ? "" : labelFrField.getText().trim();
        Integer level = levelCombo.getValue();

        if (code.isEmpty()) {
            setInvalid(codeField, true);
            showError("يجب إدخال الرمز");
            return false;
        }
        if (labelAr.isEmpty()) {
            setInvalid(labelArField, true);
            showError("يجب إدخال التسمية بالعربية");
            return false;
        }
        if (labelFr.isEmpty()) {
            setInvalid(labelFrField, true);
            showError("يجب إدخال التسمية بالفرنسية");
            return false;
        }
        if (level == null) {
            setInvalid(levelCombo, true);
            showError("يجب اختيار المستوى");
            return false;
        }
        if (level > 1 && parentCombo.getValue() == null) {
            setInvalid(parentCombo, true);
            showError("يجب اختيار البند الأب للمستويات من 2 فما فوق");
            return false;
        }

        return true;
    }

    @Override
    protected void showError(String msg) {
        super.showError(msg);
    }

    @Override
    protected void clearError() {
        super.clearError();
    }
}

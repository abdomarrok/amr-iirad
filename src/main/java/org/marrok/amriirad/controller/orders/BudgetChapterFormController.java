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

    @FXML private Label titleLabel;
    @FXML private ComboBox<Integer> levelCombo;
    @FXML private TextField codeField;
    @FXML private TextField labelArField;
    @FXML private TextField labelFrField;
    @FXML private ComboBox<BudgetChapter> parentCombo;
    @FXML private Label errorLabel;
    
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
                if (level == null) return "";
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

        // Load parent chapters for hierarchical support
        loadParentChapters();

        // When level changes, update parent combo
        levelCombo.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && newV > 1) {
                loadParentChapters();
            } else {
                parentCombo.getItems().clear();
            }
        });
    }

    private void loadParentChapters() {
        concurrencyManager.runAsync(
            () -> {
                Integer level = levelCombo.getValue();
                if (level == null || level <= 1) {
                    return chapterRepo.findAll();
                }
                return chapterRepo.findByLevel(level - 1);
            },
            result -> {
                parentCombo.setItems(FXCollections.observableArrayList((java.util.List<BudgetChapter>) result));
                parentCombo.setConverter(new javafx.util.StringConverter<BudgetChapter>() {
                    @Override
                    public String toString(BudgetChapter chapter) {
                        if (chapter == null) return "";
                        return chapter.getCode() + " - " + chapter.getLabelAr();
                    }

                    @Override
                    public BudgetChapter fromString(String string) {
                        return null;
                    }
                });
            },
            err -> showError("خطأ في تحميل البنود الأب: " + err.getMessage())
        );
    }

    public void initForCreate(Runnable onSuccess) {
        this.currentChapter = new BudgetChapter();
        this.onSuccess = onSuccess;
        titleLabel.setText("إضافة بند/محور جديد");
    }

    @FXML
    private void handleSave() {
        if (!validateForm()) return;
        clearError();

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
                logger.info("Saved budget chapter: {}", result.getCode());
                closeWindow();
                runOnSuccess();
            },
            err -> showError("خطأ في حفظ البند: " + err.getMessage())
        );
    }

    @Override
    protected boolean validateForm() {
        String code = codeField.getText() == null ? "" : codeField.getText().trim();
        String labelAr = labelArField.getText() == null ? "" : labelArField.getText().trim();
        String labelFr = labelFrField.getText() == null ? "" : labelFrField.getText().trim();
        Integer level = levelCombo.getValue();

        if (code.isEmpty()) {
            showError("يجب إدخال الرمز");
            return false;
        }
        if (labelAr.isEmpty()) {
            showError("يجب إدخال التسمية بالعربية");
            return false;
        }
        if (labelFr.isEmpty()) {
            showError("يجب إدخال التسمية بالفرنسية");
            return false;
        }
        if (level == null) {
            showError("يجب اختيار المستوى");
            return false;
        }
        if (level > 1 && parentCombo.getValue() == null) {
            showError("يجب اختيار البند الأب للمستويات من 2 فما فوق");
            return false;
        }

        return true;
    }

    @Override
    protected void showError(String msg) {
        errorLabel.setText(msg);
    }

    @Override
    protected void clearError() {
        super.clearError();
    }
}

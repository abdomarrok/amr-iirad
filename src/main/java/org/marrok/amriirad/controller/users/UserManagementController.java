package org.marrok.amriirad.controller.users;

import org.marrok.amriirad.controller.shared.TopBarController;
import org.marrok.amriirad.controller.shared.FooterController;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.marrok.amriirad.model.User;
import org.marrok.amriirad.repository.UserRepository;
import org.marrok.amriirad.util.DialogHelper;
import org.marrok.amriirad.util.SceneManager;
import javafx.beans.property.SimpleStringProperty;
import org.marrok.amriirad.core.ConcurrencyManager;
import org.marrok.amriirad.ui.AsyncTableLoader;
import org.marrok.amriirad.service.ExportService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the User Management screen.
 * Reused pattern from GstockDz.
 */
public class UserManagementController implements Initializable {
    private static final Logger logger = LogManager.getLogger(UserManagementController.class);

    @FXML private org.marrok.amriirad.controller.shared.TopBarController topBarController;
    @FXML private org.marrok.amriirad.controller.shared.FooterController footerController;

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> idCol;
    @FXML private TableColumn<User, String> usernameCol;
    @FXML private TableColumn<User, String> fullNameCol;
    @FXML private TableColumn<User, String> roleCol;
    @FXML private TableColumn<User, String> statusCol;

    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Button addUserBtn;
    @FXML private Button editUserBtn;
    @FXML private Button deleteUserBtn;

    private AsyncTableLoader<User> tableLoader;

    private final UserRepository userRepository;
    private final ExportService exportService;
    private final ConcurrencyManager concurrencyManager;
    private final ObservableList<User> userList = FXCollections.observableArrayList();

    public UserManagementController(UserRepository userRepository, ExportService exportService, ConcurrencyManager concurrencyManager) {
        this.userRepository = userRepository;
        this.exportService = exportService;
        this.concurrencyManager = concurrencyManager;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (topBarController != null) {
            topBarController.setBackVisible(true);
        }
        checkPermissions();
        tableLoader = new AsyncTableLoader<>(concurrencyManager, userTable, loadingIndicator);
        setupTable();
        loadDataAsync();
    }

    private void checkPermissions() {
        var auth = org.marrok.amriirad.core.AppContext.getInstance().getAuthService();
        boolean canManage = auth.canDo("users.manage");
        
        setBtnVisible(addUserBtn, canManage);
        setBtnVisible(editUserBtn, canManage);
        setBtnVisible(deleteUserBtn, canManage);
    }

    private void setBtnVisible(Button btn, boolean visible) {
        if (btn != null) {
            btn.setVisible(visible);
            btn.setManaged(visible);
        }
    }

    private void setupTable() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        fullNameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        roleCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRoleName()));
        statusCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().isActive() ? "نشط" : "مجمد"));
    }

    private void loadDataAsync() {
        tableLoader.load(() -> userRepository.findAll());
    }

    @FXML
    private void handleAddUser() {
        Stage stage = (Stage) userTable.getScene().getWindow();
        FXMLLoader loader = SceneManager.openModal(stage, "/org/marrok/amriirad/view/users/user-form-view.fxml", "إضافة مستخدم جديد");
        if (loader != null) {
            UserFormController controller = loader.getController();
            controller.initForCreate(this::loadDataAsync);
        }
    }

    @FXML
    private void handleEditUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogHelper.showWarning("تنبيه", "يرجى اختيار مستخدم للتعديل.");
            return;
        }
        
        Stage stage = (Stage) userTable.getScene().getWindow();
        FXMLLoader loader = SceneManager.openModal(stage, "/org/marrok/amriirad/view/users/user-form-view.fxml", "تعديل مستخدم");
        if (loader != null) {
            UserFormController controller = loader.getController();
            controller.initForUpdate(selected, this::loadDataAsync);
        }
    }

    @FXML
    private void handleDeleteUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogHelper.showWarning("تنبيه", "يرجى اختيار مستخدم للحذف.");
            return;
        }

        if ("admin".equals(selected.getUsername())) {
            DialogHelper.showError("خطأ", "لا يمكن حذف المستخدم admin الأساسي.");
            return;
        }

        if (DialogHelper.showConfirmation("تأكيد الحذف", "هل أنت متأكد من حذف المستخدم " + selected.getUsername() + "؟")) {
            if (userRepository.delete(selected.getId())) {
                loadDataAsync();
                DialogHelper.showInfo("نجاح", "تم حذف المستخدم بنجاح.");
            } else {
                DialogHelper.showError("خطأ", "فشل حذف المستخدم.");
            }
        }
    }

    @FXML
    private void handleRefresh() {
        loadDataAsync();
    }

    @FXML
    private void handleExport() {
        if (userTable.getItems().isEmpty()) {
            DialogHelper.showError("تنبيه", "لا توجد بيانات لتصديرها.");
            return;
        }

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("تصدير البيانات");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        fileChooser.setInitialFileName("users_" + java.time.LocalDate.now() + ".csv");

        java.io.File file = fileChooser.showSaveDialog(userTable.getScene().getWindow());
        if (file != null) {
            concurrencyManager.runAsync(
                () -> {
                    exportService.exportUsersToCSV(userTable.getItems(), file);
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

package org.marrok.amriirad.controller;

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

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the User Management screen.
 * Reused pattern from GstockDz.
 */
public class UserManagementController implements Initializable {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colFullName;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, Boolean> colActive;

    @FXML private Button addUserBtn;
    @FXML private Button editUserBtn;
    @FXML private Button deleteUserBtn;

    private final UserRepository userRepository;
    private final ObservableList<User> userList = FXCollections.observableArrayList();

    public UserManagementController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        checkPermissions();
        setupTable();
        loadUsers();
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
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("roleName"));
        colActive.setCellValueFactory(new PropertyValueFactory<>("active"));

        // Custom cell factory for Boolean "active" status if needed
        colActive.setCellFactory(column -> new TableCell<User, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item ? "نشط" : "غير نشط");
                    setStyle(item ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
                }
            }
        });
    }

    private void loadUsers() {
        userList.setAll(userRepository.findAll());
        userTable.setItems(userList);
    }

    @FXML
    private void handleAddUser() {
        Stage stage = (Stage) userTable.getScene().getWindow();
        FXMLLoader loader = SceneManager.openModal(stage, "/org/marrok/amriirad/view/user-form-view.fxml", "إضافة مستخدم جديد");
        if (loader != null) {
            UserFormController controller = loader.getController();
            controller.initForCreate(this::loadUsers);
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
        FXMLLoader loader = SceneManager.openModal(stage, "/org/marrok/amriirad/view/user-form-view.fxml", "تعديل مستخدم");
        if (loader != null) {
            UserFormController controller = loader.getController();
            controller.initForUpdate(selected, this::loadUsers);
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
                loadUsers();
                DialogHelper.showInfo("نجاح", "تم حذف المستخدم بنجاح.");
            } else {
                DialogHelper.showError("خطأ", "فشل حذف المستخدم.");
            }
        }
    }

    @FXML
    private void handleRefresh() {
        loadUsers();
    }
}

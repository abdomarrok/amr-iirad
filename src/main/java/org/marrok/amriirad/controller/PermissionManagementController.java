package org.marrok.amriirad.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.core.AppContext;
import org.marrok.amriirad.model.Permission;
import org.marrok.amriirad.model.Role;
import org.marrok.amriirad.repository.PermissionRepository;
import org.marrok.amriirad.repository.RoleRepository;
import org.marrok.amriirad.util.DialogHelper;
import org.marrok.amriirad.util.SceneManager;

import java.net.URL;
import java.util.*;

/**
 * Controller for the Permission Management UI.
 * Adapted from GstockDz pattern for amr-iirad.
 */
public class PermissionManagementController implements Initializable {

    private static final Logger logger = LogManager.getLogger(PermissionManagementController.class);

    @FXML private ListView<Role> rolesListView;
    @FXML private TextField roleNameField;
    @FXML private TextField roleDescriptionField;
    @FXML private Spinner<Integer> privilegeLevelSpinner;
    @FXML private VBox viewPermissionsBox;
    @FXML private VBox actionPermissionsBox;
    @FXML private Button addRoleBtn;
    @FXML private Button deleteRoleBtn;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    private ObservableList<Role> rolesList;
    private Role selectedRole;
    private boolean isNewRole = false;

    // Maps to track checkbox states
    private final Map<Integer, CheckBox> viewPermissionCheckboxes = new HashMap<>();
    private final Map<Integer, CheckBox> actionPermissionCheckboxes = new HashMap<>();

    public PermissionManagementController(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.info("Initializing PermissionManagementController");

        // Initialize spinner
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, 25);
        privilegeLevelSpinner.setValueFactory(valueFactory);

        // Load roles
        loadRoles();

        // Load permission checkboxes
        loadPermissionCheckboxes();

        // Setup role selection listener
        rolesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectRole(newVal);
            }
        });

        // Select first role by default
        if (!rolesList.isEmpty()) {
            rolesListView.getSelectionModel().selectFirst();
        }

        updateUIState();
    }

    private void loadRoles() {
        List<Role> roles = roleRepository.findAll();
        rolesList = FXCollections.observableArrayList(roles);
        rolesListView.setItems(rolesList);

        rolesListView.setCellFactory(lv -> new ListCell<Role>() {
            @Override
            protected void updateItem(Role item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + (item.isProtected() ? " 🔒" : ""));
                }
            }
        });
    }

    private void loadPermissionCheckboxes() {
        viewPermissionCheckboxes.clear();
        actionPermissionCheckboxes.clear();
        viewPermissionsBox.getChildren().clear();
        actionPermissionsBox.getChildren().clear();

        // Load view permissions
        Map<String, List<Permission>> viewPerms = permissionRepository.getViewPermissionsGroupedByCategory();
        for (Map.Entry<String, List<Permission>> entry : viewPerms.entrySet()) {
            addCategoryGroup(viewPermissionsBox, entry.getKey(), entry.getValue(), viewPermissionCheckboxes);
        }

        // Load action permissions
        Map<String, List<Permission>> actionPerms = permissionRepository.getActionPermissionsGroupedByCategory();
        for (Map.Entry<String, List<Permission>> entry : actionPerms.entrySet()) {
            addCategoryGroup(actionPermissionsBox, entry.getKey(), entry.getValue(), actionPermissionCheckboxes);
        }
    }

    private void addCategoryGroup(VBox container, String category, List<Permission> perms, Map<Integer, CheckBox> map) {
        Label categoryLabel = new Label(getCategoryDisplayName(category));
        categoryLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: var(--primary-color);");
        container.getChildren().add(categoryLabel);

        VBox permBox = new VBox(5);
        permBox.setPadding(new javafx.geometry.Insets(0, 0, 10, 10));
        for (Permission perm : perms) {
            CheckBox cb = new CheckBox(perm.getDescription());
            cb.setUserData(perm);
            map.put(perm.getId(), cb);
            permBox.getChildren().add(cb);
        }
        container.getChildren().add(permBox);
    }

    private void selectRole(Role role) {
        Optional<Role> roleOpt = roleRepository.findById(role.getId());
        this.selectedRole = roleOpt.orElse(null);
        this.isNewRole = false;

        if (selectedRole != null) {
            roleNameField.setText(selectedRole.getName());
            roleDescriptionField.setText(selectedRole.getDescription());
            privilegeLevelSpinner.getValueFactory().setValue(selectedRole.getPrivilegeLevel());

            updateCheckboxStates();
            updateUIState();
        }
    }

    private void updateCheckboxStates() {
        viewPermissionCheckboxes.values().forEach(cb -> cb.setSelected(false));
        actionPermissionCheckboxes.values().forEach(cb -> cb.setSelected(false));

        if (selectedRole == null) return;

        // Admin has all permissions
        boolean isAdmin = selectedRole.isProtected() && "ADMIN".equals(selectedRole.getName());

        viewPermissionCheckboxes.values().forEach(cb -> {
            Permission p = (Permission) cb.getUserData();
            boolean has = isAdmin || hasPermission(p.getId());
            cb.setSelected(has);
            cb.setDisable(isAdmin);
        });

        actionPermissionCheckboxes.values().forEach(cb -> {
            Permission p = (Permission) cb.getUserData();
            boolean has = isAdmin || hasPermission(p.getId());
            cb.setSelected(has);
            cb.setDisable(isAdmin);
        });
    }

    private boolean hasPermission(int permId) {
        if (selectedRole == null || selectedRole.getPermissions() == null) return false;
        return selectedRole.getPermissions().stream().anyMatch(p -> p.getId() == permId);
    }

    private void updateUIState() {
        boolean isProtected = selectedRole != null && selectedRole.isProtected();
        roleNameField.setDisable(isProtected);
        deleteRoleBtn.setDisable(isProtected || selectedRole == null);
        saveBtn.setDisable(isProtected && selectedRole != null && "ADMIN".equals(selectedRole.getName()));
    }

    @FXML
    private void handleAddNewRole() {
        isNewRole = true;
        selectedRole = null;
        rolesListView.getSelectionModel().clearSelection();

        roleNameField.setText("");
        roleDescriptionField.setText("");
        privilegeLevelSpinner.getValueFactory().setValue(10);
        
        viewPermissionCheckboxes.values().forEach(cb -> {
            cb.setSelected(false);
            cb.setDisable(false);
        });
        actionPermissionCheckboxes.values().forEach(cb -> {
            cb.setSelected(false);
            cb.setDisable(false);
        });

        roleNameField.setDisable(false);
        saveBtn.setDisable(false);
        deleteRoleBtn.setDisable(true);
        roleNameField.requestFocus();
    }

    @FXML
    private void handleDeleteRole() {
        if (selectedRole == null || selectedRole.isProtected()) return;

        if (DialogHelper.showConfirmation("تأكيد الحذف", "هل أنت متأكد من حذف الدور: " + selectedRole.getName() + "؟")) {
            if (roleRepository.delete(selectedRole.getId())) {
                DialogHelper.showInfo("نجاح", "تم حذف الدور بنجاح.");
                loadRoles();
                if (!rolesList.isEmpty()) rolesListView.getSelectionModel().selectFirst();
            }
        }
    }

    @FXML
    private void handleSave() {
        String name = roleNameField.getText().trim();
        String desc = roleDescriptionField.getText().trim();
        int level = privilegeLevelSpinner.getValue();

        if (name.isEmpty()) {
            DialogHelper.showWarning("تنبيه", "يرجى إدخال اسم الدور.");
            return;
        }

        List<Integer> selectedPermIds = new ArrayList<>();
        viewPermissionCheckboxes.forEach((id, cb) -> { if (cb.isSelected()) selectedPermIds.add(id); });
        actionPermissionCheckboxes.forEach((id, cb) -> { if (cb.isSelected()) selectedPermIds.add(id); });

        if (isNewRole) {
            int newId = roleRepository.create(name, desc, level);
            if (newId > 0) {
                roleRepository.setPermissionsForRole(newId, selectedPermIds);
                DialogHelper.showInfo("نجاح", "تم إنشاء الدور بنجاح.");
                loadRoles();
                selectById(newId);
            }
        } else if (selectedRole != null) {
            if (roleRepository.update(selectedRole.getId(), name, desc, level)) {
                roleRepository.setPermissionsForRole(selectedRole.getId(), selectedPermIds);
                DialogHelper.showInfo("نجاح", "تم تحديث الدور بنجاح.");
                loadRoles();
                selectById(selectedRole.getId());
            }
        }
        isNewRole = false;
    }

    private void selectById(int id) {
        for (Role r : rolesList) {
            if (r.getId() == id) {
                rolesListView.getSelectionModel().select(r);
                break;
            }
        }
    }

    @FXML
    private void handleCancel() {
        if (isNewRole) {
            if (!rolesList.isEmpty()) rolesListView.getSelectionModel().selectFirst();
            else handleAddNewRole();
        } else if (selectedRole != null) {
            selectRole(selectedRole);
        }
    }

    @FXML
    private void handleBack() {
        SceneManager.loadScene((Stage) rolesListView.getScene().getWindow(), "/org/marrok/amriirad/view/dashboard-view.fxml");
    }

    @FXML
    private void handleSelectAllView() {
        viewPermissionCheckboxes.values().forEach(cb -> { if (!cb.isDisabled()) cb.setSelected(true); });
    }

    @FXML
    private void handleSelectAllAction() {
        actionPermissionCheckboxes.values().forEach(cb -> { if (!cb.isDisabled()) cb.setSelected(true); });
    }

    private String getCategoryDisplayName(String category) {
        return switch (category) {
            case "dashboard" -> "لوحة التحكم";
            case "orders" -> "أوامر الإيراد";
            case "debtors" -> "المدينين";
            case "users" -> "المستخدمين";
            case "settings" -> "الإعدادات";
            case "reports" -> "التقارير";
            default -> category;
        };
    }
}

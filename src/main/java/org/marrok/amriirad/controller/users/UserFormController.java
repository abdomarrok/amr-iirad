package org.marrok.amriirad.controller.users;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.marrok.amriirad.model.Role;
import org.marrok.amriirad.model.User;
import org.marrok.amriirad.repository.RoleRepository;
import org.marrok.amriirad.repository.UserRepository;
import org.marrok.amriirad.util.DialogHelper;
import org.marrok.amriirad.util.PasswordUtil;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the User add/edit form.
 * Reused pattern from GstockDz.
 */
public class UserFormController implements Initializable {

    @FXML private Label titleLabel;
    @FXML private TextField usernameField;
    @FXML private TextField fullNameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<Role> roleCombo;
    @FXML private CheckBox activeCheck;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    
    private User targetUser;
    private Runnable onSaveCallback;
    private boolean isEditMode = false;

    public UserFormController(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupRoleCombo();
    }

    private void setupRoleCombo() {
        roleCombo.setConverter(new StringConverter<Role>() {
            @Override
            public String toString(Role role) {
                return role != null ? role.getName() : "";
            }
            @Override
            public Role fromString(String string) { return null; }
        });
        roleCombo.getItems().setAll(roleRepository.findAll());
    }

    public void initForCreate(Runnable onSaveCallback) {
        this.isEditMode = false;
        this.onSaveCallback = onSaveCallback;
        this.titleLabel.setText("إضافة مستخدم جديد");
        this.activeCheck.setSelected(true);
    }

    public void initForUpdate(User user, Runnable onSaveCallback) {
        this.isEditMode = true;
        this.targetUser = user;
        this.onSaveCallback = onSaveCallback;
        
        this.titleLabel.setText("تعديل مستخدم: " + user.getUsername());
        this.usernameField.setText(user.getUsername());
        this.fullNameField.setText(user.getFullName());
        this.activeCheck.setSelected(user.isActive());
        
        // Disable username change for admin
        if ("admin".equals(user.getUsername())) {
            usernameField.setEditable(false);
            activeCheck.setDisable(true);
        }

        // Select the current role
        roleCombo.getItems().stream()
                .filter(r -> r.getId() == user.getRoleId())
                .findFirst()
                .ifPresent(r -> roleCombo.getSelectionModel().select(r));
    }

    @FXML
    private void handleSave() {
        if (!validateInput()) return;

        String username = usernameField.getText().trim();
        String fullName = fullNameField.getText().trim();
        String password = passwordField.getText();
        Role role = roleCombo.getValue();
        boolean isActive = activeCheck.isSelected();

        if (isEditMode) {
            targetUser.setUsername(username);
            targetUser.setFullName(fullName);
            targetUser.setRoleId(role.getId());
            targetUser.setActive(isActive);
            
            // Only update password if provided
            if (password != null && !password.isEmpty()) {
                targetUser.setPassword(PasswordUtil.hashPassword(password));
            }

            if (userRepository.update(targetUser)) {
                finish();
            } else {
                DialogHelper.showError("خطأ", "فشل تحديث بيانات المستخدم.");
            }
        } else {
            if (userRepository.existsByUsername(username)) {
                DialogHelper.showError("خطأ", "اسم المستخدم موجود مسبقاً.");
                return;
            }

            User newUser = new User();
            newUser.setUsername(username);
            newUser.setFullName(fullName);
            newUser.setPassword(PasswordUtil.hashPassword(password));
            newUser.setRoleId(role.getId());
            newUser.setActive(isActive);

            if (userRepository.create(newUser) > 0) {
                finish();
            } else {
                DialogHelper.showError("خطأ", "فشل إنشاء المستخدم.");
            }
        }
    }

    private boolean validateInput() {
        if (usernameField.getText().trim().isEmpty()) {
            DialogHelper.showWarning("تنبيه", "اسم المستخدم مطلوب.");
            return false;
        }
        if (!isEditMode && passwordField.getText().isEmpty()) {
            DialogHelper.showWarning("تنبيه", "كلمة المرور مطلوبة للمستخدم الجديد.");
            return false;
        }
        if (roleCombo.getValue() == null) {
            DialogHelper.showWarning("تنبيه", "يرجى اختيار دور للمستخدم.");
            return false;
        }
        return true;
    }

    private void finish() {
        if (onSaveCallback != null) onSaveCallback.run();
        handleCancel();
    }

    @FXML
    private void handleCancel() {
        ((Stage) usernameField.getScene().getWindow()).close();
    }
}

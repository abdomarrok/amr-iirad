package org.marrok.amriirad.controller.users;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.marrok.amriirad.controller.BaseFormController;
import org.marrok.amriirad.core.ConcurrencyManager;
import org.marrok.amriirad.model.Role;
import org.marrok.amriirad.model.User;
import org.marrok.amriirad.repository.RoleRepository;
import org.marrok.amriirad.repository.UserRepository;
import org.marrok.amriirad.util.DialogHelper;
import org.marrok.amriirad.util.PasswordUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the User add/edit form.
 * Reused pattern from GstockDz.
 */
public class UserFormController extends BaseFormController implements Initializable {

    private static final Logger logger = LogManager.getLogger(UserFormController.class);

    @FXML private VBox root;
    @FXML private TextField usernameField;
    @FXML private TextField fullNameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<Role> roleCombo;
    @FXML private CheckBox activeCheck;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    
    private User targetUser;
    private boolean isEditMode = false;

    public UserFormController(UserRepository userRepository, RoleRepository roleRepository, ConcurrencyManager concurrencyManager) {
        super(concurrencyManager);
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupRoleCombo();
        setupDirtyTracking();
        setupCommonShortcuts(root, this::handleSave);
        clearError();
    }

    private void setupDirtyTracking() {
        usernameField.textProperty().addListener((o, ov, nv) -> markDirty());
        fullNameField.textProperty().addListener((o, ov, nv) -> markDirty());
        passwordField.textProperty().addListener((o, ov, nv) -> markDirty());
        roleCombo.valueProperty().addListener((o, ov, nv) -> markDirty());
        activeCheck.selectedProperty().addListener((o, ov, nv) -> markDirty());
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
        this.onSuccess = onSaveCallback;
        this.titleLabel.setText("إضافة مستخدم جديد");
        this.activeCheck.setSelected(true);
        if (passwordField != null) {
            passwordField.setPromptText("أدخل كلمة المرور للمستخدم الجديد");
        }
        clearError();
        javafx.application.Platform.runLater(this::clearDirty);
    }

    public void initForUpdate(User user, Runnable onSaveCallback) {
        this.isEditMode = true;
        this.targetUser = user;
        this.onSuccess = onSaveCallback;
        
        this.titleLabel.setText("تعديل مستخدم: " + user.getUsername());
        this.usernameField.setText(user.getUsername());
        this.fullNameField.setText(user.getFullName());
        this.activeCheck.setSelected(user.isActive());
        
        if (passwordField != null) {
            passwordField.setPromptText("اتركه فارغاً للحفاظ على الكلمة الحالية");
        }

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

        clearError();
        javafx.application.Platform.runLater(this::clearDirty);
    }

    @FXML
    private void handleSave() {
        if (!validateForm()) return;
        clearError();
        setLoading(true);

        String username = usernameField.getText().trim();
        String fullName = fullNameField.getText().trim();
        String password = passwordField.getText();
        Role role = roleCombo.getValue();
        boolean isActive = activeCheck.isSelected();

        concurrencyManager.runAsync(
            () -> {
                if (isEditMode) {
                    targetUser.setUsername(username);
                    targetUser.setFullName(fullName);
                    targetUser.setRoleId(role.getId());
                    targetUser.setActive(isActive);
                    
                    if (password != null && !password.isEmpty()) {
                        targetUser.setPassword(PasswordUtil.hashPassword(password));
                    }
                    return userRepository.update(targetUser);
                } else {
                    if (userRepository.existsByUsername(username)) {
                        throw new RuntimeException("اسم المستخدم موجود مسبقاً.");
                    }
                    User newUser = new User();
                    newUser.setUsername(username);
                    newUser.setFullName(fullName);
                    newUser.setPassword(PasswordUtil.hashPassword(password));
                    newUser.setRoleId(role.getId());
                    newUser.setActive(isActive);
                    return userRepository.create(newUser) > 0;
                }
            },
            success -> {
                setLoading(false);
                if (success) {
                    clearDirty();
                    DialogHelper.showInfo("نجاح", isEditMode ? "تم تحديث المستخدم بنجاح." : "تم إنشاء المستخدم بنجاح.");
                    closeWindow();
                    runOnSuccess();
                } else {
                    showError("فشل العملية.");
                }
            },
            err -> {
                setLoading(false);
                showError(err.getMessage());
            }
        );
    }

    @Override
    protected boolean validateForm() {
        clearError();
        setInvalid(usernameField, false);
        setInvalid(passwordField, false);
        setInvalid(roleCombo, false);

        if (usernameField.getText().trim().isEmpty()) {
            setInvalid(usernameField, true);
            showError("اسم المستخدم مطلوب.");
            return false;
        }
        if (!isEditMode && (passwordField.getText() == null || passwordField.getText().isEmpty())) {
            setInvalid(passwordField, true);
            showError("كلمة المرور مطلوبة للمستخدم الجديد.");
            return false;
        }
        if (roleCombo.getValue() == null) {
            setInvalid(roleCombo, true);
            showError("يرجى اختيار دور للمستخدم.");
            return false;
        }
        return true;
    }
}

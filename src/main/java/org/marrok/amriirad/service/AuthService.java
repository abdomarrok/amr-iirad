package org.marrok.amriirad.service;

import org.marrok.amriirad.core.AppContext;
import org.marrok.amriirad.util.PasswordUtil;
import java.util.Optional;
import org.marrok.amriirad.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service to manage authentication and user context.
 */
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final org.marrok.amriirad.repository.UserRepository userRepository;
    private final org.marrok.amriirad.repository.RoleRepository roleRepository;
    private User currentUser;
    private final java.util.Set<String> viewPermissions = new java.util.HashSet<>();
    private final java.util.Set<String> actionPermissions = new java.util.HashSet<>();

    public AuthService(org.marrok.amriirad.repository.UserRepository userRepository) {
        this.userRepository = userRepository;
        this.roleRepository = new org.marrok.amriirad.repository.RoleRepository();
    }

    public boolean login(String username, String password) {
        logger.info("Attempting login for user: {}", username);
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (PasswordUtil.verifyPassword(password, user.getPassword())) {
                currentUser = user;
                loadPermissions(user.getRoleId());
                logger.info("Login successful for user: {} (Role: {})", username, user.getRoleName());
                return true;
            } else {
                logger.warn("Invalid password for user: {}", username);
            }
        } else {
            logger.warn("User not found or inactive: {}", username);
        }
        return false;
    }

    private void loadPermissions(int roleId) {
        viewPermissions.clear();
        actionPermissions.clear();
        
        roleRepository.findById(roleId).ifPresent(role -> {
            if ("ADMIN".equals(role.getName()) && role.isProtected()) {
                viewPermissions.add("*");
                actionPermissions.add("*");
                logger.info("Admin role - granting all permissions");
            } else {
                for (org.marrok.amriirad.model.Permission perm : role.getPermissions()) {
                    if ("VIEW".equalsIgnoreCase(perm.getType())) {
                        viewPermissions.add(perm.getCode());
                    } else if ("ACTION".equalsIgnoreCase(perm.getType())) {
                        actionPermissions.add(perm.getCode());
                    }
                }
                logger.info("Loaded {} view and {} action permissions", viewPermissions.size(), actionPermissions.size());
            }
        });
    }

    public void logout() {
        if (currentUser != null) {
            logger.info("User {} logged out", currentUser.getUsername());
        }
        currentUser = null;
        viewPermissions.clear();
        actionPermissions.clear();
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isAdmin() {
        return currentUser != null && "ADMIN".equals(currentUser.getRoleName());
    }
    
    public boolean canView(String permissionCode) {
        if (currentUser == null) return false;
        if (viewPermissions.contains("*")) return true;
        if (viewPermissions.contains(permissionCode)) return true;
        
        // Parent check: "orders" grants "orders.edit"
        int dotIndex = permissionCode.indexOf('.');
        if (dotIndex > 0) {
            return viewPermissions.contains(permissionCode.substring(0, dotIndex));
        }
        return false;
    }

    public boolean canDo(String actionCode) {
        if (currentUser == null) return false;
        if (actionPermissions.contains("*")) return true;
        if (actionPermissions.contains(actionCode)) return true;

        int dotIndex = actionCode.indexOf('.');
        if (dotIndex > 0) {
            return actionPermissions.contains(actionCode.substring(0, dotIndex));
        }
        return false;
    }
}

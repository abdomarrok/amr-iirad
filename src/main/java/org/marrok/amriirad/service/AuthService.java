package org.marrok.amriirad.service;

import org.marrok.amriirad.core.AppContext;

/**
 * Service to manage authentication and user context.
 */
public class AuthService {
    
    private final org.marrok.amriirad.repository.UserRepository userRepository;

    public AuthService(org.marrok.amriirad.repository.UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String getCurrentUsername() {
        String user = AppContext.getInstance().getCurrentUser();
        return user != null ? user : "admin";
    }

    public boolean login(String username, String password) {
        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            var user = userOpt.get();
            // Simple check (password hashing recommended for prod)
            if (user.getPassword().equals(password)) {
                AppContext.getInstance().setCurrentUser(username);
                return true;
            }
        }
        return false;
    }

    public void logout() {
        AppContext.getInstance().setCurrentUser(null);
    }
}

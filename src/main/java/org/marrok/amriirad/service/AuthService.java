package org.marrok.amriirad.service;

import org.marrok.amriirad.core.AppContext;

/**
 * Service to manage authentication and user context.
 */
public class AuthService {
    
    public String getCurrentUsername() {
        String user = AppContext.getInstance().getCurrentUser();
        return user != null ? user : "admin"; // Fallback for transition
    }

    public boolean login(String username, String password) {
        // Simple authentication for now
        // In a real app, this would query the DB
        if ("admin".equals(username) && "admin".equals(password)) {
            AppContext.getInstance().setCurrentUser(username);
            return true;
        }
        return false;
    }

    public void logout() {
        AppContext.getInstance().setCurrentUser(null);
    }
}

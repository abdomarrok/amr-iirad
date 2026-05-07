package org.marrok.amriirad.service;

import org.marrok.amriirad.core.AppContext;

/**
 * Service to manage authentication and user context.
 */
public class AuthService {
    
    public String getCurrentUsername() {
        return AppContext.getInstance().getCurrentUser();
    }
    
    // Future: handle login/logout here
}

package org.marrok.amriirad.util;

import java.util.prefs.Preferences;

/**
 * Manages persistent application settings using Java Preferences API.
 * Settings survive application restarts.
 */
public class AppSettings {

    private static final Preferences prefs = Preferences.userNodeForPackage(AppSettings.class);
    private static final String KEY_APP_MODE   = "app_mode";
    private static final String KEY_DB_HOST    = "db_host";
    private static final String KEY_DB_PORT    = "db_port";
    private static final String KEY_DB_USER    = "db_user";
    private static final String KEY_DB_PASS    = "db_pass";
    private static final String KEY_DB_NAME    = "db_name";

    /** In-memory current mode (set after initialization). */
    private static AppMode currentMode;

    // -------------------------------------------------------------------------
    // Mode management
    // -------------------------------------------------------------------------

    /**
     * Returns the persisted mode from preferences.
     * Returns null if the app has never been configured (first run).
     */
    public static AppMode getSavedAppMode() {
        String saved = prefs.get(KEY_APP_MODE, null);
        if (saved == null) return null;
        try { return AppMode.valueOf(saved); }
        catch (IllegalArgumentException e) { return null; }
    }

    /**
     * Returns true if the user has already chosen Local or Server mode.
     * Used by AmrIiradApp to decide whether to show ModeSelectionView.
     */
    public static boolean isModeConfigured() {
        return getSavedAppMode() != null;
    }

    /** Returns the in-memory current mode (set after initialization). */
    public static AppMode getAppMode() {
        if (currentMode == null) currentMode = getSavedAppMode();
        return currentMode;
    }

    /**
     * Persists the chosen mode to preferences and sets the in-memory value.
     */
    public static void setAppMode(AppMode mode) {
        currentMode = mode;
        prefs.put(KEY_APP_MODE, mode.name());
    }

    // -------------------------------------------------------------------------
    // Server connection parameters (for SERVER mode)
    // -------------------------------------------------------------------------

    public static void saveServerConfig(String host, int port, String user, String pass, String dbName) {
        prefs.put(KEY_DB_HOST, host);
        prefs.putInt(KEY_DB_PORT, port);
        prefs.put(KEY_DB_USER, user);
        prefs.put(KEY_DB_PASS, pass);
        prefs.put(KEY_DB_NAME, dbName);
    }

    public static String getDbHost()  { return prefs.get(KEY_DB_HOST, "localhost"); }
    public static int    getDbPort()  { return prefs.getInt(KEY_DB_PORT, 3306); }
    public static String getDbUser()  { return prefs.get(KEY_DB_USER, "root"); }
    public static String getDbPass()  { return prefs.get(KEY_DB_PASS, ""); }
    public static String getDbName()  { return prefs.get(KEY_DB_NAME, "amr_iirad"); }

    /** Clears all saved settings (useful for reset / re-configuration). */
    public static void clearAll() {
        try { prefs.clear(); currentMode = null; }
        catch (Exception e) { /* ignore */ }
    }
}

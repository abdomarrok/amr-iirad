package org.marrok.amriirad.model;

public enum PrintLanguage {
    ARABIC("العربية", "ar"),
    FRENCH("Français", "fr");

    private final String label;
    private final String code;

    PrintLanguage(String label, String code) {
        this.label = label;
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public String getCode() {
        return code;
    }
}

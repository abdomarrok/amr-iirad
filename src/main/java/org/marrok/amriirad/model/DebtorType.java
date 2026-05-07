package org.marrok.amriirad.model;

/** Type of debtor (المدين). */
public enum DebtorType {
    INDIVIDUAL("شخص طبيعي"),
    COMPANY("شخص معنوي (شركة)"),
    STATE_ENTITY("هيئة عمومية");

    private final String arabicLabel;

    DebtorType(String arabicLabel) {
        this.arabicLabel = arabicLabel;
    }

    /** Returns the Arabic display label for this debtor type. */
    public String getArabicLabel() {
        return arabicLabel;
    }
}

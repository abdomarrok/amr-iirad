package org.marrok.amriirad.model;

/**
 * Lifecycle states for a Revenue Order (أمر الإيراد).
 * Transitions: DRAFT → ISSUED → DISPATCHED
 *              ISSUED → CANCELLED (full cancel)
 *              ISSUED → REDUCED   (partial reduction)
 */
public enum OrderStatus {
    DRAFT("مسودة"),
    ISSUED("مُصدر"),
    DISPATCHED("مُرسل"),
    CANCELLED("مُلغى"),
    REDUCED("مُخفض");

    private final String arabicLabel;

    OrderStatus(String arabicLabel) {
        this.arabicLabel = arabicLabel;
    }

    /** Returns the Arabic display label for this status. */
    public String getArabicLabel() {
        return arabicLabel;
    }
}

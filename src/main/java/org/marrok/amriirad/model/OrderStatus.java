package org.marrok.amriirad.model;

/**
 * Lifecycle states for a Revenue Order (أمر الإيراد).
 * Transitions: DRAFT → ISSUED → DISPATCHED
 *              ISSUED → CANCELLED (full cancel)
 *              ISSUED → REDUCED   (partial reduction)
 *              ISSUED → INCREASED (partial increase)
 *              ISSUED → ZERO_VALUE (accepted as non-valeur)
 */
public enum OrderStatus {
    DRAFT("مسودة"),
    ISSUED("مُصدر"),
    DISPATCHED("مُرسل"),
    CANCELLED("مُلغى"),
    REDUCED("مُخفض"),
    INCREASED("مُزاد"),
    ZERO_VALUE("قيمة منعدمة");

    private final String arabicLabel;

    OrderStatus(String arabicLabel) {
        this.arabicLabel = arabicLabel;
    }

    /** Returns the Arabic display label for this status. */
    public String getArabicLabel() {
        return arabicLabel;
    }
}

package org.marrok.amriirad.model;

/** Type of cancellation/reduction applied to a Revenue Order. */
public enum CancellationType {
    FULL_CANCEL, // إلغاء كلي — Annexe 2 (Révisée)
    REDUCTION,   // تخفيض جزئي — Annexe 2 (Révisée)
    INCREASE     // زيادة — Annexe 2 (Révisée)
}

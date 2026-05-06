package org.marrok.amriirad.model;

/**
 * Lifecycle states for a Revenue Order (أمر الإيراد).
 * Transitions: DRAFT → ISSUED → DISPATCHED
 *              ISSUED → CANCELLED (full cancel)
 *              ISSUED → REDUCED   (partial reduction)
 */
public enum OrderStatus {
    DRAFT,
    ISSUED,
    DISPATCHED,
    CANCELLED,
    REDUCED
}

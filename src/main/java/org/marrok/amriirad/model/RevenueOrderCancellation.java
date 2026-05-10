package org.marrok.amriirad.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a Cancellation or Reduction order.
 * Maps to `revenue_order_cancellation` table.
 * Covers Annexe 3 (FULL_CANCEL) and Annexe 4 (REDUCTION).
 */
public class RevenueOrderCancellation {

    private int id;
    private RevenueOrder originalOrder;         // الأمر الأصلي
    private CancellationType cancellationType;  // نوع العملية
    private String cancellationNumber;          // رقم أمر الإلغاء/التخفيض
    private LocalDate cancellationDate;         // تاريخ الإلغاء
    private String reasonAr;                    // سبب الإلغاء أو التخفيض (بالعربية)
    private String reasonFr;                    // Motif (Français)
    private BigDecimal reducedAmount;           // المبلغ المخفَّض (فقط لنوع REDUCTION)
    private String createdBy;
    private LocalDateTime createdAt;

    public RevenueOrderCancellation() {}

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public int getId()                                  { return id; }
    public void setId(int id)                           { this.id = id; }

    public RevenueOrder getOriginalOrder()              { return originalOrder; }
    public void setOriginalOrder(RevenueOrder o)        { this.originalOrder = o; }

    public CancellationType getCancellationType()       { return cancellationType; }
    public void setCancellationType(CancellationType t) { this.cancellationType = t; }

    public String getCancellationNumber()               { return cancellationNumber; }
    public void setCancellationNumber(String n)         { this.cancellationNumber = n; }

    public LocalDate getCancellationDate()              { return cancellationDate; }
    public void setCancellationDate(LocalDate d)        { this.cancellationDate = d; }

    public String getReasonAr()                         { return reasonAr; }
    public void setReasonAr(String r)                   { this.reasonAr = r; }

    public String getReasonFr()                         { return reasonFr; }
    public void setReasonFr(String r)                   { this.reasonFr = r; }

    public BigDecimal getReducedAmount()                { return reducedAmount; }
    public void setReducedAmount(BigDecimal a)          { this.reducedAmount = a; }

    public String getCreatedBy()                        { return createdBy; }
    public void setCreatedBy(String createdBy)          { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt()                         { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)           { this.createdAt = createdAt; }
}

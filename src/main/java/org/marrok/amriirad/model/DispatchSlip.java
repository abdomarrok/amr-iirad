package org.marrok.amriirad.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Dispatch Slip (بوردرو الإرسال).
 * Maps to `dispatch_slip` + `dispatch_slip_order` tables.
 * Covers Annexe 5.
 */
public class DispatchSlip {

    private int id;
    private String slipNumber;             // رقم البوردرو
    private FiscalYear fiscalYear;         // السنة المالية
    private LocalDate dispatchDate;        // تاريخ الإرسال
    private String treasuryRef;            // مرجع أمين الخزينة
    private BigDecimal totalAmount;        // المجموع الكلي
    private String createdBy;
    private LocalDateTime createdAt;
    private List<RevenueOrder> orders = new ArrayList<>();  // الأوامر المدرجة

    public DispatchSlip() {}

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }

    public String getSlipNumber()                   { return slipNumber; }
    public void setSlipNumber(String n)             { this.slipNumber = n; }

    public FiscalYear getFiscalYear()               { return fiscalYear; }
    public void setFiscalYear(FiscalYear fy)        { this.fiscalYear = fy; }

    public LocalDate getDispatchDate()              { return dispatchDate; }
    public void setDispatchDate(LocalDate d)        { this.dispatchDate = d; }

    public String getTreasuryRef()                  { return treasuryRef; }
    public void setTreasuryRef(String r)            { this.treasuryRef = r; }

    public BigDecimal getTotalAmount()              { return totalAmount; }
    public void setTotalAmount(BigDecimal a)        { this.totalAmount = a; }

    public String getCreatedBy()                    { return createdBy; }
    public void setCreatedBy(String createdBy)      { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt()                     { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)       { this.createdAt = createdAt; }

    public List<RevenueOrder> getOrders()           { return orders; }
    public void setOrders(List<RevenueOrder> o)     { this.orders = o; }

    /** Recalculates totalAmount from the list of orders. */
    public void recalculateTotal() {
        this.totalAmount = orders.stream()
                .map(RevenueOrder::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public String toString() { return slipNumber; }
}

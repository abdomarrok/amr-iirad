package org.marrok.amriirad.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a Revenue Order (أمر بالإيراد).
 * Maps to the `revenue_order` table and covers Annexe 1 & 2.
 */
public class RevenueOrder {

    private int id;
    private String orderNumber;         // الرقم التسلسلي (YYYY-NNN)
    private FiscalYear fiscalYear;      // السنة المالية
    private LocalDate issueDate;        // تاريخ الإصدار
    private Debtor debtor;              // المدين
    private BudgetChapter budgetChapter;// محور الميزانية
    private String objectAr;            // موضوع الإيراد (بالعربية)
    private BigDecimal amount;          // المبلغ (DECIMAL 18,2)
    private String amountInWordsAr;     // المبلغ بالحروف (TafqeetJ)
    private OrderStatus status;         // حالة الأمر
    private String createdBy;           // المستخدم المنشئ
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RevenueOrder() {
        this.status = OrderStatus.DRAFT;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }

    public String getOrderNumber()                  { return orderNumber; }
    public void setOrderNumber(String n)            { this.orderNumber = n; }

    public FiscalYear getFiscalYear()               { return fiscalYear; }
    public void setFiscalYear(FiscalYear fy)        { this.fiscalYear = fy; }

    public LocalDate getIssueDate()                 { return issueDate; }
    public void setIssueDate(LocalDate d)           { this.issueDate = d; }

    public Debtor getDebtor()                       { return debtor; }
    public void setDebtor(Debtor debtor)            { this.debtor = debtor; }

    public BudgetChapter getBudgetChapter()         { return budgetChapter; }
    public void setBudgetChapter(BudgetChapter bc)  { this.budgetChapter = bc; }

    public String getObjectAr()                     { return objectAr; }
    public void setObjectAr(String o)               { this.objectAr = o; }

    public BigDecimal getAmount()                   { return amount; }
    public void setAmount(BigDecimal amount)        { this.amount = amount; }

    public String getAmountInWordsAr()              { return amountInWordsAr; }
    public void setAmountInWordsAr(String w)        { this.amountInWordsAr = w; }

    public OrderStatus getStatus()                  { return status; }
    public void setStatus(OrderStatus status)       { this.status = status; }

    public String getCreatedBy()                    { return createdBy; }
    public void setCreatedBy(String createdBy)      { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt()                     { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)       { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt()                     { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)       { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return orderNumber + " — " + (debtor != null ? debtor.getFullName() : "");
    }
}

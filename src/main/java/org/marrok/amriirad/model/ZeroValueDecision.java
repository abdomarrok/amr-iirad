package org.marrok.amriirad.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Decision for Zero Value debts (Annex 6).
 * Maps to `zero_value_decision` table.
 */
public class ZeroValueDecision {

    private int id;
    private String decisionNumber;
    private LocalDate decisionDate;
    private FiscalYear fiscalYear;
    private BigDecimal totalAmount;
    private String createdBy;
    private LocalDateTime createdAt;
    
    private List<ZeroValueOrderDetail> details = new ArrayList<>();

    public ZeroValueDecision() {}

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDecisionNumber() { return decisionNumber; }
    public void setDecisionNumber(String decisionNumber) { this.decisionNumber = decisionNumber; }

    public LocalDate getDecisionDate() { return decisionDate; }
    public void setDecisionDate(LocalDate decisionDate) { this.decisionDate = decisionDate; }

    public FiscalYear getFiscalYear() { return fiscalYear; }
    public void setFiscalYear(FiscalYear fiscalYear) { this.fiscalYear = fiscalYear; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<ZeroValueOrderDetail> getDetails() { return details; }
    public void setDetails(List<ZeroValueOrderDetail> details) { this.details = details; }
}

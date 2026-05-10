package org.marrok.amriirad.dto;

import java.math.BigDecimal;

/**
 * Data Transfer Object for JasperReport data source in Dispatch Slips (Annexe 5).
 */
public class SlipOrderDTO {
    private String orderNumber;
    private String debtorName;
    private BigDecimal amount;
    private String issueDate;

    public SlipOrderDTO() {}

    public SlipOrderDTO(String orderNumber, String debtorName, BigDecimal amount, String issueDate) {
        this.orderNumber = orderNumber;
        this.debtorName = debtorName;
        this.amount = amount;
        this.issueDate = issueDate;
    }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public String getDebtorName() { return debtorName; }
    public void setDebtorName(String debtorName) { this.debtorName = debtorName; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getIssueDate() { return issueDate; }
    public void setIssueDate(String issueDate) { this.issueDate = issueDate; }
}

package org.marrok.amriirad.model;

import java.time.LocalDateTime;

public class AuditLog {
    private Long id;
    private String tableName;
    private Integer recordId;
    private String action;
    private String performedBy;
    private LocalDateTime performedAt;
    private String details;

    public AuditLog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public Integer getRecordId() { return recordId; }
    public void setRecordId(Integer recordId) { this.recordId = recordId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }

    public LocalDateTime getPerformedAt() { return performedAt; }
    public void setPerformedAt(LocalDateTime performedAt) { this.performedAt = performedAt; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}

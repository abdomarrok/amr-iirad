package org.marrok.amriirad.service;

import org.marrok.amriirad.model.AuditLog;
import org.marrok.amriirad.repository.AuditLogRepository;

import java.util.List;

public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAll();
    }

    public List<AuditLog> getLogsForRecord(String tableName, Integer recordId) {
        return auditLogRepository.findByTableAndRecord(tableName, recordId);
    }
}

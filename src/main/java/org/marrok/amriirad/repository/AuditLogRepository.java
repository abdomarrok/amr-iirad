package org.marrok.amriirad.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.model.AuditLog;
import org.marrok.amriirad.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditLogRepository {
    private static final Logger logger = LogManager.getLogger(AuditLogRepository.class);

    public List<AuditLog> findAll() {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM audit_log ORDER BY performed_at DESC LIMIT 500";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                logs.add(mapResultSetToAuditLog(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching audit logs", e);
        }
        return logs;
    }

    public List<AuditLog> findByTableAndRecord(String tableName, Integer recordId) {
        List<AuditLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM audit_log WHERE table_name = ? AND record_id = ? ORDER BY performed_at DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, tableName);
            pstmt.setInt(2, recordId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAuditLog(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching filtered audit logs", e);
        }
        return logs;
    }

    private AuditLog mapResultSetToAuditLog(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setId(rs.getLong("id"));
        log.setTableName(rs.getString("table_name"));
        log.setRecordId(rs.getInt("record_id"));
        log.setAction(rs.getString("action"));
        log.setPerformedBy(rs.getString("performed_by"));
        Timestamp ts = rs.getTimestamp("performed_at");
        if (ts != null) {
            log.setPerformedAt(ts.toLocalDateTime());
        }
        log.setDetails(rs.getString("details"));
        return log;
    }
}

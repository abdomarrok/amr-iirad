package org.marrok.amriirad.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Append-only audit trail service.
 * Logs every INSERT, UPDATE, DELETE action to the audit_log table.
 */
public class AuditService {

    private static final Logger logger = LogManager.getLogger(AuditService.class);

    /**
     * Logs an audit event.
     *
     * @param tableName   The table being acted upon (e.g. "revenue_order")
     * @param recordId    The primary key of the affected record
     * @param action      INSERT, UPDATE, or DELETE
     * @param performedBy The username performing the action
     * @param details     Free-text description of the change
     */
    public void log(String tableName, int recordId, Action action, String performedBy, String details) {
        String sql = "INSERT INTO audit_log (table_name, record_id, action, performed_by, details) VALUES (?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setInt(2, recordId);
            ps.setString(3, action.name());
            ps.setString(4, performedBy);
            ps.setString(5, details);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Audit failures should never crash the app — log and continue
            logger.error("Failed to write audit log for {}.{}: {}", tableName, recordId, e.getMessage());
        }
    }

    public enum Action {
        INSERT, UPDATE, DELETE
    }
}

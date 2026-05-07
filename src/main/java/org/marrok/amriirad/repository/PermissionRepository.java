package org.marrok.amriirad.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.model.Permission;
import org.marrok.amriirad.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for managing system permissions.
 * Reused pattern from GstockDz.
 */
public class PermissionRepository {
    private static final Logger logger = LogManager.getLogger(PermissionRepository.class);

    public List<Permission> findAll() {
        List<Permission> permissions = new ArrayList<>();
        String sql = "SELECT * FROM permission ORDER BY category, code";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                permissions.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all permissions", e);
        }
        return permissions;
    }

    public java.util.Optional<Permission> findById(int id) {
        String sql = "SELECT * FROM permission WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return java.util.Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding permission by ID: {}", id, e);
        }
        return java.util.Optional.empty();
    }

    public java.util.Map<String, List<Permission>> getViewPermissionsGroupedByCategory() {
        return getPermissionsByTypeAndGrouped("VIEW");
    }

    public java.util.Map<String, List<Permission>> getActionPermissionsGroupedByCategory() {
        return getPermissionsByTypeAndGrouped("ACTION");
    }

    private java.util.Map<String, List<Permission>> getPermissionsByTypeAndGrouped(String type) {
        java.util.Map<String, List<Permission>> grouped = new java.util.LinkedHashMap<>();
        String sql = "SELECT * FROM permission WHERE type = ? ORDER BY category, code";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, type);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Permission p = mapResultSet(rs);
                    grouped.computeIfAbsent(p.getCategory(), k -> new ArrayList<>()).add(p);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding grouped permissions by type: {}", type, e);
        }
        return grouped;
    }

    private Permission mapResultSet(ResultSet rs) throws SQLException {
        return new Permission(
            rs.getInt("id"),
            rs.getString("code"),
            rs.getString("type"),
            rs.getString("category"),
            rs.getString("description")
        );
    }
}

package org.marrok.amriirad.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.model.Permission;
import org.marrok.amriirad.model.Role;
import org.marrok.amriirad.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing roles and their associated permissions.
 * Reused pattern from GstockDz.
 */
public class RoleRepository {
    private static final Logger logger = LogManager.getLogger(RoleRepository.class);

    public List<Role> findAll() {
        List<Role> roles = new ArrayList<>();
        String sql = "SELECT * FROM role ORDER BY privilege_level DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                roles.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all roles", e);
        }
        return roles;
    }

    public Optional<Role> findById(int id) {
        String sql = "SELECT * FROM role WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Role role = mapResultSet(rs);
                    role.setPermissions(getPermissionsForRole(id));
                    return Optional.of(role);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding role by ID: {}", id, e);
        }
        return Optional.empty();
    }

    public List<Permission> getPermissionsForRole(int roleId) {
        List<Permission> permissions = new ArrayList<>();
        String sql = """
            SELECT p.* FROM permission p
            JOIN role_permission rp ON p.id = rp.permission_id
            WHERE rp.role_id = ?
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    permissions.add(new Permission(
                        rs.getInt("id"),
                        rs.getString("code"),
                        rs.getString("type"),
                        rs.getString("category"),
                        rs.getString("description")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding permissions for role: {}", roleId, e);
        }
        return permissions;
    }

    public int create(String name, String description, int privilegeLevel) {
        String sql = "INSERT INTO role (name, description, privilege_level, is_protected) VALUES (?, ?, ?, 0)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            pstmt.setString(2, description);
            pstmt.setInt(3, privilegeLevel);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error creating role: {}", name, e);
        }
        return -1;
    }

    public boolean update(int id, String name, String description, int privilegeLevel) {
        String sql = "UPDATE role SET name = ?, description = ?, privilege_level = ? WHERE id = ? AND is_protected = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, description);
            pstmt.setInt(3, privilegeLevel);
            pstmt.setInt(4, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating role: {}", id, e);
        }
        return false;
    }

    public boolean delete(int id) {
        // First delete permissions
        String deletePermsSql = "DELETE FROM role_permission WHERE role_id = ?";
        String deleteRoleSql = "DELETE FROM role WHERE id = ? AND is_protected = 0";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(deletePermsSql);
                 PreparedStatement ps2 = conn.prepareStatement(deleteRoleSql)) {
                ps1.setInt(1, id);
                ps1.executeUpdate();
                ps2.setInt(1, id);
                int deleted = ps2.executeUpdate();
                conn.commit();
                return deleted > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Error deleting role: {}", id, e);
        }
        return false;
    }

    public boolean setPermissionsForRole(int roleId, List<Integer> permissionIds) {
        String deleteSql = "DELETE FROM role_permission WHERE role_id = ?";
        String insertSql = "INSERT INTO role_permission (role_id, permission_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement delStmt = conn.prepareStatement(deleteSql);
                 PreparedStatement insStmt = conn.prepareStatement(insertSql)) {
                
                delStmt.setInt(1, roleId);
                delStmt.executeUpdate();

                for (Integer permId : permissionIds) {
                    insStmt.setInt(1, roleId);
                    insStmt.setInt(2, permId);
                    insStmt.addBatch();
                }
                insStmt.executeBatch();
                
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Error setting permissions for role: {}", roleId, e);
        }
        return false;
    }

    private Role mapResultSet(ResultSet rs) throws SQLException {
        return new Role(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getInt("privilege_level"),
            rs.getBoolean("is_protected")
        );
    }
}

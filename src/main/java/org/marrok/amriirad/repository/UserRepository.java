package org.marrok.amriirad.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.model.User;
import org.marrok.amriirad.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {
    private static final Logger logger = LogManager.getLogger(UserRepository.class);

    public Optional<User> findByUsername(String username) {
        String sql = """
            SELECT u.*, r.name as role_name 
            FROM app_user u 
            JOIN role r ON u.role_id = r.id 
            WHERE u.username = ? AND u.is_active = TRUE
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user by username: {}", username, e);
        }
        return Optional.empty();
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = """
            SELECT u.*, r.name as role_name 
            FROM app_user u 
            JOIN role r ON u.role_id = r.id 
            ORDER BY u.username ASC
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                users.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching all users", e);
        }
        return users;
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM app_user WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Error checking username existence", e);
        }
        return false;
    }

    public int create(User user) {
        String sql = "INSERT INTO app_user (username, password, full_name, role_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword()); // Should be hashed by service
            pstmt.setString(3, user.getFullName());
            pstmt.setInt(4, user.getRoleId());
            
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error creating user", e);
        }
        return -1;
    }

    public boolean update(User user) {
        String sql = "UPDATE app_user SET username = ?, password = ?, full_name = ?, role_id = ?, is_active = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getFullName());
            pstmt.setInt(4, user.getRoleId());
            pstmt.setBoolean(5, user.isActive());
            pstmt.setInt(6, user.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating user", e);
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM app_user WHERE id = ? AND username != 'admin'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting user", e);
        }
        return false;
    }

    private User mapResultSet(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setFullName(rs.getString("full_name"));
        user.setRoleId(rs.getInt("role_id"));
        user.setRoleName(rs.getString("role_name"));
        user.setActive(rs.getBoolean("is_active"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) user.setCreatedAt(ts.toLocalDateTime());
        return user;
    }
}

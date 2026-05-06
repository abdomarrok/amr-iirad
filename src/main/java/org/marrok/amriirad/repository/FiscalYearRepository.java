package org.marrok.amriirad.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.model.FiscalYear;
import org.marrok.amriirad.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC repository for fiscal_year table. */
public class FiscalYearRepository {

    private static final Logger logger = LogManager.getLogger(FiscalYearRepository.class);

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    public List<FiscalYear> findAll() throws SQLException {
        String sql = "SELECT id, year_label, is_active, created_at FROM fiscal_year ORDER BY year_label DESC";
        List<FiscalYear> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Optional<FiscalYear> findActive() throws SQLException {
        String sql = "SELECT id, year_label, is_active, created_at FROM fiscal_year WHERE is_active = TRUE LIMIT 1";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return Optional.of(map(rs));
        }
        return Optional.empty();
    }

    public Optional<FiscalYear> findById(int id) throws SQLException {
        String sql = "SELECT id, year_label, is_active, created_at FROM fiscal_year WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    // -------------------------------------------------------------------------
    // WRITE
    // -------------------------------------------------------------------------

    public FiscalYear save(String yearLabel) throws SQLException {
        String sql = "INSERT INTO fiscal_year (year_label, is_active) VALUES (?, FALSE)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, yearLabel);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    FiscalYear fy = new FiscalYear();
                    fy.setId(keys.getInt(1));
                    fy.setYearLabel(yearLabel);
                    fy.setActive(false);
                    return fy;
                }
            }
        }
        throw new SQLException("Failed to insert fiscal year: " + yearLabel);
    }

    /**
     * Sets the given fiscal year as the active one.
     * Deactivates all others in the same transaction.
     */
    public void setActive(int fiscalYearId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try (Statement stmt = c.createStatement()) {
                stmt.execute("UPDATE fiscal_year SET is_active = FALSE");
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE fiscal_year SET is_active = TRUE WHERE id = ?")) {
                ps.setInt(1, fiscalYearId);
                ps.executeUpdate();
            }
            c.commit();
            logger.info("Fiscal year {} set as active.", fiscalYearId);
        }
    }

    // -------------------------------------------------------------------------
    // MAPPING
    // -------------------------------------------------------------------------

    private FiscalYear map(ResultSet rs) throws SQLException {
        FiscalYear fy = new FiscalYear();
        fy.setId(rs.getInt("id"));
        fy.setYearLabel(rs.getString("year_label"));
        fy.setActive(rs.getBoolean("is_active"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) fy.setCreatedAt(ts.toLocalDateTime());
        return fy;
    }
}

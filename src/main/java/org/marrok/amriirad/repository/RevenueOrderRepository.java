package org.marrok.amriirad.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.model.*;
import org.marrok.amriirad.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC repository for the revenue_order table. */
public class RevenueOrderRepository {

    private static final Logger logger = LogManager.getLogger(RevenueOrderRepository.class);

    private final FiscalYearRepository fyRepo = new FiscalYearRepository();
    private final DebtorRepository debtorRepo = new DebtorRepository();
    private final BudgetChapterRepository bcRepo = new BudgetChapterRepository();

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    public List<RevenueOrder> findAll(int fiscalYearId) throws SQLException {
        String sql = "SELECT * FROM revenue_order WHERE fiscal_year_id = ? AND is_deleted = 0 ORDER BY created_at DESC";
        return query(sql, ps -> ps.setInt(1, fiscalYearId));
    }

    public Optional<RevenueOrder> findById(int id) throws SQLException {
        String sql = "SELECT * FROM revenue_order WHERE id = ? AND is_deleted = 0";
        List<RevenueOrder> r = query(sql, ps -> ps.setInt(1, id));
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    public List<RevenueOrder> findByStatus(int fiscalYearId, OrderStatus status) throws SQLException {
        String sql = "SELECT * FROM revenue_order WHERE fiscal_year_id = ? AND status = ? AND is_deleted = 0";
        return query(sql, ps -> {
            ps.setInt(1, fiscalYearId);
            ps.setString(2, status.name());
        });
    }

    // -------------------------------------------------------------------------
    // WRITE
    // -------------------------------------------------------------------------

    public RevenueOrder save(RevenueOrder ro) throws SQLException {
        String sql = "INSERT INTO revenue_order (order_number, fiscal_year_id, issue_date, debtor_id, budget_chapter_id, " +
                     "object_ar, amount, amount_in_words_ar, status, created_by) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, ro);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) ro.setId(keys.getInt(1));
            }
        }
        logger.info("Saved revenue order id={}", ro.getId());
        return ro;
    }

    public void update(RevenueOrder ro) throws SQLException {
        String sql = "UPDATE revenue_order SET order_number=?, fiscal_year_id=?, issue_date=?, debtor_id=?, budget_chapter_id=?, " +
                     "object_ar=?, amount=?, amount_in_words_ar=?, status=? WHERE id=? AND is_deleted=0";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, ro.getOrderNumber());
            ps.setInt(2, ro.getFiscalYear().getId());
            ps.setDate(3, ro.getIssueDate() != null ? Date.valueOf(ro.getIssueDate()) : null);
            ps.setInt(4, ro.getDebtor().getId());
            ps.setInt(5, ro.getBudgetChapter().getId());
            ps.setString(6, ro.getObjectAr());
            ps.setBigDecimal(7, ro.getAmount());
            ps.setString(8, ro.getAmountInWordsAr());
            ps.setString(9, ro.getStatus().name());
            ps.setInt(10, ro.getId());
            ps.executeUpdate();
        }
        logger.info("Updated revenue order id={}", ro.getId());
    }

    public void updateStatus(int id, OrderStatus status) throws SQLException {
        String sql = "UPDATE revenue_order SET status=? WHERE id=? AND is_deleted=0";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(int id, String deletedBy) throws SQLException {
        String sql = "UPDATE revenue_order SET is_deleted=1, deleted_at=NOW(), deleted_by=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, deletedBy);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    @FunctionalInterface
    private interface Binder { void bind(PreparedStatement ps) throws SQLException; }

    private List<RevenueOrder> query(String sql, Binder binder) throws SQLException {
        List<RevenueOrder> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    private void bind(PreparedStatement ps, RevenueOrder ro) throws SQLException {
        ps.setString(1, ro.getOrderNumber());
        ps.setInt(2, ro.getFiscalYear().getId());
        ps.setDate(3, ro.getIssueDate() != null ? Date.valueOf(ro.getIssueDate()) : null);
        ps.setInt(4, ro.getDebtor().getId());
        ps.setInt(5, ro.getBudgetChapter().getId());
        ps.setString(6, ro.getObjectAr());
        ps.setBigDecimal(7, ro.getAmount());
        ps.setString(8, ro.getAmountInWordsAr());
        ps.setString(9, ro.getStatus().name());
        ps.setString(10, ro.getCreatedBy());
    }

    private RevenueOrder map(ResultSet rs) throws SQLException {
        RevenueOrder ro = new RevenueOrder();
        ro.setId(rs.getInt("id"));
        ro.setOrderNumber(rs.getString("order_number"));
        
        // Lazy-ish loading of linked entities (can be optimized with JOINs later)
        ro.setFiscalYear(fyRepo.findById(rs.getInt("fiscal_year_id")).orElse(null));
        ro.setDebtor(debtorRepo.findById(rs.getInt("debtor_id")).orElse(null));
        ro.setBudgetChapter(bcRepo.findById(rs.getInt("budget_chapter_id")).orElse(null));
        
        Date issueDate = rs.getDate("issue_date");
        if (issueDate != null) ro.setIssueDate(issueDate.toLocalDate());
        
        ro.setObjectAr(rs.getString("object_ar"));
        ro.setAmount(rs.getBigDecimal("amount"));
        ro.setAmountInWordsAr(rs.getString("amount_in_words_ar"));
        ro.setStatus(OrderStatus.valueOf(rs.getString("status")));
        ro.setCreatedBy(rs.getString("created_by"));
        
        Timestamp tsC = rs.getTimestamp("created_at");
        if (tsC != null) ro.setCreatedAt(tsC.toLocalDateTime());
        Timestamp tsU = rs.getTimestamp("updated_at");
        if (tsU != null) ro.setUpdatedAt(tsU.toLocalDateTime());
        
        return ro;
    }
}

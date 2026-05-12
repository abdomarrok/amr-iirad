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

    /**
     * Base JOIN query that eagerly loads fiscal_year, debtor, and budget_chapter
     * in a single round-trip — eliminates the N+1 problem.
     */
    private static final String BASE_SELECT = """
        SELECT ro.*,
               fy.year_label   AS fy_year_label,
               fy.is_active    AS fy_is_active,
               d.full_name     AS d_full_name,
               d.id_number     AS d_id_number,
               d.address       AS d_address,
               d.phone         AS d_phone,
               d.bank_account  AS d_bank_account,
               d.cnas_number   AS d_cnas_number,
               d.nif_number    AS d_nif_number,
               d.debtor_type   AS d_debtor_type,
               bc.code         AS bc_code,
               bc.label_ar     AS bc_label_ar,
               bc.label_fr     AS bc_label_fr,
               bc.parent_id    AS bc_parent_id,
               bc.level        AS bc_level
        FROM revenue_order ro
        JOIN fiscal_year   fy ON ro.fiscal_year_id    = fy.id
        JOIN debtor        d  ON ro.debtor_id         = d.id
        JOIN budget_chapter bc ON ro.budget_chapter_id = bc.id
        """;

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    public List<RevenueOrder> findAll(int fiscalYearId) throws SQLException {
        String sql = BASE_SELECT + " WHERE ro.fiscal_year_id = ? AND ro.is_deleted = 0 ORDER BY ro.created_at DESC";
        return query(sql, ps -> ps.setInt(1, fiscalYearId));
    }

    public Optional<RevenueOrder> findById(int id) throws SQLException {
        String sql = BASE_SELECT + " WHERE ro.id = ? AND ro.is_deleted = 0";
        List<RevenueOrder> r = query(sql, ps -> ps.setInt(1, id));
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    public List<RevenueOrder> findByStatus(int fiscalYearId, OrderStatus status) throws SQLException {
        String sql = BASE_SELECT + " WHERE ro.fiscal_year_id = ? AND ro.status = ? AND ro.is_deleted = 0";
        return query(sql, ps -> {
            ps.setInt(1, fiscalYearId);
            ps.setString(2, status.name());
        });
    }

    public boolean isChapterInUse(int chapterId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM revenue_order WHERE budget_chapter_id = ? AND is_deleted = 0";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, chapterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public boolean isDebtorInUse(int debtorId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM revenue_order WHERE debtor_id = ? AND is_deleted = 0";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, debtorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // WRITE
    // -------------------------------------------------------------------------

    public RevenueOrder save(RevenueOrder ro) throws SQLException {
        String sql = "INSERT INTO revenue_order (order_number, fiscal_year_id, issue_date, debtor_id, budget_chapter_id, " +
                     "object_ar, object_fr, amount, amount_in_words_ar, amount_in_words_fr, status, created_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindInsert(ps, ro);
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
                     "object_ar=?, object_fr=?, amount=?, amount_in_words_ar=?, amount_in_words_fr=?, status=? WHERE id=? AND is_deleted=0";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, ro.getOrderNumber());
            ps.setInt(2, ro.getFiscalYear().getId());
            ps.setDate(3, ro.getIssueDate() != null ? Date.valueOf(ro.getIssueDate()) : null);
            ps.setInt(4, ro.getDebtor().getId());
            ps.setInt(5, ro.getBudgetChapter().getId());
            ps.setString(6, ro.getObjectAr());
            ps.setString(7, ro.getObjectFr());
            ps.setBigDecimal(8, ro.getAmount());
            ps.setString(9, ro.getAmountInWordsAr());
            ps.setString(10, ro.getAmountInWordsFr());
            ps.setString(11, ro.getStatus().name());
            ps.setInt(12, ro.getId());
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

    private void bindInsert(PreparedStatement ps, RevenueOrder ro) throws SQLException {
        ps.setString(1, ro.getOrderNumber());
        ps.setInt(2, ro.getFiscalYear().getId());
        ps.setDate(3, ro.getIssueDate() != null ? Date.valueOf(ro.getIssueDate()) : null);
        ps.setInt(4, ro.getDebtor().getId());
        ps.setInt(5, ro.getBudgetChapter().getId());
        ps.setString(6, ro.getObjectAr());
        ps.setString(7, ro.getObjectFr());
        ps.setBigDecimal(8, ro.getAmount());
        ps.setString(9, ro.getAmountInWordsAr());
        ps.setString(10, ro.getAmountInWordsFr());
        ps.setString(11, ro.getStatus().name());
        ps.setString(12, ro.getCreatedBy());
    }

    /**
     * Maps a ResultSet row to a RevenueOrder with all relationships
     * populated inline from the JOIN — no additional queries needed.
     */
    private RevenueOrder map(ResultSet rs) throws SQLException {
        RevenueOrder ro = new RevenueOrder();
        ro.setId(rs.getInt("id"));
        ro.setOrderNumber(rs.getString("order_number"));

        // Fiscal Year — inline from JOIN
        FiscalYear fy = new FiscalYear();
        fy.setId(rs.getInt("fiscal_year_id"));
        fy.setYearLabel(rs.getString("fy_year_label"));
        fy.setActive(rs.getBoolean("fy_is_active"));
        ro.setFiscalYear(fy);

        // Debtor — inline from JOIN
        Debtor debtor = new Debtor();
        debtor.setId(rs.getInt("debtor_id"));
        debtor.setFullName(rs.getString("d_full_name"));
        debtor.setIdNumber(rs.getString("d_id_number"));
        debtor.setAddress(rs.getString("d_address"));
        debtor.setPhone(rs.getString("d_phone"));
        debtor.setBankAccount(rs.getString("d_bank_account"));
        debtor.setCnasNumber(rs.getString("d_cnas_number"));
        debtor.setNifNumber(rs.getString("d_nif_number"));
        String debtorTypeStr = rs.getString("d_debtor_type");
        if (debtorTypeStr != null) debtor.setDebtorType(DebtorType.valueOf(debtorTypeStr));
        ro.setDebtor(debtor);

        // Budget Chapter — inline from JOIN
        BudgetChapter bc = new BudgetChapter();
        bc.setId(rs.getInt("budget_chapter_id"));
        bc.setCode(rs.getString("bc_code"));
        bc.setLabelAr(rs.getString("bc_label_ar"));
        bc.setLabelFr(rs.getString("bc_label_fr"));
        int parentId = rs.getInt("bc_parent_id");
        bc.setParentId(rs.wasNull() ? null : parentId);
        bc.setLevel(rs.getInt("bc_level"));
        ro.setBudgetChapter(bc);

        // Scalar fields
        Date issueDate = rs.getDate("issue_date");
        if (issueDate != null) ro.setIssueDate(issueDate.toLocalDate());

        ro.setObjectAr(rs.getString("object_ar"));
        ro.setObjectFr(rs.getString("object_fr"));
        ro.setAmount(rs.getBigDecimal("amount"));
        ro.setAmountInWordsAr(rs.getString("amount_in_words_ar"));
        ro.setAmountInWordsFr(rs.getString("amount_in_words_fr"));
        
        String statusStr = rs.getString("status");
        try {
            ro.setStatus(statusStr != null && !statusStr.isEmpty() ? OrderStatus.valueOf(statusStr) : OrderStatus.DRAFT);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid status found in DB: '{}' for order ID: {}. Defaulting to DRAFT.", statusStr, ro.getId());
            ro.setStatus(OrderStatus.DRAFT);
        }
        
        ro.setCreatedBy(rs.getString("created_by"));

        Timestamp tsC = rs.getTimestamp("created_at");
        if (tsC != null) ro.setCreatedAt(tsC.toLocalDateTime());
        Timestamp tsU = rs.getTimestamp("updated_at");
        if (tsU != null) ro.setUpdatedAt(tsU.toLocalDateTime());

        return ro;
    }
}

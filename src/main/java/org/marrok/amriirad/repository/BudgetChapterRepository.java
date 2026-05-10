package org.marrok.amriirad.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.model.BudgetChapter;
import org.marrok.amriirad.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC repository for the budget_chapter table. */
public class BudgetChapterRepository {

    private static final Logger logger = LogManager.getLogger(BudgetChapterRepository.class);

    public List<BudgetChapter> findAll(int fiscalYearId) throws SQLException {
        return query("SELECT * FROM budget_chapter WHERE fiscal_year_id = ? ORDER BY code ASC", ps -> ps.setInt(1, fiscalYearId));
    }

    public List<BudgetChapter> findByLevel(int fiscalYearId, int level) throws SQLException {
        return query("SELECT * FROM budget_chapter WHERE fiscal_year_id = ? AND level = ? ORDER BY code", ps -> {
            ps.setInt(1, fiscalYearId);
            ps.setInt(2, level);
        });
    }

    public List<BudgetChapter> findChildren(int parentId) throws SQLException {
        return query("SELECT * FROM budget_chapter WHERE parent_id = ? ORDER BY code", ps -> ps.setInt(1, parentId));
    }

    public Optional<BudgetChapter> findById(int id) throws SQLException {
        List<BudgetChapter> r = query("SELECT * FROM budget_chapter WHERE id = ?", ps -> ps.setInt(1, id));
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    public BudgetChapter save(BudgetChapter bc) throws SQLException {
        if (bc.getId() > 0)
            return update(bc);

        String sql = "INSERT INTO budget_chapter (code, label_ar, label_fr, parent_id, fiscal_year_id, level) VALUES (?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, bc.getCode());
            ps.setString(2, bc.getLabelAr());
            ps.setString(3, bc.getLabelFr());
            if (bc.getParentId() != null)
                ps.setInt(4, bc.getParentId());
            else
                ps.setNull(4, Types.INTEGER);
            ps.setInt(5, bc.getFiscalYearId());
            ps.setInt(6, bc.getLevel());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next())
                    bc.setId(keys.getInt(1));
            }
        }
        logger.info("Saved budget chapter: {} for year {}", bc.getCode(), bc.getFiscalYearId());
        return bc;
    }

    public BudgetChapter update(BudgetChapter bc) throws SQLException {
        String sql = "UPDATE budget_chapter SET code=?, label_ar=?, label_fr=?, parent_id=?, fiscal_year_id=?, level=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, bc.getCode());
            ps.setString(2, bc.getLabelAr());
            ps.setString(3, bc.getLabelFr());
            if (bc.getParentId() != null)
                ps.setInt(4, bc.getParentId());
            else
                ps.setNull(4, Types.INTEGER);
            ps.setInt(5, bc.getFiscalYearId());
            ps.setInt(6, bc.getLevel());
            ps.setInt(7, bc.getId());
            ps.executeUpdate();
        }
        logger.info("Updated budget chapter: {} for year {}", bc.getCode(), bc.getFiscalYearId());
        return bc;
    }

    public void delete(int id) throws SQLException {
        // First check if it has children
        List<BudgetChapter> children = findChildren(id);
        if (!children.isEmpty()) {
            throw new SQLException("لا يمكن حذف هذا البند لأنه يحتوي على بنود فرعية.");
        }

        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement ps = c.prepareStatement("DELETE FROM budget_chapter WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        logger.info("Deleted budget chapter ID: {}", id);
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private List<BudgetChapter> query(String sql, Binder binder) throws SQLException {
        List<BudgetChapter> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    list.add(map(rs));
            }
        }
        return list;
    }

    private BudgetChapter map(ResultSet rs) throws SQLException {
        BudgetChapter bc = new BudgetChapter();
        bc.setId(rs.getInt("id"));
        bc.setCode(rs.getString("code"));
        bc.setLabelAr(rs.getString("label_ar"));
        bc.setLabelFr(rs.getString("label_fr"));
        int parentId = rs.getInt("parent_id");
        bc.setParentId(rs.wasNull() ? null : parentId);
        bc.setFiscalYearId(rs.getInt("fiscal_year_id"));
        bc.setLevel(rs.getInt("level"));
        return bc;
    }
}

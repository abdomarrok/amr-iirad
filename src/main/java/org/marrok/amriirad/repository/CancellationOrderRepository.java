package org.marrok.amriirad.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.model.CancellationType;
import org.marrok.amriirad.model.RevenueOrderCancellation;
import org.marrok.amriirad.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC repository for the revenue_order_cancellation table. */
public class CancellationOrderRepository {

    private static final Logger logger = LogManager.getLogger(CancellationOrderRepository.class);
    private final RevenueOrderRepository orderRepo = new RevenueOrderRepository();

    public Optional<RevenueOrderCancellation> findByOrderId(int orderId) throws SQLException {
        String sql = "SELECT * FROM revenue_order_cancellation WHERE original_order_id = ? AND is_deleted = 0";
        List<RevenueOrderCancellation> r = query(sql, ps -> ps.setInt(1, orderId));
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    public RevenueOrderCancellation save(RevenueOrderCancellation roc) throws SQLException {
        String sql = "INSERT INTO revenue_order_cancellation (original_order_id, cancellation_type, cancellation_number, " +
                     "cancellation_date, reason_ar, reason_fr, reduced_amount, created_by) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, roc.getOriginalOrder().getId());
            ps.setString(2, roc.getCancellationType().name());
            ps.setString(3, roc.getCancellationNumber());
            ps.setDate(4, Date.valueOf(roc.getCancellationDate()));
            ps.setString(5, roc.getReasonAr());
            ps.setString(6, roc.getReasonFr());
            ps.setBigDecimal(7, roc.getReducedAmount());
            ps.setString(8, roc.getCreatedBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) roc.setId(keys.getInt(1));
            }
        }
        logger.info("Saved cancellation id={} for order id={}", roc.getId(), roc.getOriginalOrder().getId());
        return roc;
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    @FunctionalInterface
    private interface Binder { void bind(PreparedStatement ps) throws SQLException; }

    private List<RevenueOrderCancellation> query(String sql, Binder binder) throws SQLException {
        List<RevenueOrderCancellation> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    private RevenueOrderCancellation map(ResultSet rs) throws SQLException {
        RevenueOrderCancellation roc = new RevenueOrderCancellation();
        roc.setId(rs.getInt("id"));
        roc.setOriginalOrder(orderRepo.findById(rs.getInt("original_order_id")).orElse(null));
        String typeStr = rs.getString("cancellation_type");
        try {
            roc.setCancellationType(typeStr != null ? CancellationType.valueOf(typeStr) : CancellationType.FULL_CANCEL);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid cancellation type in DB: {}. Defaulting to FULL_CANCEL.", typeStr);
            roc.setCancellationType(CancellationType.FULL_CANCEL);
        }
        roc.setCancellationNumber(rs.getString("cancellation_number"));
        roc.setCancellationDate(rs.getDate("cancellation_date").toLocalDate());
        roc.setReasonAr(rs.getString("reason_ar"));
        roc.setReasonFr(rs.getString("reason_fr"));
        roc.setReducedAmount(rs.getBigDecimal("reduced_amount"));
        roc.setCreatedBy(rs.getString("created_by"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) roc.setCreatedAt(ts.toLocalDateTime());
        return roc;
    }
}

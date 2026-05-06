package org.marrok.amriirad.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.model.DispatchSlip;
import org.marrok.amriirad.model.RevenueOrder;
import org.marrok.amriirad.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC repository for dispatch_slip and dispatch_slip_order tables. */
public class DispatchSlipRepository {

    private static final Logger logger = LogManager.getLogger(DispatchSlipRepository.class);
    private final FiscalYearRepository fyRepo = new FiscalYearRepository();
    private final RevenueOrderRepository orderRepo = new RevenueOrderRepository();

    public List<DispatchSlip> findAll(int fiscalYearId) throws SQLException {
        String sql = "SELECT * FROM dispatch_slip WHERE fiscal_year_id = ? AND is_deleted = 0 ORDER BY created_at DESC";
        return query(sql, ps -> ps.setInt(1, fiscalYearId));
    }

    public Optional<DispatchSlip> findById(int id) throws SQLException {
        String sql = "SELECT * FROM dispatch_slip WHERE id = ? AND is_deleted = 0";
        List<DispatchSlip> r = query(sql, ps -> ps.setInt(1, id));
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    public DispatchSlip save(DispatchSlip ds) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                String sql = "INSERT INTO dispatch_slip (slip_number, fiscal_year_id, dispatch_date, treasury_ref, total_amount, created_by) VALUES (?,?,?,?,?,?)";
                try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, ds.getSlipNumber());
                    ps.setInt(2, ds.getFiscalYear().getId());
                    ps.setDate(3, ds.getDispatchDate() != null ? Date.valueOf(ds.getDispatchDate()) : null);
                    ps.setString(4, ds.getTreasuryRef());
                    ps.setBigDecimal(5, ds.getTotalAmount());
                    ps.setString(6, ds.getCreatedBy());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) ds.setId(keys.getInt(1));
                    }
                }
                saveOrders(c, ds);
                c.commit();
                logger.info("Saved dispatch slip id={}", ds.getId());
                return ds;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private void saveOrders(Connection c, DispatchSlip ds) throws SQLException {
        String sql = "INSERT INTO dispatch_slip_order (slip_id, order_id) VALUES (?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (RevenueOrder ro : ds.getOrders()) {
                ps.setInt(1, ds.getId());
                ps.setInt(2, ro.getId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    @FunctionalInterface
    private interface Binder { void bind(PreparedStatement ps) throws SQLException; }

    private List<DispatchSlip> query(String sql, Binder binder) throws SQLException {
        List<DispatchSlip> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    private DispatchSlip map(ResultSet rs) throws SQLException {
        DispatchSlip ds = new DispatchSlip();
        ds.setId(rs.getInt("id"));
        ds.setSlipNumber(rs.getString("slip_number"));
        ds.setFiscalYear(fyRepo.findById(rs.getInt("fiscal_year_id")).orElse(null));
        Date dispatchDate = rs.getDate("dispatch_date");
        if (dispatchDate != null) ds.setDispatchDate(dispatchDate.toLocalDate());
        ds.setTreasuryRef(rs.getString("treasury_ref"));
        ds.setTotalAmount(rs.getBigDecimal("total_amount"));
        ds.setCreatedBy(rs.getString("created_by"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) ds.setCreatedAt(ts.toLocalDateTime());
        
        ds.setOrders(fetchOrders(ds.getId()));
        return ds;
    }

    private List<RevenueOrder> fetchOrders(int slipId) throws SQLException {
        List<RevenueOrder> orders = new ArrayList<>();
        String sql = "SELECT order_id FROM dispatch_slip_order WHERE slip_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, slipId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orderRepo.findById(rs.getInt("order_id")).ifPresent(orders::add);
                }
            }
        }
        return orders;
    }
}

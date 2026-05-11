package org.marrok.amriirad.repository;

import org.marrok.amriirad.model.*;
import org.marrok.amriirad.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ZeroValueRepository {

    public ZeroValueDecision save(ZeroValueDecision decision) throws SQLException {
        String sqlDecision = "INSERT INTO zero_value_decision (decision_number, decision_date, fiscal_year_id, total_amount, created_by) VALUES (?,?,?,?,?)";
        String sqlDetail = "INSERT INTO zero_value_order_details (decision_id, revenue_order_id, non_collection_reasons, enforcement_actions, deliberative_opinion, accountant_observations) VALUES (?,?,?,?,?,?)";

        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(sqlDecision, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, decision.getDecisionNumber());
                    ps.setDate(2, Date.valueOf(decision.getDecisionDate()));
                    ps.setInt(3, decision.getFiscalYear().getId());
                    ps.setBigDecimal(4, decision.getTotalAmount());
                    ps.setString(5, decision.getCreatedBy());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) decision.setId(rs.getInt(1));
                    }
                }

                try (PreparedStatement ps = c.prepareStatement(sqlDetail)) {
                    for (ZeroValueOrderDetail detail : decision.getDetails()) {
                        ps.setInt(1, decision.getId());
                        ps.setInt(2, detail.getRevenueOrder().getId());
                        ps.setString(3, detail.getNonCollectionReasons());
                        ps.setString(4, detail.getEnforcementActions());
                        ps.setString(5, detail.getDeliberativeOpinion());
                        ps.setString(6, detail.getAccountantObservations());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
        return decision;
    }

    public List<ZeroValueDecision> findAll(int fiscalYearId) throws SQLException {
        List<ZeroValueDecision> list = new ArrayList<>();
        String sql = "SELECT * FROM zero_value_decision WHERE fiscal_year_id = ? ORDER BY created_at DESC";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, fiscalYearId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ZeroValueDecision d = new ZeroValueDecision();
                    d.setId(rs.getInt("id"));
                    d.setDecisionNumber(rs.getString("decision_number"));
                    d.setDecisionDate(rs.getDate("decision_date").toLocalDate());
                    d.setTotalAmount(rs.getBigDecimal("total_amount"));
                    list.add(d);
                }
            }
        }
        return list;
    }

    public void loadDetails(ZeroValueDecision decision) throws SQLException {
        List<ZeroValueOrderDetail> details = new ArrayList<>();
        String sql = """
            SELECT d.*, 
                   ro.order_number AS ro_num, 
                   ro.amount AS ro_amount, 
                   deb.full_name AS deb_name
            FROM zero_value_order_details d
            JOIN revenue_order ro ON d.revenue_order_id = ro.id
            JOIN debtor deb ON ro.debtor_id = deb.id
            WHERE d.decision_id = ?
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, decision.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ZeroValueOrderDetail detail = new ZeroValueOrderDetail();
                    detail.setId(rs.getInt("id"));
                    detail.setDecisionId(rs.getInt("decision_id"));
                    detail.setNonCollectionReasons(rs.getString("non_collection_reasons"));
                    detail.setEnforcementActions(rs.getString("enforcement_actions"));
                    detail.setDeliberativeOpinion(rs.getString("deliberative_opinion"));
                    detail.setAccountantObservations(rs.getString("accountant_observations"));

                    RevenueOrder ro = new RevenueOrder();
                    ro.setId(rs.getInt("revenue_order_id"));
                    ro.setOrderNumber(rs.getString("ro_num"));
                    ro.setAmount(rs.getBigDecimal("ro_amount"));
                    
                    Debtor debtor = new Debtor();
                    debtor.setFullName(rs.getString("deb_name"));
                    ro.setDebtor(debtor);
                    
                    detail.setRevenueOrder(ro);
                    details.add(detail);
                }
            }
        }
        decision.setDetails(details);
    }
}

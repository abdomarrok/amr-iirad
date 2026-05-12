package org.marrok.amriirad.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.model.Debtor;
import org.marrok.amriirad.model.DebtorType;
import org.marrok.amriirad.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC repository for the debtor table. */
public class DebtorRepository {

    private static final Logger logger = LogManager.getLogger(DebtorRepository.class);

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    public List<Debtor> findAll() throws SQLException {
        return query("SELECT * FROM debtor ORDER BY full_name ASC", ps -> {});
    }

    public Optional<Debtor> findById(int id) throws SQLException {
        List<Debtor> r = query("SELECT * FROM debtor WHERE id = ?", ps -> ps.setInt(1, id));
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    /**
     * Case-insensitive search against full_name, id_number, and phone.
     */
    public List<Debtor> search(String query) throws SQLException {
        String pattern = "%" + query + "%";
        return this.query(
                "SELECT * FROM debtor WHERE full_name LIKE ? OR id_number LIKE ? OR phone LIKE ? ORDER BY full_name",
                ps -> { ps.setString(1, pattern); ps.setString(2, pattern); ps.setString(3, pattern); }
        );
    }

    // -------------------------------------------------------------------------
    // WRITE
    // -------------------------------------------------------------------------

    public Debtor save(Debtor d) throws SQLException {
        String sql = "INSERT INTO debtor (full_name, id_number, address, phone, bank_account, cnas_number, nif_number, nis_number, debtor_type) VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, d);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) d.setId(keys.getInt(1));
            }
        }
        logger.info("Saved debtor id={}", d.getId());
        return d;
    }

    public void update(Debtor d) throws SQLException {
        String sql = "UPDATE debtor SET full_name=?, id_number=?, address=?, phone=?, bank_account=?, cnas_number=?, nif_number=?, nis_number=?, debtor_type=? WHERE id=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
             bind(ps, d);
            ps.setInt(10, d.getId());
            ps.executeUpdate();
        }
        logger.info("Updated debtor id={}", d.getId());
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM debtor WHERE id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        logger.info("Deleted debtor id={}", id);
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    @FunctionalInterface
    private interface Binder { void bind(PreparedStatement ps) throws SQLException; }

    private List<Debtor> query(String sql, Binder binder) throws SQLException {
        List<Debtor> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    private void bind(PreparedStatement ps, Debtor d) throws SQLException {
        ps.setString(1, d.getFullName());
        ps.setString(2, d.getIdNumber());
        ps.setString(3, d.getAddress());
        ps.setString(4, d.getPhone());
        ps.setString(5, d.getBankAccount());
        ps.setString(6, d.getCnasNumber());
        ps.setString(7, d.getNifNumber());
        ps.setString(8, d.getNisNumber());
        ps.setString(9, d.getDebtorType().name());
    }

    private Debtor map(ResultSet rs) throws SQLException {
        Debtor d = new Debtor();
        d.setId(rs.getInt("id"));
        d.setFullName(rs.getString("full_name"));
        d.setIdNumber(rs.getString("id_number"));
        d.setAddress(rs.getString("address"));
        d.setPhone(rs.getString("phone"));
        d.setBankAccount(rs.getString("bank_account"));
        d.setCnasNumber(rs.getString("cnas_number"));
        d.setNifNumber(rs.getString("nif_number"));
        d.setNisNumber(rs.getString("nis_number"));
        d.setDebtorType(DebtorType.valueOf(rs.getString("debtor_type")));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) d.setCreatedAt(ts.toLocalDateTime());
        return d;
    }
}

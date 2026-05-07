package org.marrok.amriirad.repository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.amriirad.model.InstitutionInfo;
import org.marrok.amriirad.util.DatabaseConnection;

import java.sql.*;

public class InstitutionRepository {
    private static final Logger logger = LogManager.getLogger(InstitutionRepository.class);

    public InstitutionInfo getInfo() {
        String sql = "SELECT * FROM institution_info WHERE id = 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return mapResultSet(rs);
            }
        } catch (SQLException e) {
            logger.error("Error fetching institution info", e);
        }
        return null;
    }

    public void update(InstitutionInfo info) {
        String sql = """
            UPDATE institution_info SET
                name_ar = ?, name_fr = ?, authorizing_officer_ar = ?,
                treasury_account_ar = ?, rib_number = ?, logo_path = ?,
                address_ar = ?, wilaya_ar = ?
            WHERE id = 1
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, info.getNameAr());
            pstmt.setString(2, info.getNameFr());
            pstmt.setString(3, info.getAuthorizingOfficerAr());
            pstmt.setString(4, info.getTreasuryAccountAr());
            pstmt.setString(5, info.getRibNumber());
            pstmt.setString(6, info.getLogoPath());
            pstmt.setString(7, info.getAddressAr());
            pstmt.setString(8, info.getWilayaAr());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating institution info", e);
        }
    }

    private InstitutionInfo mapResultSet(ResultSet rs) throws SQLException {
        InstitutionInfo info = new InstitutionInfo();
        info.setId(rs.getInt("id"));
        info.setNameAr(rs.getString("name_ar"));
        info.setNameFr(rs.getString("name_fr"));
        info.setAuthorizingOfficerAr(rs.getString("authorizing_officer_ar"));
        info.setTreasuryAccountAr(rs.getString("treasury_account_ar"));
        info.setRibNumber(rs.getString("rib_number"));
        info.setLogoPath(rs.getString("logo_path"));
        info.setAddressAr(rs.getString("address_ar"));
        info.setWilayaAr(rs.getString("wilaya_ar"));
        Timestamp ts = rs.getTimestamp("last_updated_at");
        if (ts != null) info.setLastUpdatedAt(ts.toLocalDateTime());
        return info;
    }
}

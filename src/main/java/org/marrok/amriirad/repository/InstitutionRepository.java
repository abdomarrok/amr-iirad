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
                name_ar = ?, name_fr = ?, 
                ministry_name_ar = ?, ministry_name_fr = ?,
                ordonnateur_code = ?,
                authorizing_officer_ar = ?, authorizing_officer_fr = ?,
                treasury_account_ar = ?, treasury_name_ar = ?, treasury_name_fr = ?,
                rib_number = ?, logo_path = ?,
                address_ar = ?, address_fr = ?, wilaya_ar = ?, wilaya_fr = ?
            WHERE id = 1
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, info.getNameAr());
            pstmt.setString(2, info.getNameFr());
            pstmt.setString(3, info.getMinistryNameAr());
            pstmt.setString(4, info.getMinistryNameFr());
            pstmt.setString(5, info.getOrdonnateurCode());
            pstmt.setString(6, info.getAuthorizingOfficerAr());
            pstmt.setString(7, info.getAuthorizingOfficerFr());
            pstmt.setString(8, info.getTreasuryAccountAr());
            pstmt.setString(9, info.getTreasuryNameAr());
            pstmt.setString(10, info.getTreasuryNameFr());
            pstmt.setString(11, info.getRibNumber());
            pstmt.setString(12, info.getLogoPath());
            pstmt.setString(13, info.getAddressAr());
            pstmt.setString(14, info.getAddressFr());
            pstmt.setString(15, info.getWilayaAr());
            pstmt.setString(16, info.getWilayaFr());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating institution info", e);
        }
    }

    private InstitutionInfo mapResultSet(ResultSet rs) throws SQLException {
        InstitutionInfo info = new InstitutionInfo();
        info.setId(rs.getInt("id"));
        info.setNameAr(getStringSafe(rs, "name_ar"));
        info.setNameFr(getStringSafe(rs, "name_fr"));
        info.setMinistryNameAr(getStringSafe(rs, "ministry_name_ar"));
        info.setMinistryNameFr(getStringSafe(rs, "ministry_name_fr"));
        info.setOrdonnateurCode(getStringSafe(rs, "ordonnateur_code"));
        info.setAuthorizingOfficerAr(getStringSafe(rs, "authorizing_officer_ar"));
        info.setAuthorizingOfficerFr(getStringSafe(rs, "authorizing_officer_fr"));
        info.setTreasuryAccountAr(getStringSafe(rs, "treasury_account_ar"));
        info.setTreasuryNameAr(getStringSafe(rs, "treasury_name_ar"));
        info.setTreasuryNameFr(getStringSafe(rs, "treasury_name_fr"));
        info.setRibNumber(getStringSafe(rs, "rib_number"));
        info.setLogoPath(getStringSafe(rs, "logo_path"));
        info.setAddressAr(getStringSafe(rs, "address_ar"));
        info.setAddressFr(getStringSafe(rs, "address_fr"));
        info.setWilayaAr(getStringSafe(rs, "wilaya_ar"));
        info.setWilayaFr(getStringSafe(rs, "wilaya_fr"));
        Timestamp ts = rs.getTimestamp("last_updated_at");
        if (ts != null) info.setLastUpdatedAt(ts.toLocalDateTime());
        return info;
    }

    private String getStringSafe(ResultSet rs, String col) {
        try {
            return rs.getString(col);
        } catch (SQLException e) {
            return "";
        }
    }
}

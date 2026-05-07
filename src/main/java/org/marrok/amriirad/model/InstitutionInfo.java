package org.marrok.amriirad.model;

import java.time.LocalDateTime;

public class InstitutionInfo {
    private int id;
    private String nameAr;
    private String nameFr;
    private String authorizingOfficerAr;
    private String treasuryAccountAr;
    private String ribNumber;
    private String logoPath;
    private String addressAr;
    private String wilayaAr;
    private LocalDateTime lastUpdatedAt;

    public InstitutionInfo() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNameAr() { return nameAr; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }

    public String getNameFr() { return nameFr; }
    public void setNameFr(String nameFr) { this.nameFr = nameFr; }

    public String getAuthorizingOfficerAr() { return authorizingOfficerAr; }
    public void setAuthorizingOfficerAr(String authorizingOfficerAr) { this.authorizingOfficerAr = authorizingOfficerAr; }

    public String getTreasuryAccountAr() { return treasuryAccountAr; }
    public void setTreasuryAccountAr(String treasuryAccountAr) { this.treasuryAccountAr = treasuryAccountAr; }

    public String getRibNumber() { return ribNumber; }
    public void setRibNumber(String ribNumber) { this.ribNumber = ribNumber; }

    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }

    public String getAddressAr() { return addressAr; }
    public void setAddressAr(String addressAr) { this.addressAr = addressAr; }

    public String getWilayaAr() { return wilayaAr; }
    public void setWilayaAr(String wilayaAr) { this.wilayaAr = wilayaAr; }

    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
}

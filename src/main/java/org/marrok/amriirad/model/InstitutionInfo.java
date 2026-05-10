package org.marrok.amriirad.model;

import java.time.LocalDateTime;

public class InstitutionInfo {
    private int id;
    private String nameAr;
    private String nameFr;
    private String ministryNameAr;
    private String ministryNameFr;
    private String ordonnateurCode;
    private String authorizingOfficerAr;
    private String authorizingOfficerFr;
    private String treasuryAccountAr;
    private String treasuryNameAr;
    private String treasuryNameFr;
    private String ribNumber;
    private String logoPath;
    private String addressAr;
    private String addressFr;
    private String wilayaAr;
    private String wilayaFr;
    private LocalDateTime lastUpdatedAt;

    public InstitutionInfo() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNameAr() { return nameAr; }
    public void setNameAr(String nameAr) { this.nameAr = nameAr; }

    public String getNameFr() { return nameFr; }
    public void setNameFr(String nameFr) { this.nameFr = nameFr; }

    public String getMinistryNameAr() { return ministryNameAr; }
    public void setMinistryNameAr(String m) { this.ministryNameAr = m; }

    public String getMinistryNameFr() { return ministryNameFr; }
    public void setMinistryNameFr(String m) { this.ministryNameFr = m; }

    public String getOrdonnateurCode() { return ordonnateurCode; }
    public void setOrdonnateurCode(String o) { this.ordonnateurCode = o; }

    public String getAuthorizingOfficerAr() { return authorizingOfficerAr; }
    public void setAuthorizingOfficerAr(String a) { this.authorizingOfficerAr = a; }

    public String getAuthorizingOfficerFr() { return authorizingOfficerFr; }
    public void setAuthorizingOfficerFr(String a) { this.authorizingOfficerFr = a; }

    public String getTreasuryAccountAr() { return treasuryAccountAr; }
    public void setTreasuryAccountAr(String t) { this.treasuryAccountAr = t; }

    public String getTreasuryNameAr() { return treasuryNameAr; }
    public void setTreasuryNameAr(String t) { this.treasuryNameAr = t; }

    public String getTreasuryNameFr() { return treasuryNameFr; }
    public void setTreasuryNameFr(String t) { this.treasuryNameFr = t; }

    public String getRibNumber() { return ribNumber; }
    public void setRibNumber(String ribNumber) { this.ribNumber = ribNumber; }

    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }

    public String getAddressAr() { return addressAr; }
    public void setAddressAr(String addressAr) { this.addressAr = addressAr; }

    public String getAddressFr() { return addressFr; }
    public void setAddressFr(String addressFr) { this.addressFr = addressFr; }

    public String getWilayaAr() { return wilayaAr; }
    public void setWilayaAr(String wilayaAr) { this.wilayaAr = wilayaAr; }

    public String getWilayaFr() { return wilayaFr; }
    public void setWilayaFr(String wilayaFr) { this.wilayaFr = wilayaFr; }

    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
}

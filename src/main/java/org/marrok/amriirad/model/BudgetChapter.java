package org.marrok.amriirad.model;

/**
 * Represents a node in the budget classification hierarchy.
 * Levels: 1=Titre, 2=Chapitre, 3=Article, 4=Paragraphe
 */
public class BudgetChapter {

    private int id;
    private String code; // e.g. "01", "01.02", "01.02.03"
    private String labelAr; // التسمية بالعربية
    private String labelFr; // التسمية بالفرنسية
    private Integer parentId; // null for root nodes (Titre)
    private int fiscalYearId;
    private int level; // 1=Titre, 2=Chapitre, 3=Article, 4=Paragraphe

    public BudgetChapter() {
    }

    public BudgetChapter(int id, String code, String labelAr, String labelFr,
            Integer parentId, int fiscalYearId, int level) {
        this.id = id;
        this.code = code;
        this.labelAr = labelAr;
        this.labelFr = labelFr;
        this.parentId = parentId;
        this.fiscalYearId = fiscalYearId;
        this.level = level;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabelAr() {
        return labelAr;
    }

    public void setLabelAr(String l) {
        this.labelAr = l;
    }

    public String getLabelFr() {
        return labelFr;
    }

    public void setLabelFr(String l) {
        this.labelFr = l;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer p) {
        this.parentId = p;
    }

    public int getFiscalYearId() {
        return fiscalYearId;
    }

    public void setFiscalYearId(int fiscalYearId) {
        this.fiscalYearId = fiscalYearId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    /** Display string used in ComboBox. */
    @Override
    public String toString() {
        return code + " — " + labelAr;
    }
}

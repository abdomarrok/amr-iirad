package org.marrok.amriirad.model;

import java.time.LocalDateTime;

/** Represents a fiscal year (سنة مالية). */
public class FiscalYear {

    private int id;
    private String yearLabel;   // e.g. "2024"
    private boolean active;
    private LocalDateTime createdAt;

    public FiscalYear() {}

    public FiscalYear(int id, String yearLabel, boolean active, LocalDateTime createdAt) {
        this.id        = id;
        this.yearLabel = yearLabel;
        this.active    = active;
        this.createdAt = createdAt;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public int getId()                      { return id; }
    public void setId(int id)               { this.id = id; }

    public String getYearLabel()            { return yearLabel; }
    public void setYearLabel(String y)      { this.yearLabel = y; }

    public boolean isActive()               { return active; }
    public void setActive(boolean active)   { this.active = active; }

    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return yearLabel; }
}

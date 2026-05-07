package org.marrok.amriirad.model;

import java.util.Objects;

/**
 * Model representing a system permission (view or action).
 * Reused pattern from GstockDz.
 */
public class Permission {
    private int id;
    private String code;      // e.g., "revenueOrder.create"
    private String type;      // "view" or "action"
    private String category;  // e.g., "revenueOrder", "users"
    private String description;

    public Permission() {
    }

    public Permission(int id, String code, String type, String category, String description) {
        this.id = id;
        this.code = code;
        this.type = type;
        this.category = category;
        this.description = description;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isViewPermission() { return "view".equals(type); }
    public boolean isActionPermission() { return "action".equals(type); }

    @Override
    public String toString() {
        return description != null ? description : code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Permission that = (Permission) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

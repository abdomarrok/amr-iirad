package org.marrok.amriirad.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model representing a user role with its permissions.
 * Reused pattern from GstockDz.
 */
public class Role {
    private int id;
    private String name;
    private String description;
    private int privilegeLevel;
    private boolean isProtected;
    private List<Permission> permissions;

    public Role() {
        this.permissions = new ArrayList<>();
    }

    public Role(int id, String name, String description, int privilegeLevel, boolean isProtected) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.privilegeLevel = privilegeLevel;
        this.isProtected = isProtected;
        this.permissions = new ArrayList<>();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getPrivilegeLevel() { return privilegeLevel; }
    public void setPrivilegeLevel(int privilegeLevel) { this.privilegeLevel = privilegeLevel; }

    public boolean isProtected() { return isProtected; }
    public void setProtected(boolean isProtected) { this.isProtected = isProtected; }

    public List<Permission> getPermissions() { return permissions; }
    public void setPermissions(List<Permission> permissions) { this.permissions = permissions; }

    public void addPermission(Permission permission) {
        this.permissions.add(permission);
    }

    public boolean hasPermission(String permissionCode) {
        return permissions.stream()
                .anyMatch(p -> p.getCode().equals(permissionCode));
    }

    @Override
    public String toString() {
        return name;
    }
}

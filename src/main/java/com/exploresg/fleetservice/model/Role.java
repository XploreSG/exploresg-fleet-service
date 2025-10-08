package com.exploresg.fleetservice.model;

public enum Role {
    USER,
    SUPPORT,
    ADMIN,
    FLEET_MANAGER,
    MANAGER;

    /**
     * Returns the role name with the "ROLE_" prefix for Spring Security.
     * This is used in @PreAuthorize annotations.
     * 
     * @return The role name with "ROLE_" prefix (e.g., "ROLE_ADMIN")
     */
    public String getAuthority() {
        return "ROLE_" + this.name();
    }

    /**
     * Convenience method to get the role name without prefix.
     * 
     * @return The role name (e.g., "ADMIN")
     */
    public String getRoleName() {
        return this.name();
    }
}

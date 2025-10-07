package com.exploresg.fleetservice.constants;

/**
 * Security constants for use in @PreAuthorize annotations.
 * This class provides compile-time safe authority strings.
 * These constants are derived from the Role enum to ensure consistency.
 */
public final class SecurityConstants {

    private SecurityConstants() {
        // Prevent instantiation
    }

    // Authority strings for @PreAuthorize annotations
    public static final String HAS_ROLE_USER = "hasAuthority('" + "ROLE_" + "USER" + "')";
    public static final String HAS_ROLE_SUPPORT = "hasAuthority('" + "ROLE_" + "SUPPORT" + "')";
    public static final String HAS_ROLE_ADMIN = "hasAuthority('" + "ROLE_" + "ADMIN" + "')";
    public static final String HAS_ROLE_FLEET_MANAGER = "hasAuthority('" + "ROLE_" + "FLEET_MANAGER" + "')";
    public static final String HAS_ROLE_MANAGER = "hasAuthority('" + "ROLE_" + "MANAGER" + "')";

    // Combined authorities (for multiple roles)
    public static final String HAS_ROLE_ADMIN_OR_FLEET_MANAGER = "hasAnyAuthority('ROLE_ADMIN', 'ROLE_FLEET_MANAGER')";
    public static final String HAS_ROLE_ADMIN_OR_MANAGER = "hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')";
}

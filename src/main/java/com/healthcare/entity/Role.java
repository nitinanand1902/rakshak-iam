package com.healthcare.entity;

/**
 * System roles for RBAC.
 * <p>
 * Extensibility: Adding a new role (e.g., NURSE, ADMIN) requires ONLY adding
 * a new enum constant here. Spring Security's hasRole() / @PreAuthorize picks
 * it up automatically. No service or repository changes needed.
 */
public enum Role {
    PATIENT,
    DOCTOR,
    TECHNICIAN;

    /** Returns the Spring Security authority string (e.g., ROLE_PATIENT). */
    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}

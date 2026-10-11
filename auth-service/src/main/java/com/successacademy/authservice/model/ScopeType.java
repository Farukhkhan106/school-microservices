package com.successacademy.authservice.model;

/**
 * Data boundaries governing which records/resources a permission applies to.
 */
public enum ScopeType {
    OWN,            // Resource owned by the user (self-service profile, own leaves, own attendance)
    ASSIGNED,       // Resources assigned directly to user (teacher's assigned students/subjects)
    ASSIGNED_CLASS, // Resources belonging to teacher's class / section
    TENANT,         // All resources within user's school tenant
    ALL             // Platform-wide cross-tenant scope (Platform Admin only)
}

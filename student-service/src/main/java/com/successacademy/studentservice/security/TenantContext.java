package com.successacademy.studentservice.security;

public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    public static final String DEFAULT_TENANT = "default";

    private TenantContext() {}

    public static void setTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            CURRENT_TENANT.set(DEFAULT_TENANT);
        } else {
            CURRENT_TENANT.set(tenantId.trim());
        }
    }

    public static String getTenantId() {
        String tenant = CURRENT_TENANT.get();
        return (tenant != null && !tenant.isBlank()) ? tenant : DEFAULT_TENANT;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}

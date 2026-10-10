package com.successacademy.academicservice.security;

/**
 * Thread-local tenant context to propagate tenant identity through the service and data layers.
 */
public final class TenantContext {

    private static final String DEFAULT_TENANT = "default";
    private static final ThreadLocal<String> CURRENT_TENANT = ThreadLocal.withInitial(() -> DEFAULT_TENANT);

    private TenantContext() {}

    public static String getTenantId() {
        String tenant = CURRENT_TENANT.get();
        return (tenant != null && !tenant.isBlank()) ? tenant : DEFAULT_TENANT;
    }

    public static void setTenantId(String tenantId) {
        if (tenantId != null && !tenantId.isBlank()) {
            CURRENT_TENANT.set(tenantId.trim());
        } else {
            CURRENT_TENANT.set(DEFAULT_TENANT);
        }
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}

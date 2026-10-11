# Phase 1: Multi-Tenant & Environment-Aware Configuration Foundation

## Overview
Phase 1 establishes the production-grade architectural foundation for a configurable, multi-tenant, environment-aware School ERP / SaaS platform. It enables multiple schools/tenants to operate securely on a single shared codebase with independent branding, contact identities, feature entitlements, and environment-specific settings—without any hardcoded tenant or environment conditional logic in application business code.

---

## 1. Tenant Model
A Tenant represents an independent school or educational institution utilizing the ERP SaaS platform.
- **Identifier**: Auto-incrementing primary key (`id`) and immutable machine key (`tenant_key`, e.g., `"default"`, `"school_satwas"`, `"school_b"`).
- **Attributes**:
  - `tenantKey`: Unique, stable key used across database schemas, JWT tokens, and gateway headers.
  - `displayName`: Human-readable school name (e.g., `"Success Academy"`).
  - `legalName`: Full organizational name (e.g., `"Success Academy School Satwas"`).
  - `status`: Tenant lifecycle state (`ACTIVE`, `SUSPENDED`, `INACTIVE`, `PENDING`).
  - `domain`: Optional dedicated custom domain / vanity URL.
  - `contactEmail`, `contactPhone`: Verified organizational contact channels.
  - `createdAt`, `updatedAt`: Audited timestamps.
- **Database Table**: `authdb.tenants`

---

## 2. Environment Model
The platform separates deployment and runtime environments from tenant identities.
- **Supported Environments**: `DEV` (Development), `QA` (Quality Assurance), `UAT` (User Acceptance Testing), `PROD` (Production). Extensible for future custom environments.
- **Runtime Resolution**: Resolved exclusively from server-side deployment configuration (`app.environment` or `SPRING_PROFILES_ACTIVE`).
- **Security Boundary**: Normal users and frontend clients cannot arbitrarily toggle or select environments via query parameters or body payloads.
- **Database Table**: `authdb.environments`

---

## 3. Tenant + Environment Relationship
Configurations and feature entitlements support a hierarchical fallback chain:
```
Global / Code Baseline Defaults
         ↓
Tenant-Level Configuration (environmentKey = null)
         ↓
Tenant + Environment Specific Override (e.g., tenantKey = 'default', environmentKey = 'PROD')
```
This enables tenant-specific customizations that behave differently across DEV, QA, and PROD (e.g., test payment gateways or disabled messaging in QA) without affecting other tenants or environments.

---

## 4. Configuration Model & Domains
Configuration is centralized, strongly structured, and domain-partitioned into architectural categories:
1. **`BRANDING`**: `schoolName`, `fullName`, `tagline`, `motto`, `logoUrl`, `faviconUrl`, `primaryColor`, `secondaryColor`, `accentColor`.
2. **`ORGANIZATION`**: `address`, `city`, `state`, `pincode`, `country`, `contactEmail`, `contactPhone`, `workingHours`, `officeHours`, `curriculum`, `affiliation`, `medium`, `mapEmbedUrl`.
3. **`NAVIGATION`**: `enabledModules`, `defaultLandingPage`.
4. **`DASHBOARD`**: `showKpiCards`, `showAttendanceAnalytics`, `showUpcomingEvents`, `showRecentNotices`, `customWidgets`.
5. **`BUSINESS`**: `currency`, `timezone`, `dateFormat`, `academicYearStartMonth`.
- **Database Table**: `authdb.tenant_configurations`

---

## 5. Configuration Versioning & Lifecycle
Configuration changes are auditable and publishable through a strict version lifecycle:
- **`DRAFT`**: Active working revision. Can be edited by authorized administrators.
- **`PUBLISHED`**: Active production configuration for that tenant and environment. Exactly one version is published per `(tenant_key, environment_key)` at any given time.
- **`ARCHIVED`**: Historical versions preserved indefinitely for auditability and rollback.
- **Metadata**: Tracks `versionNumber`, `changeSummary`, `createdBy`, `createdAt`, `publishedBy`, `publishedAt`, `archivedAt`.
- **Database Table**: `authdb.configuration_versions`

---

## 6. Configuration Resolution Layer
Handled centrally by `ConfigurationResolverService`:
`EffectiveConfiguration = resolve(tenantKey, environmentKey)`
1. Resolves `TenantDto` and `EnvironmentDto`.
2. Locates active `PUBLISHED` version for `(tenantKey, environmentKey)` with fallback to global published version for that tenant.
3. Deserializes category JSON data into strongly-typed DTOs, falling back safely to robust baselines if categories are omitted.
4. Queries `FeatureEntitlementService` for active feature flags.
5. Filters `navigation.enabledModules` to only entitled modules.
6. Caches resolved payloads in a thread-safe in-memory cache with TTL and instant invalidation on version publish or tenant update.

---

## 7. Feature Entitlement Concept
Features are identified by stable, machine-readable constants:
- `ATTENDANCE`
- `FEES`
- `EXAMINATIONS`
- `ACTIVITIES`
- `NOTICES`
- `EVENTS`
- `GALLERY`
- `COMMUNICATION`
- `STAFF`
- `FACULTY`
- `ACADEMIC_SESSIONS`

### Enforcement Mechanics:
- **Frontend**: `useFeature(featureKey)` and `<FeatureGate feature="...">` / `<FeatureRouteGuard feature="...">` hide navigation links, buttons, and redirect users away from disabled views.
- **Backend**: `FeatureEntitlementService.requireFeature(key)` and `@RequireFeature(key)` interceptor reject unauthorized access with `HTTP 403 Forbidden`.

---

## 8. Server-Side Tenant Context & Security Boundaries
- **API Gateway (`api-gateway`)**:
  - `JwtAuthFilter` strips any client-supplied spoofed headers (`X-Tenant-Id`, `X-Internal-Secret`).
  - Decodes verified JWT claims and extracts the authenticated `tenantId`.
  - Injects `X-Tenant-Id` and `X-Internal-Secret` into microservice requests.
  - Public paths default to the host tenant (`default`).
- **Microservices (`auth-service`, `academic-service`, etc.)**:
  - `TenantContextFilter` verifies `X-Internal-Secret` before honoring `X-Tenant-Id`.
  - Populates thread-local `TenantContext` accessible throughout service and repository layers.
  - Direct external requests cannot forge tenant identity.

---

## 9. API Contracts
### Public / Tenant-Aware:
- `GET /auth-service/configuration/effective` (and `/configuration/effective` via Gateway)
  - Returns `EffectiveConfigResponse` for the authenticated or host tenant.

### Admin Management (Requires `ADMIN` role):
- `GET /auth-service/tenants`: List all registered tenants.
- `GET /auth-service/tenants/{tenantKey}`: Retrieve tenant profile.
- `POST /auth-service/tenants`: Register new tenant with baseline configuration.
- `PUT /auth-service/tenants/{tenantKey}`: Update tenant profile.
- `GET /auth-service/configuration/versions`: List configuration versions for tenant.
- `POST /auth-service/configuration/versions`: Create new DRAFT version.
- `PUT /auth-service/configuration/versions/{versionId}`: Update DRAFT category JSON.
- `POST /auth-service/configuration/versions/{versionId}/publish`: Publish DRAFT version and archive previous.
- `GET /auth-service/configuration/features`: List feature entitlements.
- `PUT /auth-service/configuration/features`: Bulk update feature entitlements.

---

## 10. Frontend Architecture
- `TenantConfigProvider` wraps the application tree inside `App.tsx`.
- Calls `configService.getEffectiveConfig()` on startup.
- Manages loading and safe error states.
- Exposes `useTenantConfig()` and `useFeature()` hooks.
- `DashboardSidebar`, `PublicHeader`, and `PublicFooter` dynamically render school name, logos, and contact information without hardcoded tenant strings.
- Navigation menus automatically filter out modules disabled in feature entitlements.

---

## 11. Database Relationships & Schema
- `tenants` (id, tenant_key [UNIQUE], display_name, legal_name, status, domain, contact_email, contact_phone, created_at, updated_at)
- `environments` (id, environment_key [UNIQUE], display_name, is_production, status, created_at, updated_at)
- `configuration_versions` (id, tenant_key, environment_key, version_number, status, change_summary, created_by, published_by, published_at, archived_at)
- `tenant_configurations` (id, version_id [FK], tenant_key, environment_key, category, config_data, schema_version)
- `feature_entitlements` (id, tenant_key, environment_key, feature_key, enabled, description, updated_by)
- `configuration_audit_logs` (id, tenant_key, environment_key, actor_username, actor_role, action, details, timestamp)

---

## 12. Scope & Phase Boundaries
### Implemented in Phase 1:
- [x] Multi-tenant data model & environments catalog.
- [x] Configuration versioning (`DRAFT`, `PUBLISHED`, `ARCHIVED`).
- [x] Configuration resolution engine with caching and fallback.
- [x] Feature entitlement catalog & backend enforcement interceptor.
- [x] Server-side trusted TenantContext propagation.
- [x] Effective configuration API (`GET /configuration/effective`).
- [x] Frontend `TenantConfigProvider`, `useTenantConfig()`, `useFeature()`, and `FeatureGate`.
- [x] Dynamic branding across header, footer, and sidebar.
- [x] Complete backward compatibility and existing data preservation.

### Intentionally Deferred to Future Phases:
- **Phase 2**: Multi-tenant Academic Session isolation & dynamic class/section taxonomy engine per tenant.
- **Phase 3**: Granular Role & Permission matrix engine per tenant (RBAC/ABAC).
- **Phase 4**: Configurable Workflow & Approval engine per tenant.
- **Phase 5**: Custom Report & Template designer per tenant.
- **Phase 6**: Dynamic Dashboard builder with drag-and-drop tenant widgets.
- **Phase 7**: Tenant Billing, Subscription, and Automated SaaS provisioning.
- **Phase 8**: Distributed cache cluster (Redis) and multi-region routing.

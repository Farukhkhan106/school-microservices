package com.successacademy.authservice.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EffectiveConfigResponse {

    private TenantDto tenant;
    private EnvironmentDto environment;
    private VersionInfoDto version;
    private BrandingConfigDto branding;
    private OrganizationConfigDto organization;
    private Map<String, Boolean> features;
    private NavigationConfigDto navigation;
    private DashboardConfigDto dashboard;
    private BusinessConfigDto business;
}

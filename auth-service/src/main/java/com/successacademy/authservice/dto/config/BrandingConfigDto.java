package com.successacademy.authservice.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandingConfigDto {

    private String schoolName;
    private String fullName;
    private String tagline;
    private String motto;
    private String logoUrl;
    private String faviconUrl;
    private String primaryColor;
    private String secondaryColor;
    private String accentColor;
}

package com.successacademy.authservice.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NavigationItemDto {
    private String id;
    private String key;
    private String label;
    private String icon;
    private String route;
    private String parentKey;
    private Integer displayOrder;
    private String section;
    private String requiredFeature;
    private String requiredPermission;
    private String requiredScope;
    private String badge;
    @Builder.Default
    private List<NavigationItemDto> children = new ArrayList<>();
}

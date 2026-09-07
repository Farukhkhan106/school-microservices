package com.successacademy.galleryservice.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class GalleryItemRequest {
    private String title;
    private String description;
    private String imageUrl;
    private String category;
    private boolean publicVisible;
    private int displayOrder;
    private String createdBy;
}

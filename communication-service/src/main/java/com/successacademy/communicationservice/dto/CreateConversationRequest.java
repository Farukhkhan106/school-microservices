package com.successacademy.communicationservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {

    @NotBlank(message = "Conversation type is required")
    private String type; // DIRECT, CLASS_GROUP

    // If DIRECT:
    private Long recipientUserId;

    // If CLASS_GROUP:
    private String targetClass;
    private String targetSection;

    private String initialMessage;
}

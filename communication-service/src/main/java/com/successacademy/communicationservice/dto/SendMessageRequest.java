package com.successacademy.communicationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    @NotBlank(message = "Message content cannot be blank")
    @Size(max = 4000, message = "Message cannot exceed 4000 characters")
    private String content;

    private String messageType = "TEXT"; // TEXT, ANNOUNCEMENT, FILE

    private String attachmentUrl;

    private String attachmentName;
}

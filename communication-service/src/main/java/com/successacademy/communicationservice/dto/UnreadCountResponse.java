package com.successacademy.communicationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountResponse {
    private long totalUnread;
    private Map<Long, Long> byConversation;
}

package com.successacademy.staffservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffStatsResponse {

    private long totalStaff;
    private long activeStaff;
    private long onLeaveStaff;
    private long withLoginCount;
    private long withoutLoginCount;
    private long presentToday;
    private long absentToday;
    private long lateToday;
}

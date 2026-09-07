package com.successacademy.attendanceservice.dto;

import lombok.*;

import java.util.List;

/**
 * Bulk attendance save — teacher submits the whole class at once.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class BulkAttendanceRequest {

    private List<AttendanceRequest> records;
}

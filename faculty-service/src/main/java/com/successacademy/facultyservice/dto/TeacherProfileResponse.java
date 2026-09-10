package com.successacademy.facultyservice.dto;

import lombok.*;

import java.util.List;

/** Everything the Teacher portal needs about the LOGGED-IN teacher (server-side resolved from X-User-Id). */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TeacherProfileResponse {
    private FacultyResponse faculty;
    private List<AssignmentResponse> assignments;   // subject-teacher assignments
    private List<ScheduleResponse> schedule;        // own weekly timetable
    private List<String> allowedClasses;            // "8-A" style — classes this teacher may see
}
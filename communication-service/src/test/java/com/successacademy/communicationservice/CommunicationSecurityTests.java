package com.successacademy.communicationservice;

import com.successacademy.communicationservice.client.AuthServiceClient;
import com.successacademy.communicationservice.client.FacultyServiceClient;
import com.successacademy.communicationservice.client.StudentServiceClient;
import com.successacademy.communicationservice.security.UserContext;
import com.successacademy.communicationservice.service.SchoolRelationshipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CommunicationSecurityTests {

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private StudentServiceClient studentServiceClient;

    @Mock
    private FacultyServiceClient facultyServiceClient;

    private SchoolRelationshipService relationshipService;

    // Test Users
    private UserContext studentRahul; // Class 10-A
    private UserContext studentPriya; // Class 12-B
    private UserContext teacherSunita; // Class Teacher of 10-A
    private UserContext teacherRakesh; // Class Teacher of 9-B
    private UserContext principalAdmin; // Admin / Principal

    @BeforeEach
    void setUp() {
        relationshipService = new SchoolRelationshipService(
                authServiceClient,
                studentServiceClient,
                facultyServiceClient
        );

        // Rahul (Student in 10-A)
        studentRahul = UserContext.builder()
                .userId(101L)
                .username("rahul")
                .role("STUDENT")
                .studentId(1L)
                .build();

        // Priya (Student in 12-B)
        studentPriya = UserContext.builder()
                .userId(102L)
                .username("priya")
                .role("STUDENT")
                .studentId(2L)
                .build();

        // Sunita (Teacher of 10-A)
        teacherSunita = UserContext.builder()
                .userId(201L)
                .username("sunita")
                .role("TEACHER")
                .teacherId(10L)
                .build();

        // Rakesh (Teacher of 9-B)
        teacherRakesh = UserContext.builder()
                .userId(202L)
                .username("rakesh")
                .role("TEACHER")
                .teacherId(20L)
                .build();

        // Admin
        principalAdmin = UserContext.builder()
                .userId(1L)
                .username("admin")
                .role("ADMIN")
                .build();

        // Mock Student Data
        when(studentServiceClient.getStudentById(1L)).thenReturn(
                StudentServiceClient.StudentInfo.builder()
                        .id(1L)
                        .firstName("Rahul")
                        .lastName("Sharma")
                        .studentClass("10")
                        .section("A")
                        .build()
        );

        when(studentServiceClient.getStudentById(2L)).thenReturn(
                StudentServiceClient.StudentInfo.builder()
                        .id(2L)
                        .firstName("Priya")
                        .lastName("Patel")
                        .studentClass("12")
                        .section("B")
                        .build()
        );

        // Mock Faculty Data
        when(facultyServiceClient.getFacultyById(10L)).thenReturn(
                FacultyServiceClient.FacultyInfo.builder()
                        .id(10L)
                        .name("Mrs. Sunita Verma")
                        .classTeacherOf("10-A")
                        .subjects(List.of("English"))
                        .designation("Senior English Teacher")
                        .userId(201L)
                        .build()
        );

        when(facultyServiceClient.getFacultyById(20L)).thenReturn(
                FacultyServiceClient.FacultyInfo.builder()
                        .id(20L)
                        .name("Mr. Rakesh Gupta")
                        .classTeacherOf("9-B")
                        .subjects(List.of("Mathematics"))
                        .designation("Senior Teacher")
                        .userId(202L)
                        .build()
        );
    }

    @Test
    @DisplayName("RULE 7: Student-to-Student private messaging is strictly DISABLED (403)")
    void testStudentToStudentDisabled() {
        boolean allowed = relationshipService.canInitiateDirectConversation(studentRahul, studentPriya);
        assertFalse(allowed, "Student-to-student messaging must be strictly blocked");
    }

    @Test
    @DisplayName("RULE 6: Student Rahul (10-A) to assigned Teacher Sunita (10-A) is ALLOWED")
    void testStudentToAssignedTeacherAllowed() {
        boolean allowed = relationshipService.canInitiateDirectConversation(studentRahul, teacherSunita);
        assertTrue(allowed, "Student Rahul must be allowed to message his assigned Class Teacher");
    }

    @Test
    @DisplayName("RULE 4: Student Rahul (10-A) to unrelated Teacher Rakesh (9-B) is FORBIDDEN (403)")
    void testStudentToUnrelatedTeacherForbidden() {
        boolean allowed = relationshipService.canInitiateDirectConversation(studentRahul, teacherRakesh);
        assertFalse(allowed, "Student Rahul must NOT be allowed to message unrelated teacher of 9-B");
    }

    @Test
    @DisplayName("RULE 1: Student can message Principal/Admin for school inquiries")
    void testStudentToAdminAllowed() {
        boolean allowed = relationshipService.canInitiateDirectConversation(studentRahul, principalAdmin);
        assertTrue(allowed, "Student must be allowed to contact Admin/Principal");
    }

    @Test
    @DisplayName("RULE 6: Teacher Sunita (10-A) to student in her class (Rahul) is ALLOWED")
    void testTeacherToAssignedStudentAllowed() {
        boolean allowed = relationshipService.canInitiateDirectConversation(teacherSunita, studentRahul);
        assertTrue(allowed, "Teacher must be allowed to message students in her class");
    }

    @Test
    @DisplayName("RULE 6: Teacher Rakesh (9-B) to unrelated student (Rahul 10-A) is FORBIDDEN (403)")
    void testTeacherToUnrelatedStudentForbidden() {
        boolean allowed = relationshipService.canInitiateDirectConversation(teacherRakesh, studentRahul);
        assertFalse(allowed, "Teacher of 9-B must NOT be allowed to message student of 10-A");
    }

    @Test
    @DisplayName("RULE 3: Student Rahul can access own class channel (Class 10-A)")
    void testStudentAccessOwnClassChannel() {
        boolean allowed = relationshipService.canAccessClassChannel(studentRahul, "10", "A");
        assertTrue(allowed, "Rahul must be allowed into Class 10-A channel");
    }

    @Test
    @DisplayName("RULE 4: Student Rahul CANNOT access unrelated class channel (Class 12-B)")
    void testStudentAccessUnrelatedClassChannel() {
        boolean allowed = relationshipService.canAccessClassChannel(studentRahul, "12", "B");
        assertFalse(allowed, "Rahul must be rejected from Class 12-B channel");
    }

    @Test
    @DisplayName("RULE 4: Teacher Sunita can access her assigned class channel (Class 10-A)")
    void testTeacherAccessAssignedClassChannel() {
        boolean allowed = relationshipService.canAccessClassChannel(teacherSunita, "10", "A");
        assertTrue(allowed, "Sunita must be allowed into Class 10-A channel");
    }

    @Test
    @DisplayName("RULE 5: Teacher Sunita CANNOT access unrelated class channel (Class 9-B)")
    void testTeacherAccessUnrelatedClassChannel() {
        boolean allowed = relationshipService.canAccessClassChannel(teacherSunita, "9", "B");
        assertFalse(allowed, "Sunita must be rejected from Class 9-B channel");
    }

    @Test
    @DisplayName("RULE 17: Students CANNOT access Faculty Staff Room (403)")
    void testStudentAccessStaffRoomForbidden() {
        boolean allowed = relationshipService.canAccessStaffRoom(studentRahul);
        assertFalse(allowed, "Students must be rejected from Faculty Staff Room");
    }

    @Test
    @DisplayName("RULE 17: Teachers and Admin can access Faculty Staff Room")
    void testStaffRoomAccessAllowedForFacultyAndAdmin() {
        assertTrue(relationshipService.canAccessStaffRoom(teacherSunita), "Teacher can access Staff Room");
        assertTrue(relationshipService.canAccessStaffRoom(principalAdmin), "Admin can access Staff Room");
    }

    @Test
    @DisplayName("RULE 10 & 11: Only ADMIN can broadcast announcements (Teachers & Students rejected)")
    void testBroadcastPermissions() {
        assertTrue(relationshipService.canPostMessage(principalAdmin, "BROADCAST", null, null), "Admin can broadcast");
        assertFalse(relationshipService.canPostMessage(teacherSunita, "BROADCAST", null, null), "Teacher CANNOT broadcast");
        assertFalse(relationshipService.canPostMessage(studentRahul, "BROADCAST", null, null), "Student CANNOT broadcast");
    }
}

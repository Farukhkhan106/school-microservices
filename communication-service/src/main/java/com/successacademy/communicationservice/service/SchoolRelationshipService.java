package com.successacademy.communicationservice.service;

import com.successacademy.communicationservice.client.AuthServiceClient;
import com.successacademy.communicationservice.client.FacultyServiceClient;
import com.successacademy.communicationservice.client.StudentServiceClient;
import com.successacademy.communicationservice.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchoolRelationshipService {

    private final AuthServiceClient authServiceClient;
    private final StudentServiceClient studentServiceClient;
    private final FacultyServiceClient facultyServiceClient;

    /**
     * Resolves the full UserContext for a given user, looking up studentId or teacherId if missing.
     */
    public UserContext enrichUserContext(UserContext ctx) {
        if (ctx == null) return null;

        // If studentId or teacherId is missing, fetch from auth-service
        if ((ctx.isStudent() && ctx.getStudentId() == null) || (ctx.isTeacher() && ctx.getTeacherId() == null)) {
            UserContext full = null;
            if (ctx.getUserId() != null) {
                full = authServiceClient.getUserById(ctx.getUserId());
            } else if (ctx.getUsername() != null) {
                full = authServiceClient.getUserByUsername(ctx.getUsername());
            }
            if (full != null) {
                if (ctx.getUserId() == null) ctx.setUserId(full.getUserId());
                if (ctx.getStudentId() == null) ctx.setStudentId(full.getStudentId());
                if (ctx.getTeacherId() == null) ctx.setTeacherId(full.getTeacherId());
                if (ctx.getRole() == null) ctx.setRole(full.getRole());
            }
        }
        return ctx;
    }

    /**
     * Checks if a user is authorized to access a given class channel (e.g. 10-A)
     */
    public boolean canAccessClassChannel(UserContext user, String targetClass, String targetSection) {
        if (user == null || targetClass == null) return false;
        if (user.isAdmin()) return true; // Principal/Admin can access all class channels

        String normalizedSection = (targetSection != null && !targetSection.isBlank())
                ? targetSection.trim().toUpperCase() : "A";
        String channelKey = targetClass.trim() + "-" + normalizedSection;

        if (user.isStudent()) {
            StudentServiceClient.StudentInfo student = getStudentForUser(user);
            if (student == null) return false;
            String studentKey = student.getClassSectionKey();
            return channelKey.equalsIgnoreCase(studentKey);
        }

        if (user.isTeacher()) {
            FacultyServiceClient.FacultyInfo faculty = getFacultyForUser(user);
            if (faculty == null) return false;
            return faculty.teachesClassSection(targetClass, normalizedSection);
        }

        return false;
    }

    /**
     * Checks if a user is authorized to access the Faculty Staff Room
     */
    public boolean canAccessStaffRoom(UserContext user) {
        if (user == null) return false;
        return user.isAdmin() || user.isTeacher();
    }

    /**
     * Checks if a user can post a message in a conversation
     */
    public boolean canPostMessage(UserContext user, String conversationType, String targetClass, String targetSection) {
        if (user == null) return false;

        if ("BROADCAST".equalsIgnoreCase(conversationType)) {
            // Requirement 18: Only ADMIN can create announcements
            return user.isAdmin();
        }

        if ("STAFF_GROUP".equalsIgnoreCase(conversationType)) {
            // ADMIN and TEACHER can chat in Staff Room
            return user.isAdmin() || user.isTeacher();
        }

        if ("CLASS_GROUP".equalsIgnoreCase(conversationType)) {
            // Must have permission to access that class
            return canAccessClassChannel(user, targetClass, targetSection);
        }

        // For DIRECT conversations, permission is checked at conversation creation
        return true;
    }

    /**
     * Strict verification before creating/allowing a DIRECT conversation between initiator and recipient.
     */
    public boolean canInitiateDirectConversation(UserContext initiator, UserContext recipient) {
        if (initiator == null || recipient == null) return false;
        if (initiator.getUserId().equals(recipient.getUserId())) return false; // cannot message oneself

        // 1. ADMIN / Principal can message anyone
        if (initiator.isAdmin() || recipient.isAdmin()) {
            return true;
        }

        // 2. Requirement 5: STUDENT → STUDENT private messaging is DISABLED (Strict 403)
        if (initiator.isStudent() && recipient.isStudent()) {
            log.warn("BLOCKED: Student {} attempted to message student {}", initiator.getUsername(), recipient.getUsername());
            return false;
        }

        // 3. Requirement 6: STUDENT → TEACHER verification
        if (initiator.isStudent() && recipient.isTeacher()) {
            return isTeacherAssignedToStudent(recipient, initiator);
        }

        // 4. TEACHER → STUDENT verification
        if (initiator.isTeacher() && recipient.isStudent()) {
            return isTeacherAssignedToStudent(initiator, recipient);
        }

        // 5. TEACHER ↔ TEACHER communication is allowed
        if (initiator.isTeacher() && recipient.isTeacher()) {
            return true;
        }

        return false;
    }

    private boolean isTeacherAssignedToStudent(UserContext teacherCtx, UserContext studentCtx) {
        StudentServiceClient.StudentInfo student = getStudentForUser(studentCtx);
        if (student == null) return false;

        FacultyServiceClient.FacultyInfo faculty = getFacultyForUser(teacherCtx);
        if (faculty == null) return false;

        // If teacher is Principal / Vice Principal, allowed
        if (faculty.isPrincipal()) return true;

        String studentKey = student.getClassSectionKey(); // e.g. "10-A"
        if (studentKey == null) return false;

        // Check if teacher is class teacher of student's class
        if (faculty.getClassTeacherOf() != null && faculty.getClassTeacherOf().trim().equalsIgnoreCase(studentKey)) {
            return true;
        }

        return false;
    }

    public StudentServiceClient.StudentInfo getStudentForUser(UserContext user) {
        if (user.getStudentId() != null) {
            return studentServiceClient.getStudentById(user.getStudentId());
        }
        return null;
    }

    public FacultyServiceClient.FacultyInfo getFacultyForUser(UserContext user) {
        if (user.getTeacherId() != null) {
            return facultyServiceClient.getFacultyById(user.getTeacherId());
        }
        if (user.getUserId() != null) {
            return facultyServiceClient.getFacultyByUserId(user.getUserId());
        }
        return null;
    }

    /**
     * Discovers all real active class sections from student data
     */
    public Set<String> getRealActiveClasses() {
        return studentServiceClient.getActiveRealClassSections();
    }
}

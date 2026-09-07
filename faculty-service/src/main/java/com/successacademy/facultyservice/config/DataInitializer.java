package com.successacademy.facultyservice.config;

import com.successacademy.facultyservice.model.Faculty;
import com.successacademy.facultyservice.repository.FacultyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final FacultyRepository repository;

    @Override
    public void run(String... args) {

        if (repository.count() > 0) return; // already seeded

        // id=1 — matches auth-service teacher user teacherId=1
        repository.save(Faculty.builder()
                .name("Dr. Anil Kumar")
                .email("anil.kumar@successacademy.edu.in")
                .phone("9876543100")
                .designation("Principal")
                .qualification("Ph.D. in Education")
                .experience(25)
                .subjects("Administration")
                .photoUrl("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .userId(3L) // auth user id for teacher account
                .build());

        repository.save(Faculty.builder()
                .name("Mrs. Sunita Verma")
                .email("sunita.verma@successacademy.edu.in")
                .phone("9876543101")
                .designation("Vice Principal")
                .qualification("M.Ed., M.A. English")
                .experience(20)
                .subjects("English")
                .classTeacherOf("10-A")
                .photoUrl("https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build());

        repository.save(Faculty.builder()
                .name("Mr. Rakesh Gupta")
                .email("rakesh.gupta@successacademy.edu.in")
                .phone("9876543102")
                .designation("Senior Mathematics Teacher")
                .qualification("M.Sc. Mathematics, B.Ed.")
                .experience(15)
                .subjects("Mathematics,Physics")
                .classTeacherOf("9-B")
                .photoUrl("https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build());

        repository.save(Faculty.builder()
                .name("Ms. Anjali Reddy")
                .email("anjali.reddy@successacademy.edu.in")
                .phone("9876543103")
                .designation("Science Teacher")
                .qualification("M.Sc. Chemistry, B.Ed.")
                .experience(10)
                .subjects("Chemistry,Biology")
                .classTeacherOf("8-A")
                .photoUrl("https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build());

        repository.save(Faculty.builder()
                .name("Mr. Sanjay Mehta")
                .email("sanjay.mehta@successacademy.edu.in")
                .phone("9876543104")
                .designation("Physical Education Teacher")
                .qualification("B.P.Ed., M.P.Ed.")
                .experience(12)
                .subjects("Physical Education,Sports")
                .photoUrl("https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build());

        repository.save(Faculty.builder()
                .name("Mrs. Kavita Joshi")
                .email("kavita.joshi@successacademy.edu.in")
                .phone("9876543105")
                .designation("Hindi Teacher")
                .qualification("M.A. Hindi, B.Ed.")
                .experience(8)
                .subjects("Hindi,Sanskrit")
                .classTeacherOf("7-A")
                .photoUrl("https://images.unsplash.com/photo-1580489944761-15a19d654956?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build());

        System.out.println("✅ Faculty data seeded — 6 records.");
    }
}

package com.successacademy.facultyservice.config;

import com.successacademy.facultyservice.model.Faculty;
import com.successacademy.facultyservice.repository.FacultyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final FacultyRepository repository;

    @Override
    public void run(String... args) {
        List<Faculty> sampleFaculty = List.of(
            // ── 1. School Leadership & Senior Academic Administration ──
            Faculty.builder()
                .name("Dr. Anil Kumar")
                .email("anil.kumar@successacademy.edu.in")
                .phone("9876543100")
                .designation("Principal & Academic Director")
                .qualification("Ph.D. in Educational Leadership, M.Sc. Physics")
                .experience(26)
                .subjects("Administration,Educational Policy")
                .photoUrl("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .userId(3L) // Linked auth-service teacher user (teacherId=1)
                .build(),

            Faculty.builder()
                .name("Mrs. Sunita Verma")
                .email("sunita.verma@successacademy.edu.in")
                .phone("9876543101")
                .designation("Vice Principal & Senior English Faculty")
                .qualification("M.Ed., M.A. English Literature")
                .experience(20)
                .subjects("English,Communication Skills")
                .classTeacherOf("10-A")
                .photoUrl("https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            Faculty.builder()
                .name("Dr. Devendra Swaroop Sharma")
                .email("devendra.sharma@successacademy.edu.in")
                .phone("9876543106")
                .designation("Academic Dean & Controller of Examinations")
                .qualification("Ph.D. Statistics, M.Sc. Applied Mathematics")
                .experience(28)
                .subjects("Mathematics,Statistics")
                .photoUrl("https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            // ── 2. Core STEM & Secondary/Senior Secondary Faculty ──
            Faculty.builder()
                .name("Mr. Rakesh Gupta")
                .email("rakesh.gupta@successacademy.edu.in")
                .phone("9876543102")
                .designation("Senior Mathematics Teacher (PGT)")
                .qualification("M.Sc. Mathematics, B.Ed.")
                .experience(15)
                .subjects("Mathematics,Physics")
                .classTeacherOf("9-B")
                .photoUrl("https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            Faculty.builder()
                .name("Ms. Anjali Reddy")
                .email("anjali.reddy@successacademy.edu.in")
                .phone("9876543103")
                .designation("Senior Chemistry Teacher (PGT) & Science Lab Incharge")
                .qualification("M.Sc. Chemistry, B.Ed., CSIR-NET")
                .experience(10)
                .subjects("Chemistry,Environmental Science")
                .classTeacherOf("8-A")
                .photoUrl("https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            Faculty.builder()
                .name("Prof. Jean-Pierre D'Souza")
                .email("jeanpierre.dsouza@successacademy.edu.in")
                .phone("9876543107")
                .designation("Senior Physics Faculty (PGT - Competitive Wing)")
                .qualification("M.Tech. Applied Optics (IIT Madras), B.Sc. Physics")
                .experience(18)
                .subjects("Physics,Astrophysics,Applied Mechanics")
                .classTeacherOf("12-A")
                .photoUrl("https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            Faculty.builder()
                .name("Dr. Meenakshi Sundaram")
                .email("meenakshi.sundaram@successacademy.edu.in")
                .phone("9876543108")
                .designation("PGT Biology & Biotechnology Specialist")
                .qualification("Ph.D. Molecular Biology, M.Sc. Botany, B.Ed.")
                .experience(14)
                .subjects("Biology,Biotechnology,Genetics")
                .classTeacherOf("11-A")
                .photoUrl("https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            Faculty.builder()
                .name("Mr. Arjun Vikramaditya Rathore")
                .email("arjun.rathore@successacademy.edu.in")
                .phone("9876543109")
                .designation("PGT Computer Science & AI Lab Lead")
                .qualification("M.Tech CSE, B.Tech IT, AWS Certified Cloud Practitioner")
                .experience(9)
                .subjects("Computer Science,Artificial Intelligence,Python Programming")
                .classTeacherOf("12-B")
                .photoUrl("https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            Faculty.builder()
                .name("Tanya Sengupta")
                .email("tanya.sengupta@successacademy.edu.in")
                .phone("9876543110")
                .designation("TGT Mathematics & Vedic Math Trainer")
                .qualification("M.Sc. Mathematics, B.Ed.")
                .experience(6)
                .subjects("Mathematics,Mental Ability")
                .classTeacherOf("7-B")
                .photoUrl("https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            // ── 3. Commerce, Humanities & Social Sciences ──
            Faculty.builder()
                .name("Mr. Harish Chandra Aggarwal")
                .email("harish.aggarwal@successacademy.edu.in")
                .phone("9876543111")
                .designation("Senior Commerce & Accountancy Faculty (PGT)")
                .qualification("M.Com, CA-Inter, B.Ed.")
                .experience(22)
                .subjects("Accountancy,Financial Accounting,Cost Accounting")
                .classTeacherOf("11-B")
                .photoUrl("https://images.unsplash.com/photo-1560250097-0b93528c311a?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            Faculty.builder()
                .name("Mrs. Priyanka Kulkarni")
                .email("priyanka.kulkarni@successacademy.edu.in")
                .phone("9876543112")
                .designation("PGT Economics & Entrepreneurship Mentor")
                .qualification("M.A. Economics (Delhi School of Economics), B.Ed.")
                .experience(11)
                .subjects("Economics,Business Studies,Entrepreneurship")
                .classTeacherOf("11-C")
                .photoUrl("https://images.unsplash.com/photo-1573497019940-1c28c88b4f3e?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            Faculty.builder()
                .name("Mr. Mohammad Rizwan Khan")
                .email("rizwan.khan@successacademy.edu.in")
                .phone("9876543113")
                .designation("TGT Social Science & Debate Coach")
                .qualification("M.A. Modern History, B.Ed.")
                .experience(8)
                .subjects("History,Political Science,Civics")
                .classTeacherOf("9-A")
                .photoUrl("https://images.unsplash.com/photo-1628157582853-a796fa650a6a?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            Faculty.builder()
                .name("Mrs. Radhika Venkat")
                .email("radhika.venkat@successacademy.edu.in")
                .phone("9876543114")
                .designation("TGT Geography & Environmental Studies")
                .qualification("M.Sc. Applied Geography, B.Ed., GIS Diploma")
                .experience(7)
                .subjects("Geography,Disaster Management,Environmental Studies")
                .classTeacherOf("8-B")
                .photoUrl("https://images.unsplash.com/photo-1567532939604-b6b5b0db2604?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            // ── 4. Languages, Classical & International ──
            Faculty.builder()
                .name("Mrs. Kavita Joshi")
                .email("kavita.joshi@successacademy.edu.in")
                .phone("9876543105")
                .designation("Senior Hindi & Sanskrit Teacher (TGT)")
                .qualification("M.A. Hindi, B.Ed.")
                .experience(8)
                .subjects("Hindi,Sanskrit")
                .classTeacherOf("7-A")
                .photoUrl("https://images.unsplash.com/photo-1580489944761-15a19d654956?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            Faculty.builder()
                .name("Dr. Acharya Vidyadhar Shastri")
                .email("vidyadhar.shastri@successacademy.edu.in")
                .phone("9876543115")
                .designation("Acharya in Sanskrit & Vedic Philosophy")
                .qualification("Ph.D. Sanskrit Sahitya (BHU), Acharya, B.Ed.")
                .experience(19)
                .subjects("Sanskrit,Vedic Heritage,Moral Science")
                .classTeacherOf("6-A")
                .photoUrl("https://images.unsplash.com/photo-1545167622-3a6ac756afa4?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            Faculty.builder()
                .name("Ms. Sarah-Jane O'Connor")
                .email("sarahjane.oconnor@successacademy.edu.in")
                .phone("9876543116")
                .designation("Senior Foreign Language & French Faculty")
                .qualification("M.A. French Linguistics (Sorbonne), DELF C2 Certified")
                .experience(10)
                .subjects("French,European Culture,Global English")
                .photoUrl("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            Faculty.builder()
                .name("Mr. Gurmeet Singh Dhillon")
                .email("gurmeet.dhillon@successacademy.edu.in")
                .phone("9876543117")
                .designation("PRT Primary English & Phonics Instructor")
                .qualification("B.A. English Literature, D.El.Ed., Cambridge CELTA")
                .experience(5)
                .subjects("English,Phonics,Storytelling")
                .classTeacherOf("5-A")
                .photoUrl("https://images.unsplash.com/photo-1507591064344-4c6ce005b128?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            // ── 5. Sports, Co-Curricular & Creative Arts ──
            Faculty.builder()
                .name("Mr. Sanjay Mehta")
                .email("sanjay.mehta@successacademy.edu.in")
                .phone("9876543104")
                .designation("Physical Education Teacher & Sports HOD")
                .qualification("B.P.Ed., M.P.Ed.")
                .experience(12)
                .subjects("Physical Education,Sports")
                .photoUrl("https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            Faculty.builder()
                .name("Ms. Deepa Karmakar Nayak")
                .email("deepa.nayak@successacademy.edu.in")
                .phone("9876543118")
                .designation("PE Teacher, Yoga Acharya & Self-Defense Coach")
                .qualification("M.P.Ed. (LNIPE), National Gymnastics Medalist, Yoga Certified")
                .experience(7)
                .subjects("Physical Education,Yoga,Aerobics,Self-Defense")
                .photoUrl("https://images.unsplash.com/photo-1517841905240-472988babdf9?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            Faculty.builder()
                .name("Pandit Ustad Bismillah Murthy")
                .email("bismillah.murthy@successacademy.edu.in")
                .phone("9876543119")
                .designation("Head of Music & Performing Arts")
                .qualification("Sangeet Visharad, M.Mus. (Vocal & Instrumental)")
                .experience(24)
                .subjects("Classical Vocal,Instrumental Music,Keyboard,Tabla,Orchestra")
                .photoUrl("https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            Faculty.builder()
                .name("Mrs. Shilpa Chitnis")
                .email("shilpa.chitnis@successacademy.edu.in")
                .phone("9876543120")
                .designation("Master of Fine Arts & Craft Incharge")
                .qualification("B.F.A. Painting (JJ School of Art), M.F.A. Applied Art")
                .experience(13)
                .subjects("Visual Arts,Painting,Sculpture,Commercial Art,Clay Modeling")
                .photoUrl("https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            Faculty.builder()
                .name("Mrs. Margaret D'Souza")
                .email("margaret.dsouza@successacademy.edu.in")
                .phone("9876543121")
                .designation("Chief Librarian & Media Center Head")
                .qualification("M.Lib.I.Sc. (Master of Library Science), UGC-NET")
                .experience(16)
                .subjects("Library Science,Information Literacy,Reading Circle")
                .photoUrl("https://images.unsplash.com/photo-1548142813-c348350df52b?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            // ── 6. Foundational, Special Needs & Wellness ──
            Faculty.builder()
                .name("Sister Mary Roseanne")
                .email("mary.roseanne@successacademy.edu.in")
                .phone("9876543122")
                .designation("Pre-Primary Head & Early Childhood Specialist")
                .qualification("AMI Montessori Diploma, M.A. Child Psychology, B.Ed.")
                .experience(21)
                .subjects("Early Childhood Education,Phonics,Rhymes,Activity Learning")
                .classTeacherOf("UKG-A")
                .photoUrl("https://images.unsplash.com/photo-1566492031773-4f4e44671857?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            Faculty.builder()
                .name("Ms. Ananya Roy")
                .email("ananya.roy@successacademy.edu.in")
                .phone("9876543123")
                .designation("Nursery Educator & Sensory Play Coordinator")
                .qualification("Nursery Teacher Training (NTT), B.Sc. Home Science")
                .experience(4)
                .subjects("Play-Way Learning,Art & Craft,Rhymes")
                .classTeacherOf("Nursery-A")
                .photoUrl("https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            Faculty.builder()
                .name("Dr. Nidhi Chaurasia")
                .email("nidhi.chaurasia@successacademy.edu.in")
                .phone("9876543124")
                .designation("School Counselor & Student Wellness Educator")
                .qualification("M.Phil. Clinical Psychology (NIMHANS), Ph.D. Adolescent Counseling")
                .experience(11)
                .subjects("Psychology,Life Skills,Mental Health & Well-being")
                .photoUrl("https://images.unsplash.com/photo-1594744803329-e58b31de8bf5?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            Faculty.builder()
                .name("Mrs. Geeta Balakrishnan")
                .email("geeta.balakrishnan@successacademy.edu.in")
                .phone("9876543125")
                .designation("Special Educator (RCI Registered)")
                .qualification("B.Ed. in Special Education (Mental Retardation & Learning Disabilities), M.A. Sociology")
                .experience(9)
                .subjects("Special Education,Remedial Learning,Braille & Sign Basics")
                .photoUrl("https://images.unsplash.com/photo-1573497620053-ea5300f94f21?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            // ── 7. Edge Cases for Systematic Testing ──

            // Edge Case A: Single word mononym (No surname), null photo
            Faculty.builder()
                .name("Kavya")
                .email("kavya@successacademy.edu.in")
                .phone("9876543126")
                .designation("Assistant Science Teacher")
                .qualification("B.Sc. Physics, Chemistry, B.Ed.")
                .experience(2)
                .subjects("General Science")
                .classTeacherOf("6-B")
                .photoUrl(null) // Tests letter avatar fallback
                .status("Active")
                .build(),

            // Edge Case B: Extremely long name (52 characters), dual Ph.D.
            Faculty.builder()
                .name("Venkata Ramanujam Sundararajan Muthukrishnan Iyer")
                .email("venkata.iyer@successacademy.edu.in")
                .phone("9876543127")
                .designation("PGT Senior Sanskrit & Advanced Mathematics Scholar")
                .qualification("Dual Ph.D. in Vedic Mathematics & Sanskrit Grammar")
                .experience(34)
                .subjects("Vedic Mathematics,Advanced Geometry,Discrete Structures")
                .photoUrl("https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            // Edge Case C: Fresher / Trainee intern with exactly 0 years experience
            Faculty.builder()
                .name("Rohan Sharma")
                .email("rohan.sharma@successacademy.edu.in")
                .phone("9876543128")
                .designation("Trainee Teacher / Graduate Intern")
                .qualification("B.Sc. Computer Science (Graduated 2026)")
                .experience(0) // Tests 0 yrs exp
                .subjects("Basic Computer Skills,Robotics Club")
                .photoUrl(null)
                .status("Active")
                .build(),

            // Edge Case D: Maximum veteran experience (40 years)
            Faculty.builder()
                .name("Dr. B. R. Ambedkar Murthy")
                .email("ambedkar.murthy@successacademy.edu.in")
                .phone("9876543129")
                .designation("Professor Emeritus & Senior Academic Advisor")
                .qualification("Ph.D., D.Litt., Lifetime Fellow of Indian Science Academy")
                .experience(40)
                .subjects("Physics,Philosophy of Science")
                .photoUrl("https://images.unsplash.com/photo-1501196354995-cbb51c65aaea?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            // Edge Case E: INACTIVE - On extended maternity / sabbatical leave
            Faculty.builder()
                .name("Mrs. Shailaja Deshmukh")
                .email("shailaja.deshmukh@successacademy.edu.in")
                .phone("9876543130")
                .designation("TGT Social Sciences (On Sabbatical / Extended Leave)")
                .qualification("M.A. Political Science, B.Ed.")
                .experience(8)
                .subjects("Political Science,Social Studies")
                .photoUrl("https://images.unsplash.com/photo-1544717305-2782549b5136?w=200&h=200&fit=crop&crop=face")
                .status("Inactive") // Tests Inactive status filter & activation toggle
                .build(),

            // Edge Case F: INACTIVE - Resigned / contract concluded
            Faculty.builder()
                .name("Mr. Alok Nath Tripathi")
                .email("alok.tripathi@successacademy.edu.in")
                .phone("9876543131")
                .designation("Former PGT Physics Lecturer")
                .qualification("M.Sc. Physics, B.Ed.")
                .experience(12)
                .subjects("Physics")
                .photoUrl(null)
                .status("Inactive")
                .build(),

            // Edge Case G: INACTIVE - Transferred to affiliated campus
            Faculty.builder()
                .name("Ms. Fatima Noor Jehan")
                .email("fatima.jehan@successacademy.edu.in")
                .phone("9876543132")
                .designation("Former Primary Urdu & Arabic Faculty")
                .qualification("M.A. Urdu Literature, B.Ed.")
                .experience(6)
                .subjects("Urdu,Arabic Language")
                .photoUrl("https://images.unsplash.com/photo-1534751516642-a171edd272b5?w=200&h=200&fit=crop&crop=face")
                .status("Inactive")
                .build(),

            // Edge Case H: Broken/Invalid photo URL to test frontend onError image fallback
            Faculty.builder()
                .name("Mr. David K. Lyngdoh")
                .email("david.lyngdoh@successacademy.edu.in")
                .phone("9876543133")
                .designation("TGT Environmental Science & Nature Club Warden")
                .qualification("M.Sc. Ecology & Forestry (NEHU), B.Ed.")
                .experience(11)
                .subjects("Environmental Science,Ecology,Nature Conservation")
                .classTeacherOf("5-B")
                .photoUrl("https://broken-image-link-test.com/nonexistent_avatar.jpg")
                .status("Active")
                .build(),

            // Edge Case I: Empty subjects string to test fallback 'No subjects' chip
            Faculty.builder()
                .name("Swami Chinmayananda Saraswati")
                .email("chinmayananda@successacademy.edu.in")
                .phone("9876543134")
                .designation("Visiting Spiritual Mentor & Ethics Guide")
                .qualification("M.A. Philosophy, Acharya in Vedanta")
                .experience(30)
                .subjects("") // Empty subjects string
                .photoUrl("https://images.unsplash.com/photo-1546961329-78bef0414d7c?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build(),

            // Edge Case J: International name with Unicode accents & umlauts (é, ü)
            Faculty.builder()
                .name("Dr. Ren\u00e9e M\u00fcller-Schneider")
                .email("renee.mueller@successacademy.edu.in")
                .phone("9876543135")
                .designation("Visiting German Language Professor & International Relations")
                .qualification("Ph.D. in Germanic Philology (Heidelberg University)")
                .experience(16)
                .subjects("German Language,European History,Comparative Literature")
                .photoUrl("https://images.unsplash.com/photo-1580894732444-8ecded7900cd?w=200&h=200&fit=crop&crop=face")
                .status("Active")
                .build()
        );

        int addedCount = 0;
        for (Faculty faculty : sampleFaculty) {
            boolean exists = false;
            if (faculty.getEmail() != null && !faculty.getEmail().isBlank()) {
                exists = repository.existsByEmail(faculty.getEmail());
            }
            if (!exists && !repository.findByNameContainingIgnoreCase(faculty.getName()).isEmpty()) {
                exists = true;
            }

            if (!exists) {
                repository.save(faculty);
                addedCount++;
            }
        }

        log.info("\u2705 Faculty data initialization complete. {} new faculty records inserted (Total in DB: {}).",
                addedCount, repository.count());

        // Ensure all existing faculty records have operational fields populated
        repository.findAll().forEach(f -> {
            boolean modified = false;
            if (f.getFacultyCode() == null || f.getFacultyCode().isBlank()) {
                f.setFacultyCode(String.format("FAC-%04d", f.getId()));
                modified = true;
            }
            if (f.getDepartment() == null || f.getDepartment().isBlank()) {
                f.setDepartment("Academic");
                modified = true;
            }
            if (f.getEmploymentType() == null || f.getEmploymentType().isBlank()) {
                f.setEmploymentType("FULL_TIME");
                modified = true;
            }
            if (f.getJoiningDate() == null) {
                f.setJoiningDate(java.time.LocalDate.of(2022, 6, 1));
                modified = true;
            }
            if (f.getBaseSalary() == null) {
                f.setBaseSalary(java.math.BigDecimal.valueOf(35000.00));
                modified = true;
            }
            if (modified) {
                repository.save(f);
            }
        });
    }
}

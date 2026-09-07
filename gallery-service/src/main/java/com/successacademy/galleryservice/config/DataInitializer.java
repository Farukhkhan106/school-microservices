package com.successacademy.galleryservice.config;

import com.successacademy.galleryservice.model.GalleryItem;
import com.successacademy.galleryservice.repository.GalleryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final GalleryRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) return;

        String admin = "admin";

        Object[][] items = {
            // {title, description, imageUrl, category, publicVisible, displayOrder}
            {"Annual Sports Day 2024",      "Students competing in track and field events",           "https://images.unsplash.com/photo-1461896836934-b0303975883e?w=800&h=600&fit=crop", "Sports",    true,  1},
            {"Science Exhibition Winners",   "Award winning projects at annual science exhibition",    "https://images.unsplash.com/photo-1427504494785-3a9ca7044f45?w=800&h=600&fit=crop", "Academic",  true,  2},
            {"Annual Day Performance",       "Students performing classical dance on stage",           "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=800&h=600&fit=crop", "Cultural",  true,  3},
            {"Cricket Tournament Final",     "School cricket team winning the district tournament",    "https://images.unsplash.com/photo-1571260899304-425eee4c7efc?w=800&h=600&fit=crop", "Sports",    true,  4},
            {"Computer Lab Session",         "Students learning programming in smart computer lab",    "https://images.unsplash.com/photo-1509062522246-3755977927d7?w=800&h=600&fit=crop", "Academic",  true,  5},
            {"Independence Day Celebration", "Flag hoisting ceremony on Independence Day",             "https://images.unsplash.com/photo-1532375810709-75b1da00537c?w=800&h=600&fit=crop", "Events",    true,  6},
            {"School Building Front View",   "The beautiful main building of Success Academy",         "https://images.unsplash.com/photo-1580582932707-520aed937b7b?w=800&h=600&fit=crop", "Campus",    true,  7},
            {"Diwali Celebration",           "Students celebrating Diwali with lamps and sweets",      "https://images.unsplash.com/photo-1577896851231-70ef18881754?w=800&h=600&fit=crop", "Cultural",  true,  8},
            {"School Library",               "Students reading books in well-stocked school library",  "https://images.unsplash.com/photo-1503676260728-1c00da094a0b?w=800&h=600&fit=crop", "Campus",    true,  9},
            {"Yoga Day Celebration",         "Students and teachers participating in Yoga Day",        "https://images.unsplash.com/photo-1546410531-bb4caa6b424d?w=800&h=600&fit=crop", "Events",    true,  10},
            {"Republic Day March Past",      "Smart march past by NCC students on Republic Day",       "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800&h=600&fit=crop", "Events",    true,  11},
            {"Childrens Day Drama",          "Wonderful drama performance by Class 8 students",        "https://images.unsplash.com/photo-1503676260728-1c00da094a0b?w=800&h=600&fit=crop", "Cultural",  true,  12},
            {"Smart Classroom",              "Interactive smart board teaching session",               "https://images.unsplash.com/photo-1509062522246-3755977927d7?w=800&h=600&fit=crop", "Academic",  true,  13},
            {"Football Match",               "Inter-house football tournament semifinal",              "https://images.unsplash.com/photo-1571260899304-425eee4c7efc?w=800&h=600&fit=crop", "Sports",    true,  14},
            {"Science Lab Experiments",      "Students conducting chemistry experiments in lab",       "https://images.unsplash.com/photo-1427504494785-3a9ca7044f45?w=800&h=600&fit=crop", "Academic",  true,  15},
            {"School Garden",                "Beautiful garden maintained by Eco Club students",       "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800&h=600&fit=crop", "Campus",    true,  16},
            {"Teachers Day Celebration",     "Students honouring teachers on Teachers Day",            "https://images.unsplash.com/photo-1577896851231-70ef18881754?w=800&h=600&fit=crop", "Events",    true,  17},
            {"Prize Distribution",           "Principal giving prizes to meritorious students",        "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=800&h=600&fit=crop", "Events",    true,  18},
            {"Drawing Competition",          "Students showcasing their artistic talents",             "https://images.unsplash.com/photo-1503676260728-1c00da094a0b?w=800&h=600&fit=crop", "Cultural",  true,  19},
            {"School Canteen",               "Well-maintained hygienic canteen for students",          "https://images.unsplash.com/photo-1580582932707-520aed937b7b?w=800&h=600&fit=crop", "Campus",    true,  20},
        };

        for (Object[] item : items) {
            repository.save(GalleryItem.builder()
                    .title((String) item[0])
                    .description((String) item[1])
                    .imageUrl((String) item[2])
                    .category((String) item[3])
                    .publicVisible((boolean) item[4])
                    .active(true)
                    .displayOrder((int) item[5])
                    .createdBy(admin)
                    .build());
        }

        System.out.println("✅ Gallery seeded — 20 items.");
    }
}

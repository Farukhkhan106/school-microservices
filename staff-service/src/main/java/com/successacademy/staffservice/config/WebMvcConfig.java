package com.successacademy.staffservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get("uploads", "staff").toAbsolutePath().normalize();
        String uploadPath = uploadDir.toUri().toString();

        registry.addResourceHandler("/staff-service/uploads/staff/**")
                .addResourceLocations(uploadPath);

        registry.addResourceHandler("/uploads/staff/**")
                .addResourceLocations(uploadPath);
    }
}

package com.example.demo;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // مسار مجلد uploads على الخادم
        String uploadsPath = Paths.get("uploads").toFile().getAbsolutePath();
        
        // ربط الرابط /uploads/** بالمجلد الحقيقي
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/" + uploadsPath + "/")
                .addResourceLocations("file:" + uploadsPath + "/");
    }
}

package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
@EnableScheduling
public class MaweedApplication {

    public static void main(String[] args) {
        // Parse Railway DATABASE_URL if available
        String dbUrl = System.getenv("DATABASE_URL");
        if (dbUrl != null && dbUrl.startsWith("postgres")) {
            try {
                java.net.URI dbUri = new java.net.URI(dbUrl);
                String username = dbUri.getUserInfo().split(":")[0];
                String password = dbUri.getUserInfo().split(":")[1];
                String jdbc = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath() + "?sslmode=disable";
                System.setProperty("spring.datasource.url", jdbc);
                System.setProperty("spring.datasource.username", username);
                System.setProperty("spring.datasource.password", password);
                System.out.println("✅ Database configured from Railway DATABASE_URL");
            } catch (Exception e) {
                System.err.println("⚠️ Failed to parse DATABASE_URL: " + e.getMessage());
            }
        }
        SpringApplication.run(MaweedApplication.class, args);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

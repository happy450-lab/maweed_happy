package com.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // 1. فتح مسارات تسجيل الدخول والتسجيل للمرضى
                        .requestMatchers("/api/auth/**").permitAll()

                        // 2. فتح مسارات تسجيل الدكاترة عشان يقدروا يسجلوا من غير Token
                        // لاحظ إننا استخدمنا doctors (جمع) عشان تطابق الـ RequestMapping في الكنترولر
                        .requestMatchers("/api/doctors/register").permitAll()
                        .requestMatchers("/api/doctors/activate").permitAll()

                        // 3. فتح مسارات الـ Media لو حبيت تعرض الشهادات المرفوعة
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        // باقي المسارات اللي فتحناها قبل كدة
                        .requestMatchers("/api/auth/register").permitAll()
                        .requestMatchers("/api/doctors/register").permitAll()
                        .requestMatchers("/api/doctors/**").permitAll()
                        // 4. حماية باقي ميثودات الدكاترة (زي عرض القائمة أو الرفع)
                        // دي ممكن تفتحها PermitAll دلوقتي للتجربة وبعدين تقفلها

                        // 5. فتح الحجوزات 
                        .requestMatchers("/api/appointments/**").permitAll()

                        // 6. فتح مسارات لوحة التحكم (المدير)
                        .requestMatchers("/api/admin/**").permitAll()

                        // 7. فتح مسارات الروشتات
                        .requestMatchers("/api/prescriptions/**").permitAll()

                        // 8. أي طلب آخر يحتاج تسجيل دخول
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "https://maweed-ui.vercel.app")); // السماح للمحلي ولموقع Vercel
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
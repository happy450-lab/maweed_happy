package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // ✅ Public — بدون توكن
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/doctors/register").permitAll()
                .requestMatchers("/api/doctors/activate").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/doctors").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/doctors/{id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/doctors/search-by-code/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/doctors/*/working-hours").permitAll()
                .requestMatchers("/uploads/**", "/files/**").permitAll()
                .requestMatchers("/ws-maweed/**").permitAll()

                .requestMatchers("/api/admin/**").permitAll() // يُدار بواسطة AdminSecurityFilter المستقل في Order(1)
                .requestMatchers(HttpMethod.GET, "/api/reviews/recent-high").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/doctors/top").permitAll()
                .requestMatchers("/error").permitAll() // لعدم حجب أخطاء 500 وتحويلها لـ 403
                
                // 🔐 بقية الـ APIs تتطلب توكن JWT صالح
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:3000", "https://maweed-ui.vercel.app"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "x-national-id"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
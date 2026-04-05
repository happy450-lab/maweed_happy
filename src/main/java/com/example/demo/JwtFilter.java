package com.example.demo;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 🔐 JwtFilter — يفحص كل Request ويتحقق من التوكن
 * لو التوكن صحيح → يدخل الـ Security Context
 * لو غلط أو مافيش → يمر (الـ SecurityConfig هيقرر يرفض أو يقبل)
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private com.example.demo.repository.UserRepository userRepository;
    
    @Autowired
    private com.example.demo.repository.DoctorRepository doctorRepository;
    
    @Autowired
    private com.example.demo.repository.AssistantRequestRepository assistantRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtUtil.isTokenValid(token)) {
                    String nationalId = jwtUtil.extractNationalId(token);
                    String role = jwtUtil.extractRole(token);
                    
                    boolean isActive = false;
                    
                    if (role.equals("ROLE_PATIENT")) {
                        var user = userRepository.findByNationalId(nationalId);
                        if (user.isPresent() && token.equals(user.get().getActiveToken())) {
                            isActive = true;
                        }
                    } else if (role.equals("ROLE_DOCTOR")) {
                        var doctor = doctorRepository.findByNationalId(nationalId);
                        // Doctor token requires checking both activeToken and if they have been approved
                        if (doctor.isPresent() && token.equals(doctor.get().getActiveToken())) {
                            isActive = true;
                        }
                    } else if (role.equals("ROLE_ACCOUNTANT")) {
                        var assistants = assistantRepository.findByAssistantNationalId(nationalId);
                        for (var a : assistants) {
                            if (token.equals(a.getActiveToken()) && "APPROVED".equals(a.getStatus())) {
                                isActive = true;
                                break;
                            }
                        }
                    }

                    if (isActive) {
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                nationalId, null,
                                List.of(new SimpleGrantedAuthority(role))
                        );
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    } else {
                        // توكن قديم (مكتوب فوقه بتسجيل دخول جديد)
                        System.out.println("🚨 قفل جلسة قديمة للرقم: " + nationalId);
                        SecurityContextHolder.clearContext();
                    }
                }
            } catch (Exception e) {
                // توكن خاطئ — نتجاهله
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}

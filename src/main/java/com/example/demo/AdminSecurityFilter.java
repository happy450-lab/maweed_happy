package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 🔐 AdminSecurityFilter — يحمي كل endpoints /api/admin/*
 * كلمة السر محفوظة في application.properties → تُغيَّر من Railway Variables
 */
@Component
@Order(1)
public class AdminSecurityFilter implements Filter {

    @Value("${admin.secret.token}")
    private String adminSecretToken;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if (req.getRequestURI().startsWith("/api/admin") && !req.getMethod().equalsIgnoreCase("OPTIONS")) {
            String authHeader = req.getHeader("Authorization");
            String expectedToken = "Bearer " + adminSecretToken.trim();

            if (authHeader == null || !authHeader.trim().equals(expectedToken)) {
                System.out.println("🚨 Admin access attempt BLOCKED — URI: " + req.getRequestURI());
                System.out.println("   -> Received: [" + authHeader + "]");
                System.out.println("   -> Expected: [" + expectedToken + "]");
                
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"error\":\"Forbidden: Invalid or missing Admin Token\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}

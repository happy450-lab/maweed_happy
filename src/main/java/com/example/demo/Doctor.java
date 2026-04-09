package com.example.demo;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "doctors")
@Data
@NoArgsConstructor
public class Doctor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nationalId;

    @Column(nullable = false)
    private String NameDoctor;
    @Column(nullable = false, unique = true, length = 20)
    private String phoneNumberDoctor;
    private String specialization;

    @Column(name = "doctor_certificate")
    private String doctorCertificate;
    
    // Default password to satisfy existing NOT NULL database constraints without using the frontend
    @Column(nullable = false)
    private String password = "N/A";

    private Long doctorOwnerId;
    @Column(nullable = false, unique = true, length = 20)
    private String specialAccessCode = "P-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
// يتولد من الأدمن بعد القبول فقط


    private String role; // بنخزنه كـ String عشان التوافق مع الخدمة
    @Column(length = 500)
    private String doctorPhoto;
 // صورة الطبيب (مسار الصورة)
    private boolean enabled = false;
    private boolean approved = false;
    private String location; // المنطقة/المحافظة

    @Column(length = 500)
    private String coverPhoto;   // صورة غلاف العيادة

    private Double checkupPrice; // سعر الكشف
    private Double recheckPrice; // سعر الإعادة

    @Column(length = 1000)
    private String googleMapsLink; // رابط خرائط جوجل

    @Column(columnDefinition = "TEXT")
    private String aboutDoctor; 

    @Column(columnDefinition = "TEXT")
    private String qualifications; 

    private LocalDateTime subscriptionEndDate; // نهاية الاشتراك

    private Double averageRating = 0.0;
    private Integer totalReviews = 0;

    @Column(length = 500)
    private String activeToken; // تتبع الجلسة الحالية لمنع الدخول المتعدد
}
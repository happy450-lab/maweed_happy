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
    @Column(nullable = false)
private  String password;
    private Long doctorOwnerId;
    @Column(nullable = false, unique = true, length = 10)
    private String specialAccessCode;
// يتولد من الأدمن بعد القبول فقط


    private String role; // بنخزنه كـ String عشان التوافق مع الخدمة
    @Column(length = 500)
    private String doctorPhoto;
 // صورة الطبيب (مسار الصورة)
    private boolean enabled = false;
    private boolean approved = false;
    private String location; // المنطقة/المحافظة
}
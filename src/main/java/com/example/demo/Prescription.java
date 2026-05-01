package com.example.demo;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "medical_prescriptions")
@Data
@NoArgsConstructor
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // جعل الحقول اختيارية في قاعدة البيانات لضمان الحفظ حتى لو نقص بيان واحد
    @Column(nullable = true)
    private String doctorNationalId;

    @Column(nullable = true)
    private String doctorName;

    @Column(nullable = true)
    private String patientNationalId;

    @Column(nullable = true)
    @Convert(converter = AttributeEncryptor.class)
    private String patientName;

    @Column(nullable = true)
    private String doctorSpecialization;

    @Column(nullable = true)
    private String consultationDate;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = AttributeEncryptor.class)
    private String diagnosis;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = AttributeEncryptor.class)
    private String medicines;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = AttributeEncryptor.class)
    private String instructions;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}

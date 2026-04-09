package com.example.demo;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
public class Appointment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String patientNationalId;

    @Column(nullable = false)
    private String patientName;

    @Column(nullable = false)
    private String doctorNationalId;

    @Column(nullable = false)
    private String doctorName;

    @Column(nullable = false)
    private LocalDate appointmentDate;

    @Column(nullable = false)
    private LocalTime appointmentTime;

    // حالة الحجز: PENDING, APPROVED, REJECTED
    @Column(nullable = false)
    private String status = "PENDING";

    @Column(nullable = true)
    private String visitType; // كشف أو إعادة

    @Column(columnDefinition = "boolean default false")
    private Boolean isReviewed = false;
}

package com.example.demo.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "doctor_archive_patients")
public class DoctorArchivePatient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String doctorNationalId;
    private String patientName;
    private String patientPhone;
    private String visitYear;
    private LocalDateTime createdAt = LocalDateTime.now();

    public DoctorArchivePatient() {
    }

    public DoctorArchivePatient(String doctorNationalId, String patientName, String patientPhone) {
        this.doctorNationalId = doctorNationalId;
        this.patientName = patientName;
        this.patientPhone = patientPhone;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDoctorNationalId() {
        return doctorNationalId;
    }

    public void setDoctorNationalId(String doctorNationalId) {
        this.doctorNationalId = doctorNationalId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientPhone() {
        return patientPhone;
    }

    public void setPatientPhone(String patientPhone) {
        this.patientPhone = patientPhone;
    }

    public String getVisitYear() {
        return visitYear;
    }

    public void setVisitYear(String visitYear) {
        this.visitYear = visitYear;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

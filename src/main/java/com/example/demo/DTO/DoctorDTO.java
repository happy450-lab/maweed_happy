package com.example.demo.DTO;

import jakarta.persistence.Column;
import lombok.Data;

@Data
public class DoctorDTO {
    private String NameDoctor;
    private String nationalId;
    private String phoneNumberDoctor;
    private String specialization;
    @Column(nullable = false, unique = true, length = 20)
    private String  specialAccessCode;

}

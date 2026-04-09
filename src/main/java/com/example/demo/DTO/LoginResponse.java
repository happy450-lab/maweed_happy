package com.example.demo.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String nationalId;
    private String fullName;
    private String role;
    private String doctorNationalId; // للمساعدين
    private String specialization;   // للأطباء
    private String photo;            // صورة الطبيب
    private String token;            // 🔐 JWT Token

    public LoginResponse(String nationalId, String fullName, String role, String doctorNationalId, String specialization, String photo) {
        this.nationalId = nationalId;
        this.fullName = fullName;
        this.role = role;
        this.doctorNationalId = doctorNationalId;
        this.specialization = specialization;
        this.photo = photo;
    }
}

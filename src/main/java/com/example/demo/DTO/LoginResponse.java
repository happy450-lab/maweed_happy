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
    private String doctorNationalId; // الحقل المهم للمساعدين
    private String specialization; // للأطباء
    private String photo; // صورة الطبيب
}

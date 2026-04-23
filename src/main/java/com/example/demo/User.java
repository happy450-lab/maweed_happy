package com.example.demo;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 14)
    private String nationalId;

    @NotBlank(message = "الاسم م ينفعش يكون فاضي")
    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true, length = 20)
    private String phoneNumber;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean enabled = false;
    @Column(columnDefinition = "int default 0")
    private int noShowCount = 0; // عدد غيابات المريض — يؤدي للحظر بعد 3 مرات
    private LocalDateTime createDate = LocalDateTime.now();

    @Transient
    @JsonProperty("doctorNationalId") 
    private String doctorNationalId; // الحقل ده مش بيتحفظ في الداتابيز،بنستخدمه بس للرد على اللوجن للمساعدين

    // -- Medical Profile Data --
    private String bloodType;
    private Double weight;
    private Double height;
    private Integer age;
    
    @Column(length = 1000)
    private String chronicDiseases;
    
    @Column(length = 1000)
    private String allergies;

    @Column(length = 500)
    private String activeToken; // تتبع الجلسة الحالية لمنع الدخول المتعدد
}
package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "assistant_requests")
public class AssistantRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String doctorNationalId;

    @Column(nullable = false)
    private String assistantNationalId;

    @Column(nullable = false)
    private String doctorCode;

    // تم إضافة الأسماء حتى يسهل على المدير التعرف عليهم
    @Column
    private String doctorName;

    @Column
    private String assistantName;

    // حالة الطلب: PENDING (قيد المراجعة), APPROVED (موافق عليه), REJECTED (مرفوض)
    @Column(nullable = false)
    private String status = "PENDING";

    @Column(length = 500)
    private String activeToken; // تتبع الجلسة الحالية لمنع الدخول المتعدد
}

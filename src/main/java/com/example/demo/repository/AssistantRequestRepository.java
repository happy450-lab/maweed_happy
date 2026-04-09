package com.example.demo.repository;

import com.example.demo.domain.AssistantRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssistantRequestRepository extends JpaRepository<AssistantRequest, Long> {
    
    // للبحث عن المساعد اثناء الدخول (هل تمت الموافقة عليه من قبل هذا الدكتور؟)
    Optional<AssistantRequest> findByAssistantNationalIdAndDoctorCodeAndStatus(String assistantNationalId, String doctorCode, String status);

    // لمعرفة عدد المساعدين المعتمدين لدكتور معين (عشان نحددهم بـ 3)
    long countByDoctorNationalIdAndStatus(String doctorNationalId, String status);

    // لجلب طلبات أو مساعدين دكتور معين
    List<AssistantRequest> findByDoctorNationalId(String doctorNationalId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByDoctorNationalId(String doctorNationalId);

    // للتأكد إن الدكتور مبعتش نفس الطلب قبل كده لنفس الرقم القومي
    Optional<AssistantRequest> findByAssistantNationalIdAndDoctorNationalId(String assistantNationalId, String doctorNationalId);
    
    // لجلب طلبات أو مساعدين برقمهم القومي
    List<AssistantRequest> findByAssistantNationalId(String assistantNationalId);

    // لعدد كل الطلبات بحالة معينة (notification badge)
    long countByStatus(String status);
    
    // Fallback: للبحث عن المساعد اثناء الدخول (للحسابات القديمة التي لا تحتوي على حالة)
    Optional<AssistantRequest> findByAssistantNationalIdAndDoctorCode(String assistantNationalId, String doctorCode);
}


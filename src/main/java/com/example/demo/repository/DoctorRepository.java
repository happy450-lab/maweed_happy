package com.example.demo.repository;

import com.example.demo.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    boolean existsByNationalId(String nationalId);

    Optional<Doctor> findByNationalId(String nationalId);

    Optional<Doctor> findBySpecialAccessCode(String specialAccessCode);

    // لجلب الدكاترة اللي لسه مش متقبلوش (طلبات تسجيل جديدة)
    List<Doctor> findByApprovedFalse();

    // لعدد الطلبات المعلقة (notification badge)
    long countByApprovedFalse();

    // جلب أفضل 5 أطباء حسب التقييم
    List<Doctor> findTop5ByApprovedTrueAndEnabledTrueOrderByAverageRatingDesc();

    // جلب جميع الدكاترة الموافق عليهم والمفعلين (للصفحة العامة)
    List<Doctor> findByApprovedTrueAndEnabledTrue();
}

package com.example.demo.repository;

import com.example.demo.domain.DoctorArchivePatient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorArchivePatientRepository extends JpaRepository<DoctorArchivePatient, Long> {
    List<DoctorArchivePatient> findByDoctorNationalId(String doctorNationalId);
    boolean existsByDoctorNationalIdAndPatientPhone(String doctorNationalId, String patientPhone);
}

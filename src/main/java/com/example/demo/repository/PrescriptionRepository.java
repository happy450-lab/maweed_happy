package com.example.demo.repository;

import com.example.demo.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByPatientNationalId(String patientNationalId);
    List<Prescription> findByDoctorNationalId(String doctorNationalId);
}

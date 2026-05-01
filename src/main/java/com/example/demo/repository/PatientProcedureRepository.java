package com.example.demo.repository;

import com.example.demo.domain.PatientProcedure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientProcedureRepository extends JpaRepository<PatientProcedure, Long> {
    List<PatientProcedure> findByDoctorNationalId(String doctorNationalId);
    List<PatientProcedure> findByDoctorNationalIdAndPatientNationalId(String doctorNationalId, String patientNationalId);
    List<PatientProcedure> findByPatientNationalId(String patientNationalId);
}

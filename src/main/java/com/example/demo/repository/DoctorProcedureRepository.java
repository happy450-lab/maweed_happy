package com.example.demo.repository;

import com.example.demo.Doctor;
import com.example.demo.domain.DoctorProcedure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorProcedureRepository extends JpaRepository<DoctorProcedure, Long> {
    List<DoctorProcedure> findByDoctor(Doctor doctor);
    void deleteByDoctor(Doctor doctor);
}

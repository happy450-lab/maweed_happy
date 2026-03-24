package com.example.demo.repository;

import com.example.demo.domain.WorkingHour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkingHourRepository extends JpaRepository<WorkingHour, Long> {
    List<WorkingHour> findByDoctorNationalId(String nationalId);
    void deleteByDoctorNationalId(String nationalId);
}

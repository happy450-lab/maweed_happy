package com.example.demo.repository;

import com.example.demo.domain.IpRegistrationLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IpRegistrationLimitRepository extends JpaRepository<IpRegistrationLimit, String> {
    // findById(ipAddress) من الـ JpaRepository كافي تماماً
}

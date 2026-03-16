package com.example.demo.repository;

import com.example.demo.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface UserRepository  extends JpaRepository<User,Long> {
    Optional<User> findByNationalId(String nationalId);

     // ميثود لو حبيت تتأكد إن الرقم القومي موجود قبل كدة ولا لأ عشان متطلعش Error
             boolean existsByNationalId(String nationalId);
     //ميثود عشان لو حد سجل بالهاتف قبل كده
             boolean existsByPhoneNumber(String phoneNumber);
     //  لو حبيت تبحث عن المرضى برقم الموبايل
             Optional<User> findByPhoneNumber(String phoneNumber);
             Optional<User> findByNationalIdAndPhoneNumber(String nationalId, String phoneNumber);


}

package com.example.demo.repository;

import com.example.demo.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    // جلب أحدث التقييمات الإيجابية (مثلاً التقييم 4 فما فوق)
    List<Review> findTop10ByRatingGreaterThanEqualOrderByCreatedAtDesc(Integer rating);

    // جلب أحدث 10 تعليقات بغض النظر عن التقييم
    List<Review> findTop10ByOrderByCreatedAtDesc();

    // جلب جميع تقييمات دكتور معين
    List<Review> findByDoctorNationalIdOrderByCreatedAtDesc(String doctorNationalId);
}

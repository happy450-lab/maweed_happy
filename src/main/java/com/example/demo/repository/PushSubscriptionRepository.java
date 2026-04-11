package com.example.demo.repository;

import com.example.demo.domain.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    // جيب كل subscriptions لمستخدم معين (ممكن يبقى عنده أكتر من جهاز)
    List<PushSubscription> findByNationalId(String nationalId);

    // امسح الـ subscription لما المتصفح يرفضها
    void deleteByEndpoint(String endpoint);

    // هل المستخدم مسجل؟
    boolean existsByNationalIdAndEndpoint(String nationalId, String endpoint);
}

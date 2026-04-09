package com.example.demo;

import com.example.demo.repository.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ✅ مهمة تلقائية تعمل كل يوم الساعة 12 ليلاً
 * تفحص اشتراكات الأطباء وتوقف الحسابات المنتهية بعد فترة التسامح (5 أيام)
 */
@Component
public class SubscriptionScheduler {

    @Autowired
    private DoctorRepository doctorRepository;

    // يشتغل كل يوم الساعة 12:00 ليلاً
    @Scheduled(cron = "0 0 0 * * *")
    public void disableExpiredDoctors() {
        System.out.println("⏰ [Scheduler] فحص اشتراكات الأطباء المنتهية...");

        // الحد الأدنى: الاشتراك انتهى من 5 أيام أو أكثر → تعطيل
        LocalDateTime gracePeriodDeadline = LocalDateTime.now().minusDays(5);

        List<Doctor> doctors = doctorRepository.findAll();
        int disabledCount = 0;

        for (Doctor doctor : doctors) {
            // لو الاشتراك موجود وانتهى من أكثر من 5 أيام والحساب لسه مفعّل
            if (doctor.getSubscriptionEndDate() != null
                    && doctor.getSubscriptionEndDate().isBefore(gracePeriodDeadline)
                    && doctor.isEnabled()) {

                doctor.setEnabled(false);
                doctorRepository.save(doctor);
                disabledCount++;
                System.out.println("🔴 [Scheduler] تم تعطيل حساب الدكتور: " + doctor.getNameDoctor()
                        + " | انتهى الاشتراك: " + doctor.getSubscriptionEndDate());
            }

            // 🚨 فحص التحايل (Daily Ban) والحسابات غير المعتمدة
            if (!doctor.isApproved() && doctor.isEnabled()) {
                doctor.setEnabled(false);
                doctor.setActiveToken(null);
                doctor.setSpecialAccessCode(null);
                doctorRepository.save(doctor);
                System.out.println("🚨 ⛔ [Security Guard] تم تبنيد وحظر حساب بشكل فوري لمحاولته التواجد كحساب رسمي وهو لم يستلم كود أو غير معتمد: " + doctor.getNameDoctor() + " | بطاقة: " + doctor.getNationalId());
            }
        }

        System.out.println("✅ [Scheduler] انتهى الفحص. عدد الحسابات المعطّلة: " + disabledCount);
    }
}

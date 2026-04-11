package com.example.demo;

import com.example.demo.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * ✅ AppointmentReminderScheduler
 * بيشتغل كل دقيقة ويبعت تذكير للمرضى اللي موعدهم بعد 20 دقيقة
 */
@Component
public class AppointmentReminderScheduler {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PushNotificationService pushService;

    @Scheduled(fixedRate = 60_000) // كل دقيقة
    public void sendAppointmentReminders() {
        LocalDate today = LocalDate.now();
        LocalTime now   = LocalTime.now();

        // نافذة الـ 20 دقيقة: من 19 لـ 21 دقيقة قدام
        LocalTime from = now.plusMinutes(19);
        LocalTime to   = now.plusMinutes(21);

        // جيب المواعيد اللي في النافذة دي ولسه ماتبعتلهاش تذكير
        List<com.example.demo.Appointment> upcoming =
            appointmentRepository.findRemindersToSend(today, from, to);

        for (com.example.demo.Appointment appt : upcoming) {
            try {
                // تنسيق الوقت بشكل مقروء
                int h  = appt.getAppointmentTime().getHour();
                int m  = appt.getAppointmentTime().getMinute();
                String ampm = h < 12 ? "ص" : "م";
                int h12 = h == 0 ? 12 : h > 12 ? h - 12 : h;
                String timeStr = String.format("%d:%02d %s", h12, m, ampm);

                pushService.sendToUser(
                    appt.getPatientNationalId(),
                    "⏰ تذكير: موعدك بعد 20 دقيقة!",
                    "موعدك مع " + appt.getDoctorName() + " الساعة " + timeStr
                );

                // ضع علامة إن الرسالة اتبعتت
                appt.setReminderSent(true);
                appointmentRepository.save(appt);

            } catch (Exception e) {
                System.err.println("⚠️ خطأ في إرسال تذكير الموعد: " + e.getMessage());
            }
        }
    }
}

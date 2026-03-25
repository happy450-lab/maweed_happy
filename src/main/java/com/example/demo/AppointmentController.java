package com.example.demo;

import com.example.demo.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = {"http://localhost:3000", "https://maweed-ui.vercel.app"})
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentRepository appointmentRepository;

    // الحد الأقصى للوقت بين الحجوزات (20 دقيقة)
    private static final int MIN_GAP_MINUTES = 20;

    /**
     * 1. حجز موعد جديد (من قبل المريض)
     */
    @PostMapping("/book")
    public ResponseEntity<?> bookAppointment(@RequestBody Appointment appointment, jakarta.servlet.http.HttpServletRequest request) {
        try {
            // حماية الـ Endpoint: التأكد من أن الطلب قادم من الموقع الرسمي فقط وليس من Postman أو أي سيرفر آخر
            String origin = request.getHeader("Origin");
            String referer = request.getHeader("Referer");
            
            boolean isValidSource = false;
            if (origin != null && (origin.equals("http://localhost:3000") || origin.equals("https://maweed-ui.vercel.app"))) {
                isValidSource = true;
            } else if (referer != null && (referer.startsWith("http://localhost:3000") || referer.startsWith("https://maweed-ui.vercel.app"))) {
                isValidSource = true;
            }

            if (!isValidSource) {
                System.out.println("🚨 تم حظر محاولة حجز خارجية! Origin: " + origin + ", Referer: " + referer);
                return ResponseEntity.status(403).body("غير مصرح لك بإجراء الحجز. يجب أن يتم الحجز من خلال الموقع الرسمي فقط.");
            }
            // تنظيف الأرقام القومية من أي مسافات
            if (appointment.getDoctorNationalId() != null)
                appointment.setDoctorNationalId(appointment.getDoctorNationalId().trim());
            if (appointment.getPatientNationalId() != null)
                appointment.setPatientNationalId(appointment.getPatientNationalId().trim());

            // 1. فحص قاعدة: ميعاد واحد للمريض مع نفس الدكتور خلال الـ 3 أيام كحد أقصى
            LocalDate today = LocalDate.now();
            LocalDate dayAfterTomorrow = today.plusDays(2);
            
            List<Appointment> patientApptsWithThisDoctor = appointmentRepository
                    .findByPatientAndDoctorInDateRange(
                            appointment.getPatientNationalId(), 
                            appointment.getDoctorNationalId(), 
                            today, 
                            dayAfterTomorrow
                    );
                    
            if (!patientApptsWithThisDoctor.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        "لقد قمت بحجز موعد مع هذا الطبيب مسبقاً وتنتظر الكشف. لا يمكنك حجز أكثر من موعد واحد لنفس الطبيب خلال فترة الثلاثة أيام المتاحة."
                );
            }

            // 2. فحص التعارض الزمني للدكتور (أقل من 20 دقيقة)
            List<Appointment> existingAppointments = appointmentRepository
                    .findByDoctorNationalIdAndAppointmentDate(
                            appointment.getDoctorNationalId(),
                            appointment.getAppointmentDate()
                    );

            LocalTime newTime = appointment.getAppointmentTime();

            // فحص التعارض: السماح لمريضين كحد أقصى في نفس الموعد
            int overlapCount = 0;
            for (Appointment existing : existingAppointments) {
                if ("REJECTED".equals(existing.getStatus()) || "CANCELLED".equals(existing.getStatus())) {
                    continue;
                }

                LocalTime existingTime = existing.getAppointmentTime();
                long diffMinutes = Math.abs(
                        newTime.toSecondOfDay() - existingTime.toSecondOfDay()
                ) / 60;

                if (diffMinutes < MIN_GAP_MINUTES) {
                    overlapCount++;
                }
            }

            if (overlapCount >= 2) {
                return ResponseEntity.badRequest().body(
                        "عفواً، اكتمل العدد الأقصى لهذا الموعد (مريضين كحد أقصى)."
                );
            }

            Appointment saved = appointmentRepository.save(appointment);
            System.out.println("✅ Appointment saved: doctorNationalId=" + saved.getDoctorNationalId() + ", patient=" + saved.getPatientNationalId());
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("خطأ في تسجيل الحجز: " + e.getMessage());
        }
    }


    /**
     * 2. جلب حجوزات الطبيب (للوحة تحكم الطبيب)
     */
    @GetMapping("/doctor/{nationalId}")
    public ResponseEntity<List<Appointment>> getDoctorAppointments(@PathVariable String nationalId) {
        String trimmedId = nationalId.trim();
        System.out.println("📋 Fetching appointments for doctorNationalId: [" + trimmedId + "]");
        List<Appointment> appointments = appointmentRepository.findByDoctorNationalId(trimmedId);
        System.out.println("📋 Found: " + appointments.size() + " appointments");
        return ResponseEntity.ok(appointments);
    }

    /**
     * 3. جلب حجوزات المريض (للوحة تحكم المريض - حجوزاتي)
     */
    @GetMapping("/patient/{nationalId}")
    public ResponseEntity<List<Appointment>> getPatientAppointments(@PathVariable String nationalId) {
        List<Appointment> appointments = appointmentRepository.findByPatientNationalId(nationalId);
        return ResponseEntity.ok(appointments);
    }

    /**
     * 4. تغيير حالة الحجز (مقبول / مرفوض) من لوحة الطبيب
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Optional<Appointment> optionalAppointment = appointmentRepository.findById(id);
        if (optionalAppointment.isPresent()) {
            Appointment appointment = optionalAppointment.get();
            String newStatus = payload.get("status"); // PENDING, APPROVED, REJECTED
            if (newStatus != null) {
                String upperStatus = newStatus.toUpperCase();
                if (upperStatus.equals("PENDING") || upperStatus.equals("APPROVED") || upperStatus.equals("REJECTED") || upperStatus.equals("DONE")) {
                    appointment.setStatus(upperStatus);
                    appointmentRepository.save(appointment);
                    return ResponseEntity.ok(appointment);
                } else {
                    return ResponseEntity.badRequest().body("حالة الحجز غير مدعومة");
                }
            }
        }
        return ResponseEntity.notFound().build();
    }
    /**
     * 5. حذف حجز
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAppointment(@PathVariable Long id) {
        if (appointmentRepository.existsById(id)) {
            appointmentRepository.deleteById(id);
            return ResponseEntity.ok("تم حذف الحجز بنجاح");
        }
        return ResponseEntity.notFound().build();
    }
}


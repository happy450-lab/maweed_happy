package com.example.demo;

import com.example.demo.domain.WorkingHour;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WorkingHourRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private WorkingHourRepository workingHourRepository;

    // الحد الأقصى للوقت بين الحجوزات (20 دقيقة)
    private static final int MIN_GAP_MINUTES = 20;

    // تحويل يوم الأسبوع الإنجليزي إلى العربي (لمطابقة قاعدة البيانات)
    private String toArabicDay(DayOfWeek dow) {
        switch (dow) {
            case SATURDAY:  return "السبت";
            case SUNDAY:    return "الأحد";
            case MONDAY:    return "الاثنين";
            case TUESDAY:   return "الثلاثاء";
            case WEDNESDAY: return "الأربعاء";
            case THURSDAY:  return "الخميس";
            case FRIDAY:    return "الجمعة";
            default:        return "";
        }
    }

    /**
     * Helper to get current principal
     */
    private String getCurrentUser() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && !auth.getPrincipal().equals("anonymousUser")) ? auth.getName() : null;
    }

    private String getCurrentRole() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !auth.getAuthorities().isEmpty() && !auth.getPrincipal().equals("anonymousUser")) {
            return auth.getAuthorities().iterator().next().getAuthority();
        }
        return null;
    }

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

            // 🔒 التحقق من الهوية (IDOR Protection)
            String curRole = getCurrentRole();
            String curUser = getCurrentUser();
            if ("ROLE_PATIENT".equals(curRole) && !curUser.equals(appointment.getPatientNationalId())) {
                return ResponseEntity.status(403).body("لا يمكنك حجز موعد لمريض آخر.");
            }

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

            // 3. فحص أيام الإجازة: تأكد أن اليوم المختار ليس يوم إجازة للدكتور
            String doctorNatId = appointment.getDoctorNationalId();
            List<WorkingHour> workingHours = workingHourRepository.findByDoctorNationalId(doctorNatId);
            if (!workingHours.isEmpty()) {
                String arabicDay = toArabicDay(appointment.getAppointmentDate().getDayOfWeek());
                boolean isOffDay = workingHours.stream()
                        .anyMatch(wh -> wh.getDayOfWeek().equals(arabicDay) && wh.isOff());
                if (isOffDay) {
                    return ResponseEntity.badRequest().body(
                            "عفواً، الطبيب في إجازة يوم " + arabicDay + ". يرجى اختيار يوم عمل آخر."
                    );
                }
            }

            Appointment saved = appointmentRepository.save(appointment);
            
            // بث الإشعار عبر الـ WebSocket
            messagingTemplate.convertAndSend("/topic/appointments/" + saved.getDoctorNationalId(), "NEW_APPOINTMENT");

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

        // 🔒 التحقق من الهوية (IDOR Protection)
        String curRole = getCurrentRole();
        String curUser = getCurrentUser();
        if ("ROLE_DOCTOR".equals(curRole) && !trimmedId.equals(curUser)) {
            return ResponseEntity.status(403).build();
        }

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
        // 🔒 التحقق من الهوية (IDOR Protection)
        if ("ROLE_PATIENT".equals(getCurrentRole()) && !nationalId.equals(getCurrentUser())) {
            return ResponseEntity.status(403).build();
        }

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

            // 🔒 التحقق من الهوية
            String curRole = getCurrentRole();
            String curUser = getCurrentUser();
            if ("ROLE_DOCTOR".equals(curRole) && !apartmentNationalIdMatchesDoctor(appointment, curUser)) {
                return ResponseEntity.status(403).body("غير مصرح لك بتعديل حالة حجز لطبيب آخر.");
            }

            String newStatus = payload.get("status"); // PENDING, APPROVED, REJECTED
            if (newStatus != null) {
                String upperStatus = newStatus.toUpperCase();
                if (upperStatus.equals("PENDING") || upperStatus.equals("APPROVED") || upperStatus.equals("REJECTED") || upperStatus.equals("DONE") || upperStatus.equals("NO_SHOW") || upperStatus.equals("ARRIVED")) {
                    appointment.setStatus(upperStatus);
                    appointmentRepository.save(appointment);

                    // 🚨 زيادة عدد الغيابات إذا كانت الحالة NO_SHOW
                    if (upperStatus.equals("NO_SHOW")) {
                        userRepository.findByNationalId(appointment.getPatientNationalId()).ifPresent(user -> {
                            user.setNoShowCount(user.getNoShowCount() + 1);
                            if (user.getNoShowCount() >= 3) {
                                user.setEnabled(false); // 🚫 حظر المريض بعد 3 غيابات
                            }
                            userRepository.save(user);
                        });
                    }

                    // بث التحديث عبر الـ WebSocket
                    messagingTemplate.convertAndSend("/topic/appointments/" + appointment.getDoctorNationalId(), "UPDATE_APPOINTMENT");

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
        Optional<Appointment> opt = appointmentRepository.findById(id);
        if (opt.isPresent()) {
            Appointment appointment = opt.get();

            // 🔒 التحقق من الهوية
            String curRole = getCurrentRole();
            String curUser = getCurrentUser();
            
            if ("ROLE_PATIENT".equals(curRole) && !curUser.equals(appointment.getPatientNationalId())) {
                return ResponseEntity.status(403).body("لا يمكنك حذف حجز لا يخصك.");
            }
            if ("ROLE_DOCTOR".equals(curRole) && !apartmentNationalIdMatchesDoctor(appointment, curUser)) {
                return ResponseEntity.status(403).body("لا يمكنك حذف حجز لا يخصك.");
            }

            appointmentRepository.deleteById(id);
            // بث التحديث عبر الـ WebSocket
            messagingTemplate.convertAndSend("/topic/appointments/" + appointment.getDoctorNationalId(), "DELETE_APPOINTMENT");
            return ResponseEntity.ok("تم حذف الحجز بنجاح");
        }
        return ResponseEntity.notFound().build();
    }
    private boolean apartmentNationalIdMatchesDoctor(Appointment appointment, String doctorId) {
        if (appointment.getDoctorNationalId() == null) return false;
        return appointment.getDoctorNationalId().trim().equals(doctorId);
    }
}


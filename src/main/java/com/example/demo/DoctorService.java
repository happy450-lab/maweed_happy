package com.example.demo;

import com.example.demo.DTO.DoctorDTO;
import com.example.demo.domain.AssistantRequest;
import com.example.demo.repository.DoctorRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.security.SecureRandom;

import static com.example.demo.Role.ROLE_DOCTOR;
import com.example.demo.DTO.UpdateProfileDTO;
import com.example.demo.domain.WorkingHour;
import com.example.demo.repository.WorkingHourRepository;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.PrescriptionRepository;
import com.example.demo.repository.AssistantRequestRepository;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.util.Map;
import java.util.HashMap;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Service
@Data
public class DoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private WorkingHourRepository workingHourRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private AssistantRequestRepository assistantRequestRepository;

    @Autowired
    private PushNotificationService pushNotificationService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // حروف وأرقام عشوائية لتوليد الكود
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * توليد كود عشوائي من 6 حروف وأرقام (alphanumeric)
     */
    private String generateAlphanumericCode() {
        StringBuilder code = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            code.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return code.toString();
    }

    /**
     * ✅ 1. تسجيل الطبيب
     */
    public Doctor registerDoctor(DoctorDTO dto) {
        if (doctorRepository.existsByNationalId(dto.getNationalId())) {
            throw new RuntimeException("هذا الرقم القومي مسجل كطبيب بالفعل!");
        }

        Doctor doctor = new Doctor();
        doctor.setNameDoctor(dto.getNameDoctor().trim());
        doctor.setNationalId(dto.getNationalId().trim());
        doctor.setPhoneNumberDoctor(dto.getPhoneNumberDoctor().trim());
        doctor.setSpecialization(dto.getSpecialization());

        doctor.setRole(ROLE_DOCTOR.name());
        doctor.setApproved(false);
        doctor.setEnabled(false);

        return doctorRepository.save(doctor);
    }

    /**
     * ✅ 2. رفع الشهادة
     */
    public String uploadCertificate(Long id, MultipartFile file) throws IOException {
        String uploadDir = "uploads/certificates/";
        java.io.File dir = new java.io.File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        String fileName = "doctor_" + id + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir + fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("عفواً، الطبيب غير موجود!"));

        doctor.setDoctorCertificate(filePath.toString());
        doctorRepository.save(doctor);

        return "تم رفع الشهادة بنجاح وحفظ المسار للدكتور: " + doctor.getNameDoctor();
    }

    /**
     * ✅ 3. توليد كود التفعيل (alphanumeric 6 خانات)
     */
    public String generateAndSaveAccessCode(String nationalId) {
        Doctor doctor = doctorRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new RuntimeException("لم يتم العثور على طبيب بهذا الرقم القومي"));

        String finalCode = generateAlphanumericCode();
        doctor.setSpecialAccessCode(finalCode);
        doctorRepository.save(doctor);

        return finalCode;
    }

    /**
     * ✅ 4. قبول تسجيل الدكتور من الأدمن — يولد كود ويفعّل الحساب
     */
    public String approveDoctorRegistration(String nationalId, int months) {
        Doctor doctor = doctorRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new RuntimeException("لم يتم العثور على طبيب بهذا الرقم القومي"));

        String code = generateAlphanumericCode();
        doctor.setSpecialAccessCode(code);
        doctor.setApproved(true);
        doctor.setEnabled(true);
        
        // Add subscription calculation
        java.time.LocalDate newEndDate = java.time.LocalDate.now().plusMonths(months);
        doctor.setSubscriptionEndDate(newEndDate.atStartOfDay());
        
        doctorRepository.save(doctor);

        return code;
    }

    /**
     * ✅ 5. تفعيل الحساب النهائي بعد مطابقة الكود
     */
    public Doctor activateAccount(String nationalId, String code) {
        Doctor doctor = doctorRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new RuntimeException("الرقم القومي غير صحيح"));

        if (doctor.getSpecialAccessCode() == null || !doctor.getSpecialAccessCode().equals(code)) {
            throw new RuntimeException("الكود الخاص غير صحيح أو منتهي الصلاحية");
        }

        doctor.setApproved(true);
        doctor.setEnabled(true);

        return doctorRepository.save(doctor);
    }

    /**
     * ✅ 6. تحديث الملف الشخصي للطبيب
     */
    public Doctor updateProfile(String nationalId, UpdateProfileDTO dto) {
        Doctor doctor = doctorRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new RuntimeException("الطبيب غير موجود"));

        if (dto.getLocation() != null) doctor.setLocation(dto.getLocation());
        if (dto.getSpecialization() != null) doctor.setSpecialization(dto.getSpecialization());
        if (dto.getCheckupPrice() != null) doctor.setCheckupPrice(dto.getCheckupPrice());
        if (dto.getRecheckPrice() != null) doctor.setRecheckPrice(dto.getRecheckPrice());
        if (dto.getGoogleMapsLink() != null) doctor.setGoogleMapsLink(dto.getGoogleMapsLink());
        if (dto.getAboutDoctor() != null) doctor.setAboutDoctor(dto.getAboutDoctor());
        if (dto.getQualifications() != null) doctor.setQualifications(dto.getQualifications());
        if (dto.getClinicPhone() != null) doctor.setClinicPhone(dto.getClinicPhone());

        return doctorRepository.save(doctor);
    }

    /**
     * ✅ 7. تغيير كود التفعيل بواسطة الطبيب نفسه
     * يقوم أيضاً بتحديث باسورد الدخول الخاص بالمساعدين الملحقين بهذا الطبيب ليعملوا بنفس الكود
     */
    @org.springframework.transaction.annotation.Transactional
    public Doctor updateAccessCode(String nationalId, String newCode) {
        Doctor doctor = doctorRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new RuntimeException("الطبيب غير موجود"));

        doctor.setSpecialAccessCode(newCode);
        
        // 🔄 مزامنة الكود الجديد لجميع المساعدين حتى لا يفقدوا حساباتهم
        List<AssistantRequest> assistants = assistantRequestRepository.findByDoctorNationalId(nationalId);
        for (AssistantRequest assistant : assistants) {
            assistant.setDoctorCode(newCode);
        }
        assistantRequestRepository.saveAll(assistants);

        return doctorRepository.save(doctor);
    }

    /**
     * ✅ 8. حفظ مواعيد وأيام العمل
     */
    @org.springframework.transaction.annotation.Transactional
    public List<WorkingHour> saveWorkingHours(String nationalId, List<WorkingHour> hours) {
        Doctor doctor = doctorRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new RuntimeException("الطبيب غير موجود"));

        // Delete old hours and save new ones
        workingHourRepository.deleteByDoctorNationalId(nationalId);

        for (WorkingHour hour : hours) {
            hour.setDoctor(doctor);
        }

        return workingHourRepository.saveAll(hours);
    }

    /**
     * ✅ 9. رفع صورة الغلاف
     */
    public String uploadCover(String nationalId, MultipartFile file) throws IOException {
        String uploadDir = "uploads/covers/";
        java.io.File dir = new java.io.File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        String fileName = "cover_" + nationalId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir + fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        Doctor doctor = doctorRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new RuntimeException("الطبيب غير موجود"));

        doctor.setCoverPhoto("/uploads/covers/" + fileName);
        doctorRepository.save(doctor);

        return doctor.getCoverPhoto();
    }

    /**
     * ✅ 10. رفع الصورة الشخصية
     */
    public String uploadPhoto(String nationalId, MultipartFile file) throws IOException {
        String uploadDir = "uploads/photos/";
        java.io.File dir = new java.io.File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        String fileName = "photo_" + nationalId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir + fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        Doctor doctor = doctorRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new RuntimeException("الطبيب غير موجود"));

        doctor.setDoctorPhoto("/uploads/photos/" + fileName);
        doctorRepository.save(doctor);

        return doctor.getDoctorPhoto();
    }

    /**
     * ✅ 11. حذف الطبيب وجميع البيانات المرتبطة به نهائياً (ديناميكي)
     */
    @org.springframework.transaction.annotation.Transactional
    public void deleteDoctorCompletely(String nationalId) {
        Doctor doctor = doctorRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new RuntimeException("الطبيب غير موجود"));

        // Delete from all related tables
        appointmentRepository.deleteByDoctorNationalId(nationalId);
        prescriptionRepository.deleteByDoctorNationalId(nationalId);
        assistantRequestRepository.deleteByDoctorNationalId(nationalId);
        workingHourRepository.deleteByDoctorNationalId(nationalId);

        // Try to delete physical files if they exist
        try {
            if (doctor.getDoctorPhoto() != null && !doctor.getDoctorPhoto().isEmpty()) {
                Path photoPath = Paths.get(doctor.getDoctorPhoto().replaceFirst("^/", ""));
                Files.deleteIfExists(photoPath);
            }
            if (doctor.getCoverPhoto() != null && !doctor.getCoverPhoto().isEmpty()) {
                Path coverPath = Paths.get(doctor.getCoverPhoto().replaceFirst("^/", ""));
                Files.deleteIfExists(coverPath);
            }
            if (doctor.getDoctorCertificate() != null && !doctor.getDoctorCertificate().isEmpty()) {
                Path certPath = Paths.get(doctor.getDoctorCertificate().replaceFirst("^/", ""));
                Files.deleteIfExists(certPath);
            }
        } catch (Exception e) {
            System.err.println("Failed to delete doctor files: " + e.getMessage());
        }

        // Finally, delete the doctor record itself
        doctorRepository.delete(doctor);
    }

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
     * ✅ خوارزمية الترحيل المتتالي (Cascading Queue Rebuild)
     */
    @org.springframework.transaction.annotation.Transactional
    public int applySuddenHoliday(String nationalId, LocalDate offDate) {
        // 1. جلب بيانات الطبيب وساعات العمل
        Doctor doctor = doctorRepository.findByNationalId(nationalId).orElseThrow(() -> new RuntimeException("الطبيب غير موجود"));
        List<WorkingHour> workingHours = workingHourRepository.findByDoctorNationalId(nationalId);
        if (workingHours == null || workingHours.isEmpty()) {
            throw new RuntimeException("الطبيب ليس لديه ساعات عمل مسجلة لإنشاء الحجوزات.");
        }
        
        // 1.5 تسجيل اليوم كإجازة استثنائية لمنع أي حجز جديد فيه
        String offDateStr = offDate.toString();
        if (doctor.getBlockedDates() == null) {
            doctor.setBlockedDates(offDateStr);
        } else if (!doctor.getBlockedDates().contains(offDateStr)) {
            doctor.setBlockedDates(doctor.getBlockedDates() + "," + offDateStr);
        }
        doctorRepository.save(doctor);
        
        Map<String, WorkingHour> whMap = new HashMap<>();
        for (WorkingHour wh : workingHours) {
            whMap.put(wh.getDayOfWeek(), wh);
        }

        // 2. جلب كافة المواعيد PENDING من offDate وما بعده مرتبة حسب التاريخ والوقت
        List<Appointment> pendingQueue = appointmentRepository.findPendingAppointmentsFromDate(nationalId, offDate);
        if (pendingQueue.isEmpty()) {
            return 0; // لا يوجد مواعيد للترحيل
        }

        // 3. تجهيز الـ Cursor لبدء الترحيل من اليوم التالي للإجازة المفاجئة
        LocalDate cursorDate = offDate.plusDays(1);
        
        // إيجاد أول يوم عمل بعد يوم الإجازة لاستخدامه كنقطة بداية (تخطي أيام الإجازات)
        WorkingHour currentWh = whMap.get(toArabicDay(cursorDate.getDayOfWeek()));
        while (currentWh == null || currentWh.isOff() || (currentWh.getStartTime().equals(LocalTime.MIDNIGHT) && currentWh.getEndTime().equals(LocalTime.MIDNIGHT))) {
            cursorDate = cursorDate.plusDays(1);
            currentWh = whMap.get(toArabicDay(cursorDate.getDayOfWeek()));
        }

        LocalTime cursorTime = currentWh.getStartTime();
        int slotCount = 0; // مرضى في نفس الـ 20 دقيقة (Max 2)

        // 4. المرور على كافة المواعيد في المستقبل وإعادة تسكينها بالترتيب (Shift Forward)
        for (Appointment app : pendingQueue) {
            LocalDateTime originalDateTime = LocalDateTime.of(app.getAppointmentDate(), app.getAppointmentTime());
            LocalDateTime cursorDateTime = LocalDateTime.of(cursorDate, cursorTime);

            // نأخذ الأكبر دائماَ (لا يمكن إرجاع أي ميعاد للخلف)
            if (originalDateTime.isAfter(cursorDateTime)) {
                cursorDate = app.getAppointmentDate();
                cursorTime = app.getAppointmentTime();
                slotCount = 0;
            }

            // التحقق من تجاوز ساعات العمل أو الوقوع في يوم إجازة للدكتور
            boolean slotValid = false;
            int safetyLimit = 0; // عداد حماية من اللوب اللانهائي
            while (!slotValid && safetyLimit < 365) {
                safetyLimit++;
                WorkingHour wh = whMap.get(toArabicDay(cursorDate.getDayOfWeek()));
                boolean isOffDay = wh == null || wh.isOff() || 
                    (wh.getStartTime().equals(LocalTime.MIDNIGHT) && wh.getEndTime().equals(LocalTime.MIDNIGHT));

                if (isOffDay) {
                    // انتقل لليوم التالي وابدأ من أول ساعة عمل فيه
                    cursorDate = cursorDate.plusDays(1);
                    cursorTime = LocalTime.MIDNIGHT; // placeholder — سيتم تحديثه في الدورة التالية
                    slotCount = 0;
                    WorkingHour nextWh = whMap.get(toArabicDay(cursorDate.getDayOfWeek()));
                    if (nextWh != null && !nextWh.isOff() && !nextWh.getStartTime().equals(LocalTime.MIDNIGHT)) {
                        cursorTime = nextWh.getStartTime();
                    }
                } else if (cursorTime.equals(LocalTime.MIDNIGHT) || cursorTime.isBefore(wh.getStartTime())) {
                    // الـ cursorTime قبل بداية الدوام، نضبطه لبداية الدوام
                    cursorTime = wh.getStartTime();
                    slotCount = 0;
                } else if (cursorTime.isAfter(wh.getEndTime()) || cursorTime.equals(wh.getEndTime()) || cursorTime.plusMinutes(20).isAfter(wh.getEndTime())) {
                    // تجاوز ساعات العمل، ننتقل لليوم التالي
                    cursorDate = cursorDate.plusDays(1);
                    cursorTime = LocalTime.MIDNIGHT; // placeholder
                    slotCount = 0;
                    WorkingHour nextWh = whMap.get(toArabicDay(cursorDate.getDayOfWeek()));
                    if (nextWh != null && !nextWh.isOff() && !nextWh.getStartTime().equals(LocalTime.MIDNIGHT)) {
                        cursorTime = nextWh.getStartTime();
                    }
                } else {
                    slotValid = true; // وجدنا وقت صحيح داخل ساعات العمل
                }
            }

            // تحديث الميعاد
            app.setAppointmentDate(cursorDate);
            app.setAppointmentTime(cursorTime);

            // تحديث الـ Cursor
            slotCount++;
            if (slotCount >= 2) { // 2 مرضى لكل 20 دقيقة
                cursorTime = cursorTime.plusMinutes(20);
                slotCount = 0;
            }
        }

        // 5. حفظ كافة المواعيد المحدثة (منفصل ومحمي)
        try {
            appointmentRepository.saveAll(pendingQueue);
        } catch (Exception saveEx) {
            System.err.println("خطأ أثناء حفظ المواعيد: " + saveEx.getMessage());
            throw saveEx; // نعيد الإلقاء عشان الـ @Transactional يشتغل
        }

        int savedCount = pendingQueue.size();
        
        // 6. إرسال Push Notifications للمرضى المٌتأثرين (لا يؤثر فشله على الـ Transaction)
        for (Appointment app : pendingQueue) {
            try {
                pushNotificationService.sendToUser(
                    app.getPatientNationalId(),
                    "📅 تعديل ميعاد الحجز",
                    "عذراً، لظروف طارئة لطبيبك تم تأجيل موعدك ليكون يوم " + app.getAppointmentDate() + " الساعة " + app.getAppointmentTime() + ". يمكنك مراجعة العيادة في حال التعارض."
                );
            } catch (Exception e) {
                // Push Notification فشلت — نكمل بدونها
                System.err.println("خطأ أثناء إرسال الإشعار للمريض " + app.getPatientNationalId() + ": " + e.getMessage());
            }
        }
        
        // 7. إخبار الفرونت إند عبر WebSocket
        try {
            messagingTemplate.convertAndSend("/topic/appointments/" + nationalId, "UPDATE_APPOINTMENT");
        } catch (Exception wsEx) {
            System.err.println("خطأ WebSocket: " + wsEx.getMessage());
        }
        
        return savedCount;
    }
}
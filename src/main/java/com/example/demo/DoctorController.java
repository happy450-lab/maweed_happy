package com.example.demo;

import com.example.demo.DTO.DoctorDTO;
import com.example.demo.DTO.DoctorResponseDTO;
import com.example.demo.DTO.DoctorPrivateDTO;
import com.example.demo.DTO.AssistantResponseDTO;
import com.example.demo.DTO.UserDTO;
import com.example.demo.repository.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.repository.AssistantRequestRepository;
import com.example.demo.domain.AssistantRequest;
import com.example.demo.DTO.UpdateProfileDTO;
import com.example.demo.domain.WorkingHour;
import com.example.demo.repository.WorkingHourRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@CrossOrigin(origins = {"http://localhost:3000", "https://maweed-ui.vercel.app"})
@RestController
@RequestMapping("/api/doctors") // المسار الموحد لكل ما يخص الطبيب
public class DoctorController {

    @Autowired
    private DoctorRepository doctorRepository;

    
    @Autowired
    private DoctorService doctorService;

    @Autowired
    private AssistantRequestRepository assistantRequestRepository;

    @Autowired
    private WorkingHourRepository workingHourRepository;

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

    private boolean isAuthorized(String targetNationalId) {
        String role = getCurrentRole();
        String user = getCurrentUser();
        // For endpoints strictly meant for doctors modifying their own data:
        if ("ROLE_DOCTOR".equals(role) && user != null && !user.equals(targetNationalId)) {
            return false;
        }
        return true;
    }

    /**
     * ✅ 1. تسجيل الطبيب (المرحلة الأولى)
     * يستقبل البيانات ويخزنها في جدول الدكاترة حصراً
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerDoctor(@RequestBody DoctorDTO dto, jakarta.servlet.http.HttpServletRequest request) {
        try {
            // 🔒 حماية: فقط الطلبات القادمة من الموقع الرسمي
            String origin = request.getHeader("Origin");
            String referer = request.getHeader("Referer");
            boolean isValidSource = false;
            if (origin != null && (origin.equals("http://localhost:3000") || origin.equals("https://maweed-ui.vercel.app"))) {
                isValidSource = true;
            } else if (referer != null && (referer.startsWith("http://localhost:3000") || referer.startsWith("https://maweed-ui.vercel.app"))) {
                isValidSource = true;
            }
            if (!isValidSource) {
                return ResponseEntity.status(403).body("غير مصرح. يجب التسجيل من الموقع الرسمي فقط.");
            }

            Doctor newDoctor = doctorService.registerDoctor(dto);
            return ResponseEntity.ok(newDoctor);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("خطأ في تسجيل الدكتور: " + e.getMessage());
        }
    }

    /**
     * ✅ 2. ميثود رفع الشهادة (تم نقلها هنا للفصل التام)
     * تتعامل مع الملفات وتربطها بجدول الدكاترة فقط
     */
    @PostMapping("/{id}/upload-certificate")
    public ResponseEntity<?> uploadCertificate(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            // استدعاء البادي من DoctorService لضمان فصل منطق الملفات
            String message = doctorService.uploadCertificate(id, file);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            // إرجاع خطأ في حال فشل الرفع أو البحث
            return ResponseEntity.status(500).body("فشل رفع الملف: " + e.getMessage());
        }
    }

    /**
     * ✅ 3. توليد كود التفعيل (للمدير)
     */
    @PostMapping("/generate-code/{nationalId}")
    public ResponseEntity<String> generateCode(@PathVariable String nationalId) {
        try {
            String code = doctorService.generateAndSaveAccessCode(nationalId);
            return ResponseEntity.ok("تم إنشاء الكود بنجاح: " + code);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * ✅ 4. تفعيل الحساب (المرحلة النهائية)
     */
    @PostMapping("/activate")
    public ResponseEntity<?> activateAccount(@RequestBody Map<String, String> request) {
        try {
            String nationalId = request.get("nationalId");
            String code = request.get("code");
            Doctor doctor = doctorService.activateAccount(nationalId, code);
            return ResponseEntity.ok("تم تفعيل حسابك بنجاح يا دكتور " + doctor.getNameDoctor());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * ✅ 5. عرض قائمة الأطباء للمراجعة
     * 🔒 يرجع DoctorResponseDTO فقط — لا يكشف بيانات حساسة (password, activeToken, nationalId)
     */
    @GetMapping
    public List<DoctorResponseDTO> getAllDoctors() {
        return doctorRepository.findByApprovedTrueAndEnabledTrue()
                .stream()
                .map(DoctorResponseDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * ✅ 5.1 عرض أفضل 5 أطباء تقييماً للصفحة الرئيسية
     */
    @GetMapping("/top")
    public List<DoctorResponseDTO> getTopDoctors() {
        return doctorRepository.findTop5ByApprovedTrueAndEnabledTrueOrderByAverageRatingDesc()
                .stream()
                .map(DoctorResponseDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * ✅ 6. البحث عن طريق الكود الخاص
     * 🔒 يرجع DoctorResponseDTO فقط — لا يكشف بيانات حساسة
     */
    @GetMapping("/search-by-code/{code}")
    public ResponseEntity<DoctorResponseDTO> getDoctorByCode(@PathVariable String code) {
        return doctorRepository.findBySpecialAccessCode(code)
                .map(doctor -> ResponseEntity.ok().body(DoctorResponseDTO.from(doctor)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ✅ 7. جلب تفاصيل طبيب واحد بالـ ID
     * 🔒 يرجع DoctorResponseDTO فقط
     */
    @GetMapping("/{id}")
    public ResponseEntity<DoctorResponseDTO> getDoctorById(@PathVariable Long id) {
        return doctorRepository.findById(id)
                .map(doctor -> ResponseEntity.ok().body(DoctorResponseDTO.from(doctor)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ✅ 7.1 جلب تفاصيل طبيب واحد بالرقم القومي
     * 🔐 هذا الـ endpoint محمي بتوكن JWT — فقط للطبيب والمساعد
     * يرجع DoctorPrivateDTO الذي يحتوي على specialAccessCode و phoneNumberDoctor
     */
    @GetMapping("/nationalId/{nationalId}")
    public ResponseEntity<DoctorPrivateDTO> getDoctorByNationalId(@PathVariable String nationalId) {
        return doctorRepository.findByNationalId(nationalId)
                .map(doctor -> ResponseEntity.ok().body(DoctorPrivateDTO.from(doctor)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ✅ 8. إضافة مساعد (للأطباء) - تم التعديل لتكون الموافقة فورية
     */
    @PostMapping("/request-assistant")
    public ResponseEntity<?> requestAssistant(@RequestBody Map<String, String> request) {
        String doctorNationalId = request.get("doctorNationalId");
        String assistantNationalId = request.get("assistantNationalId");
        String assistantName = request.get("assistantName"); // الاستقبال من الواجهة

        if (!isAuthorized(doctorNationalId)) {
            return ResponseEntity.status(403).body("غير مصرح لك بإضافة مساعد لطبيب آخر.");
        }

        Optional<Doctor> doctorOpt = doctorRepository.findByNationalId(doctorNationalId);
        if (doctorOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("الدكتور غير موجود");
        }

        // تحقق من الحد الأقصى للمساعدين المعتمدين (3 مساعدين)
        long currentApprovedCount = assistantRequestRepository.countByDoctorNationalIdAndStatus(doctorNationalId, "APPROVED");
        if (currentApprovedCount >= 3) {
            return ResponseEntity.badRequest().body("لقد وصلت للحد الأقصى للمساعدين (3 مساعدين)");
        }

        // تحقق إن الطلب متعملش قبل كده لنفس الشخص
        Optional<AssistantRequest> existingRequest = assistantRequestRepository.findByAssistantNationalIdAndDoctorNationalId(assistantNationalId, doctorNationalId);
        if (existingRequest.isPresent()) {
            return ResponseEntity.badRequest().body("يوجد طلب سابق لهذا المساعد");
        }

        AssistantRequest assistantRequest = new AssistantRequest();
        assistantRequest.setDoctorNationalId(doctorNationalId);
        assistantRequest.setAssistantNationalId(assistantNationalId);
        assistantRequest.setDoctorCode(doctorOpt.get().getSpecialAccessCode());
        // إضافة الأسماء للطلب
        assistantRequest.setDoctorName(doctorOpt.get().getNameDoctor());
        assistantRequest.setAssistantName(assistantName != null ? assistantName : "غير محدد");

        // الموافقة تلقائية الآن بناءً على طلب المستخدم
        assistantRequest.setStatus("APPROVED");

        assistantRequestRepository.save(assistantRequest);
        return ResponseEntity.ok("تم إضافة المساعد بنجاح");
    }

    /**
     * ✅ 9. حذف مساعد
     */
    @DeleteMapping("/assistant/{id}")
    public ResponseEntity<?> deleteAssistant(@PathVariable Long id) {
        if (assistantRequestRepository.existsById(id)) {
            assistantRequestRepository.deleteById(id);
            return ResponseEntity.ok("تم حذف المساعد بنجاح");
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * ✅ 10. عرض مساعدين الدكتور (للأطباء)
     * 🔒 يرجع AssistantResponseDTO بدون activeToken
     */
    @GetMapping("/assistants/{doctorNationalId}")
    public ResponseEntity<List<AssistantResponseDTO>> getDoctorAssistants(@PathVariable String doctorNationalId) {
        List<AssistantResponseDTO> result = assistantRequestRepository.findByDoctorNationalId(doctorNationalId)
                .stream()
                .map(AssistantResponseDTO::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * ✅ 11. تحديث الملف الشخصي
     */
    @PutMapping("/{nationalId}/profile")
    public ResponseEntity<?> updateProfile(@PathVariable String nationalId, @RequestBody UpdateProfileDTO dto) {
        if (!isAuthorized(nationalId)) return ResponseEntity.status(403).body("غير مصرح لتعديل بيانات طبيب آخر");
        try {
            Doctor updatedDoctor = doctorService.updateProfile(nationalId, dto);
            return ResponseEntity.ok(updatedDoctor);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * ✅ 12. تغيير كود التفعيل
     */
    @PutMapping("/{nationalId}/access-code")
    public ResponseEntity<?> updateAccessCode(@PathVariable String nationalId, @RequestBody Map<String, String> request) {
        if (!isAuthorized(nationalId)) return ResponseEntity.status(403).body("غير مصرح");
        try {
            String newCode = request.get("code");
            if (newCode == null || newCode.isEmpty()) {
                return ResponseEntity.badRequest().body("الكود لا يمكن أن يكون فارغاً");
            }
            Doctor updatedDoctor = doctorService.updateAccessCode(nationalId, newCode);
            return ResponseEntity.ok("تم تغيير كود الدخول بنجاح");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * ✅ 13. رفع صورة الغلاف
     */
    @PostMapping("/{nationalId}/upload-cover")
    public ResponseEntity<?> uploadCover(@PathVariable String nationalId, @RequestParam("file") MultipartFile file) {
        if (!isAuthorized(nationalId)) return ResponseEntity.status(403).body("غير مصرح");
        try {
            String path = doctorService.uploadCover(nationalId, file);
            return ResponseEntity.ok(path);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("فشل رفع الصورة: " + e.getMessage());
        }
    }

    /**
     * ✅ 14. رفع الصورة الشخصية
     */
    @PostMapping("/{nationalId}/upload-photo")
    public ResponseEntity<?> uploadPhoto(@PathVariable String nationalId, @RequestParam("file") MultipartFile file) {
        if (!isAuthorized(nationalId)) return ResponseEntity.status(403).body("غير مصرح");
        try {
            String path = doctorService.uploadPhoto(nationalId, file);
            return ResponseEntity.ok(path);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("فشل رفع الصورة: " + e.getMessage());
        }
    }

    /**
     * ✅ 15. تحديث ساعات العمل
     */
    @PostMapping("/{nationalId}/working-hours")
    public ResponseEntity<?> updateWorkingHours(@PathVariable String nationalId, @RequestBody List<WorkingHour> hours) {
        if (!isAuthorized(nationalId)) return ResponseEntity.status(403).body("غير مصرح");
        try {
            List<WorkingHour> savedHours = doctorService.saveWorkingHours(nationalId, hours);
            return ResponseEntity.ok(savedHours);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * ✅ 16. جلب ساعات العمل
     */
    @GetMapping("/{nationalId}/working-hours")
    public ResponseEntity<List<WorkingHour>> getWorkingHours(@PathVariable String nationalId) {
        return ResponseEntity.ok(workingHourRepository.findByDoctorNationalId(nationalId));
    }
}
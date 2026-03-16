package com.example.demo;

import com.example.demo.DTO.DoctorDTO;
import com.example.demo.DTO.UserDTO;
import com.example.demo.repository.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.repository.AssistantRequestRepository;
import com.example.demo.domain.AssistantRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    /**
     * ✅ 1. تسجيل الطبيب (المرحلة الأولى)
     * يستقبل البيانات ويخزنها في جدول الدكاترة حصراً
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerDoctor(@RequestBody DoctorDTO dto) {
        try {
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
     */
    @GetMapping
    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    /**
     * ✅ 6. البحث عن طريق الكود الخاص
     */
    @GetMapping("/search-by-code/{code}")
    public ResponseEntity<Doctor> getDoctorByCode(@PathVariable String code) {
        return doctorRepository.findBySpecialAccessCode(code)
                .map(doctor -> ResponseEntity.ok().body(doctor))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ✅ 7. جلب تفاصيل طبيب واحد بالـ ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Doctor> getDoctorById(@PathVariable Long id) {
        return doctorRepository.findById(id)
                .map(doctor -> ResponseEntity.ok().body(doctor))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ✅ 7.1 جلب تفاصيل طبيب واحد بالرقم القومي
     */
    @GetMapping("/nationalId/{nationalId}")
    public ResponseEntity<Doctor> getDoctorByNationalId(@PathVariable String nationalId) {
        return doctorRepository.findByNationalId(nationalId)
                .map(doctor -> ResponseEntity.ok().body(doctor))
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
        Optional<AssistantRequest> existingRequest = assistantRequestRepository.findByDoctorNationalIdAndAssistantNationalId(doctorNationalId, assistantNationalId);
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
     */
    @GetMapping("/assistants/{doctorNationalId}")
    public ResponseEntity<List<AssistantRequest>> getDoctorAssistants(@PathVariable String doctorNationalId) {
        return ResponseEntity.ok(assistantRequestRepository.findByDoctorNationalId(doctorNationalId));
    }
}
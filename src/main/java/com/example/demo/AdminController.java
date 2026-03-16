package com.example.demo;

import com.example.demo.domain.AssistantRequest;
import com.example.demo.repository.AssistantRequestRepository;
import com.example.demo.repository.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = {"http://localhost:3000", "https://maweed-ui.vercel.app"})
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AssistantRequestRepository assistantRequestRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DoctorService doctorService;

    // ═══════════════════════════════════════════════
    // 🔔 NOTIFICATION BADGE — عدد الطلبات المعلقة
    // ═══════════════════════════════════════════════

    /**
     * ✅ يرجع عدد طلبات المساعدين + طلبات تسجيل الدكاترة المعلقة
     * (بيستخدمها الأدمن للـ notification badge)
     */
    @GetMapping("/pending-counts")
    public ResponseEntity<Map<String, Long>> getPendingCounts() {
        long pendingAssistants = assistantRequestRepository.countByStatus("PENDING");
        long pendingDoctors = doctorRepository.countByApprovedFalse();
        Map<String, Long> counts = new HashMap<>();
        counts.put("pendingAssistants", pendingAssistants);
        counts.put("pendingDoctors", pendingDoctors);
        counts.put("total", pendingAssistants + pendingDoctors);
        return ResponseEntity.ok(counts);
    }

    // ═══════════════════════════════════════════════
    // 🩺 DOCTOR REGISTRATION REQUESTS
    // ═══════════════════════════════════════════════

    /**
     * ✅ جلب كل الدكاترة اللي لسه مش متقبلوش
     */
    @GetMapping("/doctor-requests")
    public ResponseEntity<List<Doctor>> getPendingDoctors() {
        return ResponseEntity.ok(doctorRepository.findByApprovedFalse());
    }

    /**
     * ✅ قبول تسجيل دكتور — بيولد specialAccessCode ويفعّل الحساب
     */
    @PutMapping("/doctor-requests/{nationalId}/approve")
    public ResponseEntity<?> approveDoctorRegistration(@PathVariable String nationalId) {
        try {
            String code = doctorService.approveDoctorRegistration(nationalId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "تم قبول الدكتور بنجاح");
            response.put("specialAccessCode", code);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * ✅ رفض تسجيل دكتور — يحذفه من الداتابيز
     */
    @DeleteMapping("/doctor-requests/{nationalId}/reject")
    public ResponseEntity<?> rejectDoctorRegistration(@PathVariable String nationalId) {
        Optional<Doctor> doctorOpt = doctorRepository.findByNationalId(nationalId);
        if (doctorOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        doctorRepository.delete(doctorOpt.get());
        return ResponseEntity.ok("تم رفض وحذف طلب تسجيل الدكتور");
    }

    // ═══════════════════════════════════════════════
    // 👥 ASSISTANT REQUESTS
    // ═══════════════════════════════════════════════

    /**
     * ✅ عرض كل طلبات إضافة المساعدين (للمدير)
     */
    @GetMapping("/assistant-requests")
    public ResponseEntity<List<AssistantRequest>> getAllRequests() {
        return ResponseEntity.ok(assistantRequestRepository.findAll());
    }

    /**
     * ✅ الموافقة أو الرفض على مساعد
     */
    @PutMapping("/assistant-requests/{id}/{status}")
    public ResponseEntity<?> updateAssistantRequestStatus(@PathVariable Long id, @PathVariable String status) {
        String newStatus = status.toUpperCase();
        if (!newStatus.equals("APPROVED") && !newStatus.equals("REJECTED")) {
            return ResponseEntity.badRequest().body("حالة غير صحيحة");
        }

        Optional<AssistantRequest> requestOpt = assistantRequestRepository.findById(id);
        if (requestOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AssistantRequest request = requestOpt.get();

        if (newStatus.equals("APPROVED")) {
            long currentApprovedCount = assistantRequestRepository.countByDoctorNationalIdAndStatus(request.getDoctorNationalId(), "APPROVED");
            if (!request.getStatus().equals("APPROVED") && currentApprovedCount >= 3) {
                return ResponseEntity.badRequest().body("الدكتور لديه 3 مساعدين معتمدين بالفعل. لا يمكنك الموافقة على المزيد.");
            }
        }

        request.setStatus(newStatus);
        assistantRequestRepository.save(request);
        return ResponseEntity.ok("تم " + (newStatus.equals("APPROVED") ? "الموافقة على" : "رفض") + " الطلب بنجاح");
    }

    /**
     * ✅ حذف مساعد
     */
    @DeleteMapping("/assistant-requests/{id}")
    public ResponseEntity<?> deleteAssistantRequest(@PathVariable Long id) {
        if (!assistantRequestRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        assistantRequestRepository.deleteById(id);
        return ResponseEntity.ok("تم حذف الطلب بنجاح");
    }
}

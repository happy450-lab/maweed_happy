package com.example.demo;

import com.example.demo.repository.AssistantRequestRepository;
import com.example.demo.repository.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
@CrossOrigin(origins = {"http://localhost:3000", "https://maweed-ui.vercel.app"})
public class PrescriptionController {

    private String getCurrentUser() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && !"anonymousUser".equals(auth.getPrincipal())) ? auth.getName() : null;
    }

    private String getCurrentRole() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !auth.getAuthorities().isEmpty() && !"anonymousUser".equals(auth.getPrincipal()))
            return auth.getAuthorities().iterator().next().getAuthority();
        return null;
    }

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private AssistantRequestRepository assistantRepository;

    @Autowired
    private PushNotificationService pushNotificationService;

    @PostMapping
    public ResponseEntity<?> createPrescription(@RequestBody Prescription prescription) {
        // 🔐 فقط طبيب أو مساعد ممكن يكتب روشتة
        String role = getCurrentRole();
        if (!"ROLE_DOCTOR".equals(role) && !"ROLE_ACCOUNTANT".equals(role)) {
            return ResponseEntity.status(403).body("غير مصرح لك. فقط الأطباء مسموح لهم بكتابة الروشتات.");
        }
        try {
            Prescription saved = prescriptionRepository.save(prescription);
            if (saved.getPatientNationalId() != null) {
                try {
                    pushNotificationService.sendToUser(
                        saved.getPatientNationalId(),
                        "💊 تم إضافة روشتة جديدة",
                        "قام الطبيب " + saved.getDoctorName() + " بكتابة روشتة طبية جديدة لك. يرجى مراجعة حجوزاتك."
                    );
                } catch (Exception e) {
                    System.err.println("فشل إرسال إشعار الروشتة للمريض: " + e.getMessage());
                }
            }
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            System.err.println("Error saving prescription: " + e.getMessage());
            return ResponseEntity.status(500).body("فشل الحفظ في قاعدة البيانات: " + e.getMessage());
        }
    }

    // 🔐 المريض يشوف روشتاته بس، والطبيب/المساعد يشوف لمرضاه (IDOR protection)
    @GetMapping("/patient/{nid}")
    public ResponseEntity<List<Prescription>> getPatientPrescriptions(@PathVariable String nid) {
        String caller = getCurrentUser();
        String role   = getCurrentRole();
        boolean isOwner      = nid.equals(caller);
        boolean isPrivileged = "ROLE_DOCTOR".equals(role) || "ROLE_ACCOUNTANT".equals(role);
        if (!isOwner && !isPrivileged) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(prescriptionRepository.findByPatientNationalId(nid));
    }

    // 🔐 فقط الطبيب نفسه يشوف روشتاته (IDOR protection)
    @GetMapping("/doctor/{nid}")
    public ResponseEntity<List<Prescription>> getDoctorPrescriptions(@PathVariable String nid) {
        String caller = getCurrentUser();
        String role   = getCurrentRole();
        if ("ROLE_DOCTOR".equals(role) && !nid.equals(caller)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(prescriptionRepository.findByDoctorNationalId(nid));
    }

    // 🔐 فقط طبيب/مساعد ممكن يحذف روشتة — والمساعد بس يحذف روشتات طبيبه
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrescription(@PathVariable Long id) {
        String role   = getCurrentRole();
        String caller = getCurrentUser();

        if (!"ROLE_DOCTOR".equals(role) && !"ROLE_ACCOUNTANT".equals(role)) {
            return ResponseEntity.status(403).build();
        }

        // 🔐 IDOR: تحقق أن الروشتة تخص طبيبه
        var prescriptionOpt = prescriptionRepository.findById(id);
        if (prescriptionOpt.isEmpty()) return ResponseEntity.notFound().build();

        String prescriptionDoctorId = prescriptionOpt.get().getDoctorNationalId();

        if ("ROLE_DOCTOR".equals(role) && !caller.equals(prescriptionDoctorId)) {
            return ResponseEntity.status(403).build(); // طبيب يحذف روشتة طبيب آخر
        }
        if ("ROLE_ACCOUNTANT".equals(role)) {
            // 🔐 المساعد يحذف فقط روشتات طبيبه المرتبط به
            var assistants = assistantRepository.findByAssistantNationalId(caller);
            boolean canDelete = assistants.stream()
                    .anyMatch(a -> "APPROVED".equals(a.getStatus())
                            && a.getDoctorNationalId().equals(prescriptionDoctorId));
            if (!canDelete) return ResponseEntity.status(403).build();
        }

        prescriptionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

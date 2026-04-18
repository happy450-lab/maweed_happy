package com.example.demo;

import com.example.demo.repository.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
@CrossOrigin(origins = {"http://localhost:3000", "https://maweed-ui.vercel.app"})
public class PrescriptionController {

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PushNotificationService pushNotificationService;

    @PostMapping
    public ResponseEntity<?> createPrescription(@RequestBody Prescription prescription) {
        try {
            Prescription saved = prescriptionRepository.save(prescription);

            // ✅ إرسال إشعار Web Push (من بره) للمريض
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

    @GetMapping("/patient/{nid}")
    public ResponseEntity<List<Prescription>> getPatientPrescriptions(@PathVariable String nid) {
        return ResponseEntity.ok(prescriptionRepository.findByPatientNationalId(nid));
    }

    @GetMapping("/doctor/{nid}")
    public ResponseEntity<List<Prescription>> getDoctorPrescriptions(@PathVariable String nid) {
        return ResponseEntity.ok(prescriptionRepository.findByDoctorNationalId(nid));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrescription(@PathVariable Long id) {
        prescriptionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

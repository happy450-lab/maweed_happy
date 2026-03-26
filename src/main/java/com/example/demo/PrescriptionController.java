package com.example.demo;

import com.example.demo.Prescription;
import com.example.demo.repository.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
@CrossOrigin(origins = {"http://localhost:3000", "https://maweed-ui.vercel.app", "https://maweed-ui2-production.up.railway.app"})
public class PrescriptionController {

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @PostMapping
    public ResponseEntity<?> createPrescription(@RequestBody Prescription prescription) {
        try {
            Prescription saved = prescriptionRepository.save(prescription);
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

package com.example.demo;

import com.example.demo.domain.PatientProcedure;
import com.example.demo.repository.PatientProcedureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/patient-procedures")
public class PatientProcedureController {

    @Autowired
    private PatientProcedureRepository patientProcedureRepository;

    @Autowired
    private com.example.demo.repository.AssistantRequestRepository assistantRequestRepository;
    
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
    
    private boolean isAuthorizedForDoctor(String doctorNationalId) {
        String role = getCurrentRole();
        String user = getCurrentUser();
        if (role == null || user == null) return false;
        if ("ROLE_DOCTOR".equals(role)) return user.equals(doctorNationalId);
        if ("ROLE_ACCOUNTANT".equals(role)) {
            return assistantRequestRepository.findByAssistantNationalId(user).stream()
                    .anyMatch(a -> a.getDoctorNationalId().equals(doctorNationalId) && "APPROVED".equals(a.getStatus()));
        }
        return false;
    }

    @PostMapping
    public ResponseEntity<?> createProcedure(@RequestBody PatientProcedure procedure) {
        if (!isAuthorizedForDoctor(procedure.getDoctorNationalId())) {
            return ResponseEntity.status(403).body("غير مصرح لك بإضافة إجراء لهذا الطبيب.");
        }
        if (procedure.getTotalPrice() == null) {
            procedure.setTotalPrice(0.0);
        }
        if (procedure.getPaidAmount() == null) {
            procedure.setPaidAmount(0.0);
        }
        procedure.setRemainingAmount(procedure.getTotalPrice() - procedure.getPaidAmount());
        procedure.setCreatedAt(LocalDateTime.now());
        procedure.setUpdatedAt(LocalDateTime.now());
        if (procedure.getRemainingAmount() <= 0) {
            procedure.setStatus("COMPLETED");
        } else {
            procedure.setStatus("ONGOING");
        }
        
        PatientProcedure saved = patientProcedureRepository.save(procedure);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/doctor/{doctorNationalId}")
    public ResponseEntity<List<PatientProcedure>> getDoctorProcedures(@PathVariable String doctorNationalId) {
        if (!isAuthorizedForDoctor(doctorNationalId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(patientProcedureRepository.findByDoctorNationalId(doctorNationalId));
    }

    @GetMapping("/patient/{patientNationalId}")
    public ResponseEntity<List<PatientProcedure>> getPatientProcedures(@PathVariable String patientNationalId) {
        String role = getCurrentRole();
        String user = getCurrentUser();
        // Allow patient to view their own procedures
        if ("ROLE_PATIENT".equals(role) && !patientNationalId.equals(user)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(patientProcedureRepository.findByPatientNationalId(patientNationalId));
    }

    @PutMapping("/{id}/pay")
    public ResponseEntity<?> addPayment(@PathVariable Long id, @RequestBody Map<String, Double> payload) {
        Optional<PatientProcedure> optional = patientProcedureRepository.findById(id);
        if (optional.isPresent()) {
            PatientProcedure procedure = optional.get();
            if (!isAuthorizedForDoctor(procedure.getDoctorNationalId())) {
                return ResponseEntity.status(403).body("غير مصرح لك بتعديل مدفوعات هذا الطبيب.");
            }
            Double payment = payload.get("payment");
            if (payment == null || payment <= 0) {
                return ResponseEntity.badRequest().body("Invalid payment amount");
            }
            procedure.setPaidAmount(procedure.getPaidAmount() + payment);
            procedure.setRemainingAmount(procedure.getTotalPrice() - procedure.getPaidAmount());
            if (procedure.getRemainingAmount() <= 0) {
                procedure.setStatus("COMPLETED");
            }
            procedure.setUpdatedAt(LocalDateTime.now());
            return ResponseEntity.ok(patientProcedureRepository.save(procedure));
        }
        return ResponseEntity.notFound().build();
    }
}

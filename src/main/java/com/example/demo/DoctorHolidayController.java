package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@CrossOrigin(origins = {"http://localhost:3000", "https://maweed-ui.vercel.app"})
@RestController
@RequestMapping("/api/doctors/{nationalId}/holiday")
public class DoctorHolidayController {

    @Autowired
    private DoctorService doctorService;

    @PostMapping("/sudden")
    public ResponseEntity<?> applySuddenHoliday(@PathVariable String nationalId, @RequestBody Map<String, String> payload) {
        try {
            String dateStr = payload.get("offDate");
            if (dateStr == null || dateStr.isEmpty()) {
                return ResponseEntity.badRequest().body("يجب إرسال تاريخ الإجازة (offDate).");
            }
            LocalDate offDate = LocalDate.parse(dateStr);
            int count = doctorService.applySuddenHoliday(nationalId, offDate);
            return ResponseEntity.ok(count + "");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("تعذر تنفيذ الخوارزمية: " + e.getMessage());
        }
    }
}

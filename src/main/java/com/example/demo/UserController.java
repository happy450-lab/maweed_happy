package com.example.demo;

import com.example.demo.DTO.LoginResponse;
import com.example.demo.DTO.UserDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = {"http://localhost:3000", "https://maweed-ui.vercel.app"})
@RestController
@RequestMapping("/api/auth")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * ✅ تسجيل مريض جديد
     * تم تغيير المناداة لـ registerPatient لتطابق السيرفس الجديدة
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserDTO userDTO) {
        try {
            // بننادي الميثود اللي بتسيف في جدول المرضى فقط
            User savedAccount = userService.registerPatient(userDTO);
            return ResponseEntity.ok(savedAccount);
        } catch (Exception e) {
            // معالجة الخطأ في حالة الرقم القومي المسجل مسبقاً
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * ✅ ميثود اللوجن الموحدة (مرضى ودكاترة)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserDTO loginDto) {
        LoginResponse response = userService.login(loginDto.getNationalId(), loginDto.getPassword()); 
    
        if (response != null) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(401).body("الرقم القومي أو كلمة المرور غير صحيحة");
    }
}
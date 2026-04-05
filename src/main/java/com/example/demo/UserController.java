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
        try {
            LoginResponse response = userService.login(loginDto.getNationalId(), loginDto.getPassword()); 
        
            if (response != null) {
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(401).body("الرقم القومي أو كلمة المرور غير صحيحة");
        } catch (RuntimeException e) {
            // معالجة خطأ الدخول المتعدد (أو أي خطأ صريح من UserService)
            return ResponseEntity.status(409).body(e.getMessage()); // 409 Conflict
        }
    }

    @Autowired
    private com.example.demo.repository.UserRepository userRepository;
    
    @Autowired
    private com.example.demo.repository.DoctorRepository doctorRepository;
    
    @Autowired
    private com.example.demo.repository.AssistantRequestRepository assistantRepository;

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.ok("Already logged out.");
        }

        String nationalId = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        if (role.equals("ROLE_PATIENT")) {
            var user = userRepository.findByNationalId(nationalId);
            if (user.isPresent()) {
                user.get().setActiveToken(null);
                userRepository.save(user.get());
            }
        } else if (role.equals("ROLE_DOCTOR")) {
            var doctor = doctorRepository.findByNationalId(nationalId);
            if (doctor.isPresent()) {
                doctor.get().setActiveToken(null);
                doctorRepository.save(doctor.get());
            }
        } else if (role.equals("ROLE_ACCOUNTANT")) {
            var assistants = assistantRepository.findByAssistantNationalId(nationalId);
            for (var a : assistants) {
                if ("APPROVED".equals(a.getStatus())) {
                    a.setActiveToken(null);
                    assistantRepository.save(a);
                }
            }
        }

        return ResponseEntity.ok("تم تسجيل الخروج بنجاح.");
    }
}
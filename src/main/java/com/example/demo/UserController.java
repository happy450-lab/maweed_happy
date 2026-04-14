package com.example.demo;

import com.example.demo.DTO.LoginResponse;
import com.example.demo.DTO.UserDTO;
import com.example.demo.DTO.UserResponseDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = {"http://localhost:3000", "https://maweed-ui.vercel.app"})
@RestController
@RequestMapping("/api/auth")
public class UserController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    /**
     * ✅ تسجيل مريض جديد
     * 🔒 محمي: يرفض أي طلب من خارج الموقع الرسمي (Postman, curl, مواقع أخرى)
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(
            @Valid @RequestBody UserDTO userDTO,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            // 🔒 فحص المصدر — فقط الموقع الرسمي مسموح له بالتسجيل
            String origin = request.getHeader("Origin");
            String referer = request.getHeader("Referer");
            boolean isValidSource = false;

            if (origin != null && (
                    origin.equals("http://localhost:3000") ||
                    origin.equals("https://maweed-ui.vercel.app"))) {
                isValidSource = true;
            } else if (referer != null && (
                    referer.startsWith("http://localhost:3000") ||
                    referer.startsWith("https://maweed-ui.vercel.app"))) {
                isValidSource = true;
            }

            if (!isValidSource) {
                System.out.println("🚨 محاولة تسجيل من مصدر غير مصرح به! Origin: " + origin + ", Referer: " + referer);
                return ResponseEntity.status(403).body("غير مصرح. يجب التسجيل من الموقع الرسمي فقط.");
            }

            // بننادي الميثود اللي بتسيف في جدول المرضى فقط
            User savedAccount = userService.registerPatient(userDTO);
            // 🔒 نرجع DTO آمن بدون password أو activeToken
            return ResponseEntity.ok(UserResponseDTO.from(savedAccount));
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
                // 1. Generate JWT Token
                String token = jwtUtil.generateToken(response.getNationalId(), response.getRole());
                response.setToken(token);

                // 2. Save active token to database to prevent concurrent logins globally
                if ("ROLE_PATIENT".equals(response.getRole())) {
                    userRepository.findByNationalId(response.getNationalId()).ifPresent(u -> {
                        u.setActiveToken(token);
                        userRepository.save(u);
                    });
                } else if ("ROLE_DOCTOR".equals(response.getRole())) {
                    doctorRepository.findByNationalId(response.getNationalId()).ifPresent(d -> {
                        d.setActiveToken(token);
                        doctorRepository.save(d);
                    });
                } else if ("ROLE_ACCOUNTANT".equals(response.getRole())) {
                    assistantRepository.findByAssistantNationalId(response.getNationalId()).forEach(a -> {
                        if ("APPROVED".equals(a.getStatus())) {
                            a.setActiveToken(token);
                            assistantRepository.save(a);
                        }
                    });
                }

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

    /**
     * ✅ جلب الملف الطبي للمريض عن طريق الرقم القومي
     */
    @GetMapping("/profile/{nationalId}")
    public ResponseEntity<?> getUserProfile(@PathVariable String nationalId) {
        java.util.Optional<User> userOptional = userRepository.findByNationalId(nationalId);
        if (userOptional.isPresent()) {
            return ResponseEntity.ok(UserResponseDTO.from(userOptional.get()));
        }
        return ResponseEntity.status(404).body("المريض غير موجود");
    }

    /**
     * ✅ تحديث الملف الطبي للمريض
     */
    @PutMapping("/profile/{nationalId}")
    public ResponseEntity<?> updateUserProfile(@PathVariable String nationalId, @RequestBody com.example.demo.DTO.UserProfileDTO profileDTO) {
        java.util.Optional<User> userOptional = userRepository.findByNationalId(nationalId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setBloodType(profileDTO.getBloodType());
            user.setWeight(profileDTO.getWeight());
            user.setHeight(profileDTO.getHeight());
            user.setAge(profileDTO.getAge());
            user.setChronicDiseases(profileDTO.getChronicDiseases());
            user.setAllergies(profileDTO.getAllergies());
            
            userRepository.save(user);
            return ResponseEntity.ok(UserResponseDTO.from(user));
        }
        return ResponseEntity.status(404).body("المريض غير موجود");
    }
}
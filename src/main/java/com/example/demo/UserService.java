package com.example.demo;

import com.example.demo.DTO.LoginResponse;
import com.example.demo.DTO.UserDTO;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.AssistantRequestRepository;
import com.example.demo.domain.AssistantRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AssistantRequestRepository assistantRequestRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public User registerPatient(UserDTO dto) {
        validateBasicData(dto.getFullName(), dto.getNationalId(), dto.getPhoneNumber());

        if (userRepository.findByNationalId(dto.getNationalId()).isPresent()) {
            throw new RuntimeException("هذا الرقم القومي مسجل مسبقاً كمريض.");
        }

        User user = new User();
        user.setFullName(dto.getFullName().trim());
        user.setNationalId(dto.getNationalId().trim());
        user.setPhoneNumber(dto.getPhoneNumber().trim());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.ROLE_PATIENT);

        return userRepository.save(user);
    }

    public LoginResponse login(String nationalId, String password) {
        System.out.println("=== LOGIN ATTEMPT ===");
        System.out.println("National ID: [" + nationalId + "]");

        // 1. دكتور
        Optional<Doctor> doctor = doctorRepository.findByNationalId(nationalId);
        if (doctor.isPresent()) {
            Doctor d = doctor.get();
            if (password != null && password.equals(d.getSpecialAccessCode())) {
                // 🚨 منع التلاعب: حتى لو حصل على الكود، يجب أن يكون معتمد رسمياً
                if (!d.isApproved() || !d.isEnabled()) {
                    System.out.println("🚨 Login Block: Unauthorized DOCTOR -> " + d.getNameDoctor());
                    return null;
                }
                
                System.out.println("Login success: DOCTOR -> " + d.getNameDoctor());
                
                checkConcurrentLogin(d.getActiveToken());

                String token = jwtUtil.generateToken(d.getNationalId(), "ROLE_DOCTOR");
                d.setActiveToken(token);
                doctorRepository.save(d);

                return new LoginResponse(
                        d.getNationalId(),
                        d.getNameDoctor(),
                        "ROLE_DOCTOR",
                        null,
                        d.getSpecialization(),
                        d.getDoctorPhoto(),
                        token
                );
            } else {
                System.out.println("Login failed: DOCTOR found but wrong specialAccessCode.");
                return null;
            }
        }

        // 2. مساعد
        Optional<AssistantRequest> assistantRequest = assistantRequestRepository
                .findByAssistantNationalIdAndDoctorCodeAndStatus(nationalId, password, "APPROVED");

        if (assistantRequest.isPresent()) {
            AssistantRequest ar = assistantRequest.get();
            System.out.println("Login success: ASSISTANT");

            checkConcurrentLogin(ar.getActiveToken());

            String name = ar.getAssistantName();
            Optional<User> userRecord = userRepository.findByNationalId(nationalId);
            if (userRecord.isPresent()) {
                name = userRecord.get().getFullName();
            }

            String docPhoto = null;
            Optional<Doctor> linkedDoc = doctorRepository.findByNationalId(ar.getDoctorNationalId());
            if (linkedDoc.isPresent()) {
                docPhoto = linkedDoc.get().getDoctorPhoto();
            }

            String token = jwtUtil.generateToken(nationalId, "ROLE_ACCOUNTANT");
            ar.setActiveToken(token);
            assistantRequestRepository.save(ar);

            return new LoginResponse(nationalId, name, "ROLE_ACCOUNTANT", ar.getDoctorNationalId(), null, docPhoto, token);
        }

        // 3. مريض
        Optional<User> patient = userRepository.findByNationalId(nationalId);
        if (patient.isPresent()) {
            User u = patient.get();
            boolean isPasswordMatch = false;

            if (password != null && u.getPassword() != null) {
                if (u.getPassword().startsWith("$2a$") || u.getPassword().startsWith("$2b$")) {
                    // Password is hashed with BCrypt
                    isPasswordMatch = passwordEncoder.matches(password, u.getPassword());
                } else {
                    // Legacy plain text check
                    if (password.equals(u.getPassword())) {
                        isPasswordMatch = true;
                        // 🔒 Auto-upgrade legacy password to BCrypt
                        u.setPassword(passwordEncoder.encode(password));
                        userRepository.save(u);
                        System.out.println("🔒 Upgraded legacy plaintext password to BCrypt for patient: " + nationalId);
                    }
                }
            }

            if (isPasswordMatch) {
                System.out.println("Login success: PATIENT -> " + u.getFullName());
                
                checkConcurrentLogin(u.getActiveToken());

                String roleName = (u.getRole() != null) ? u.getRole().name() : "ROLE_PATIENT";
                String token = jwtUtil.generateToken(u.getNationalId(), roleName);
                
                u.setActiveToken(token);
                userRepository.save(u);

                return new LoginResponse(u.getNationalId(), u.getFullName(), roleName, null, null, null, token);
            } else {
                System.out.println("Login failed: PATIENT wrong password.");
                return null;
            }
        }

        System.out.println("Login failed: National ID not found.");
        return null;
    }

    private void checkConcurrentLogin(String storedActiveToken) {
        if (storedActiveToken != null) {
            try {
                if (jwtUtil.isTokenValid(storedActiveToken)) {
                    throw new RuntimeException("تم تسجيل الدخول بهذا الحساب من جهاز آخر. قم بتسجيل الخروج من الجهاز الآخر أولاً.");
                }
            } catch (Exception e) {
                // الكود انتهي أو التوكن القديم باظ، نقدر نعمل تسجيل دخول عادي
            }
        }
    }

    private void validateBasicData(String fullName, String nationalId, String phoneNumber) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new RuntimeException("الاسم لا يمكن أن يكون فارغاً!");
        }
        if (nationalId == null || nationalId.length() != 14) {
            throw new RuntimeException("الرقم القومي غير صحيح (يجب أن يكون 14 رقم)!");
        }
    }
}
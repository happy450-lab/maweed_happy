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

    public User registerPatient(UserDTO dto) {
        validateBasicData(dto.getFullName(), dto.getNationalId(), dto.getPhoneNumber());

        // تصحيح النقطة المفقودة هنا
        if (userRepository.findByNationalId(dto.getNationalId()).isPresent()) {
            throw new RuntimeException("هذا الرقم القومي مسجل مسبقاً كمريض.");
        }

        User user = new User();
        user.setFullName(dto.getFullName().trim());
        user.setNationalId(dto.getNationalId().trim());
        // تأكد أن اسم الحقل في الـ User هو phoneNumber
        user.setPhoneNumber(dto.getPhoneNumber().trim());
        user.setPassword(dto.getPassword());
        user.setRole(Role.ROLE_PATIENT);

        return userRepository.save(user);
    }

    public LoginResponse login(String nationalId, String password) {
        System.out.println("=== LOGIN ATTEMPT ===");
        System.out.println("National ID: [" + nationalId + "]");
        System.out.println("Password/Code: [" + password + "]");

        // 1. لو دكتور (الرقم القومي موجود في جدول الدكاترة)
        Optional<Doctor> doctor = doctorRepository.findByNationalId(nationalId);
        if (doctor.isPresent()) {
            Doctor d = doctor.get();
            // الدكتور يسجل فقط بالكود الخاص بيه (specialAccessCode)
            if (password != null && password.equals(d.getSpecialAccessCode())) {
                System.out.println("Login success: DOCTOR -> " + d.getNameDoctor());
                return new LoginResponse(
                        d.getNationalId(),
                        d.getNameDoctor(),
                        "ROLE_DOCTOR",
                        null,
                        d.getSpecialization(),
                        d.getDoctorPhoto()
                );
            } else {
                // الرقم القومي ده بتاع دكتور لكن الكود غلط → رفض فوري بدون محاولة أدوار أخرى
                System.out.println("Login failed: DOCTOR found but wrong specialAccessCode.");
                return null;
            }
        }

        // 2. لو مساعد (الرقم القومي موجود في طلبات المساعدين المعتمدة + كود الدكتور صح)
        Optional<AssistantRequest> assistantRequest = assistantRequestRepository
                .findByAssistantNationalIdAndDoctorCodeAndStatus(nationalId, password, "APPROVED");

        if (assistantRequest.isPresent()) {
            AssistantRequest ar = assistantRequest.get();
            System.out.println("Login success: ASSISTANT matched with APPROVED status.");

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

            return new LoginResponse(nationalId, name, "ROLE_ACCOUNTANT", ar.getDoctorNationalId(), null, docPhoto);
        }

        // 3. لو مريض (الرقم القومي موجود في جدول المرضى + كلمة السر صح)
        Optional<User> patient = userRepository.findByNationalId(nationalId);
        if (patient.isPresent()) {
            User u = patient.get();
            // تحقق من كلمة السر بدقة
            if (password != null && password.equals(u.getPassword())) {
                System.out.println("Login success: PATIENT -> " + u.getFullName());
                String roleName = (u.getRole() != null) ? u.getRole().name() : "ROLE_PATIENT";
                return new LoginResponse(u.getNationalId(), u.getFullName(), roleName, null, null, null);
            } else {
                System.out.println("Login failed: PATIENT found but wrong password.");
                return null;
            }
        }

        // 4. الرقم القومي غير موجود نهائياً في أي جدول
        System.out.println("Login failed: National ID not found in any table.");
        return null;
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
package com.example.demo;

import com.example.demo.DTO.DoctorDTO;
import com.example.demo.repository.DoctorRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.security.SecureRandom;

import static com.example.demo.Role.ROLE_DOCTOR;

@Service
@Data
public class DoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    // حروف وأرقام عشوائية لتوليد الكود
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * توليد كود عشوائي من 6 حروف وأرقام (alphanumeric)
     */
    private String generateAlphanumericCode() {
        StringBuilder code = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            code.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return code.toString();
    }

    /**
     * ✅ 1. تسجيل الطبيب
     */
    public Doctor registerDoctor(DoctorDTO dto) {
        if (doctorRepository.existsByNationalId(dto.getNationalId())) {
            throw new RuntimeException("هذا الرقم القومي مسجل كطبيب بالفعل!");
        }

        Doctor doctor = new Doctor();
        doctor.setNameDoctor(dto.getNameDoctor().trim());
        doctor.setNationalId(dto.getNationalId().trim());
        doctor.setPhoneNumberDoctor(dto.getPhoneNumberDoctor().trim());
        doctor.setPassword(dto.getPassword());
        doctor.setSpecialization(dto.getSpecialization());
        doctor.setDoctorPhoto(dto.getDoctorPhoto());

        doctor.setRole(ROLE_DOCTOR.name());
        doctor.setApproved(false);
        doctor.setEnabled(false);

        return doctorRepository.save(doctor);
    }

    /**
     * ✅ 2. رفع الشهادة
     */
    public String uploadCertificate(Long id, MultipartFile file) throws IOException {
        String uploadDir = "uploads/certificates/";
        java.io.File dir = new java.io.File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        String fileName = "doctor_" + id + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir + fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("عفواً، الطبيب غير موجود!"));

        doctor.setDoctorCertificate(filePath.toString());
        doctorRepository.save(doctor);

        return "تم رفع الشهادة بنجاح وحفظ المسار للدكتور: " + doctor.getNameDoctor();
    }

    /**
     * ✅ 3. توليد كود التفعيل (alphanumeric 6 خانات)
     */
    public String generateAndSaveAccessCode(String nationalId) {
        Doctor doctor = doctorRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new RuntimeException("لم يتم العثور على طبيب بهذا الرقم القومي"));

        String finalCode = generateAlphanumericCode();
        doctor.setSpecialAccessCode(finalCode);
        doctorRepository.save(doctor);

        return finalCode;
    }

    /**
     * ✅ 4. قبول تسجيل الدكتور من الأدمن — يولد كود ويفعّل الحساب
     */
    public String approveDoctorRegistration(String nationalId) {
        Doctor doctor = doctorRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new RuntimeException("لم يتم العثور على طبيب بهذا الرقم القومي"));

        String code = generateAlphanumericCode();
        doctor.setSpecialAccessCode(code);
        doctor.setApproved(true);
        doctor.setEnabled(true);
        doctorRepository.save(doctor);

        return code;
    }

    /**
     * ✅ 5. تفعيل الحساب النهائي بعد مطابقة الكود
     */
    public Doctor activateAccount(String nationalId, String code) {
        Doctor doctor = doctorRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new RuntimeException("الرقم القومي غير صحيح"));

        if (doctor.getSpecialAccessCode() == null || !doctor.getSpecialAccessCode().equals(code)) {
            throw new RuntimeException("الكود الخاص غير صحيح أو منتهي الصلاحية");
        }

        doctor.setApproved(true);
        doctor.setEnabled(true);

        return doctorRepository.save(doctor);
    }
}
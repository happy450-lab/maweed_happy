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
import com.example.demo.DTO.UpdateProfileDTO;
import com.example.demo.domain.WorkingHour;
import com.example.demo.repository.WorkingHourRepository;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.PrescriptionRepository;
import com.example.demo.repository.AssistantRequestRepository;

import java.util.List;

@Service
@Data
public class DoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private WorkingHourRepository workingHourRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private AssistantRequestRepository assistantRequestRepository;

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
        doctor.setSpecialization(dto.getSpecialization());

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

    /**
     * ✅ 6. تحديث الملف الشخصي للطبيب
     */
    public Doctor updateProfile(String nationalId, UpdateProfileDTO dto) {
        Doctor doctor = doctorRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new RuntimeException("الطبيب غير موجود"));

        if (dto.getLocation() != null) doctor.setLocation(dto.getLocation());
        if (dto.getSpecialization() != null) doctor.setSpecialization(dto.getSpecialization());
        if (dto.getCheckupPrice() != null) doctor.setCheckupPrice(dto.getCheckupPrice());
        if (dto.getRecheckPrice() != null) doctor.setRecheckPrice(dto.getRecheckPrice());
        if (dto.getGoogleMapsLink() != null) doctor.setGoogleMapsLink(dto.getGoogleMapsLink());
        if (dto.getAboutDoctor() != null) doctor.setAboutDoctor(dto.getAboutDoctor());
        if (dto.getQualifications() != null) doctor.setQualifications(dto.getQualifications());

        return doctorRepository.save(doctor);
    }

    /**
     * ✅ 7. تغيير كود التفعيل بواسطة الطبيب نفسه
     */
    public Doctor updateAccessCode(String nationalId, String newCode) {
        Doctor doctor = doctorRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new RuntimeException("الطبيب غير موجود"));

        doctor.setSpecialAccessCode(newCode);
        return doctorRepository.save(doctor);
    }

    /**
     * ✅ 8. حفظ مواعيد وأيام العمل
     */
    @org.springframework.transaction.annotation.Transactional
    public List<WorkingHour> saveWorkingHours(String nationalId, List<WorkingHour> hours) {
        Doctor doctor = doctorRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new RuntimeException("الطبيب غير موجود"));

        // Delete old hours and save new ones
        workingHourRepository.deleteByDoctorNationalId(nationalId);

        for (WorkingHour hour : hours) {
            hour.setDoctor(doctor);
        }

        return workingHourRepository.saveAll(hours);
    }

    /**
     * ✅ 9. رفع صورة الغلاف
     */
    public String uploadCover(String nationalId, MultipartFile file) throws IOException {
        String uploadDir = "uploads/covers/";
        java.io.File dir = new java.io.File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        String fileName = "cover_" + nationalId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir + fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        Doctor doctor = doctorRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new RuntimeException("الطبيب غير موجود"));

        doctor.setCoverPhoto("/uploads/covers/" + fileName);
        doctorRepository.save(doctor);

        return doctor.getCoverPhoto();
    }

    /**
     * ✅ 10. رفع الصورة الشخصية
     */
    public String uploadPhoto(String nationalId, MultipartFile file) throws IOException {
        String uploadDir = "uploads/photos/";
        java.io.File dir = new java.io.File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        String fileName = "photo_" + nationalId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir + fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        Doctor doctor = doctorRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new RuntimeException("الطبيب غير موجود"));

        doctor.setDoctorPhoto("/uploads/photos/" + fileName);
        doctorRepository.save(doctor);

        return doctor.getDoctorPhoto();
    }

    /**
     * ✅ 11. حذف الطبيب وجميع البيانات المرتبطة به نهائياً (ديناميكي)
     */
    @org.springframework.transaction.annotation.Transactional
    public void deleteDoctorCompletely(String nationalId) {
        Doctor doctor = doctorRepository.findByNationalId(nationalId)
                .orElseThrow(() -> new RuntimeException("الطبيب غير موجود"));

        // Delete from all related tables
        appointmentRepository.deleteByDoctorNationalId(nationalId);
        prescriptionRepository.deleteByDoctorNationalId(nationalId);
        assistantRequestRepository.deleteByDoctorNationalId(nationalId);
        workingHourRepository.deleteByDoctorNationalId(nationalId);

        // Try to delete physical files if they exist
        try {
            if (doctor.getDoctorPhoto() != null && !doctor.getDoctorPhoto().isEmpty()) {
                Path photoPath = Paths.get(doctor.getDoctorPhoto().replaceFirst("^/", ""));
                Files.deleteIfExists(photoPath);
            }
            if (doctor.getCoverPhoto() != null && !doctor.getCoverPhoto().isEmpty()) {
                Path coverPath = Paths.get(doctor.getCoverPhoto().replaceFirst("^/", ""));
                Files.deleteIfExists(coverPath);
            }
            if (doctor.getDoctorCertificate() != null && !doctor.getDoctorCertificate().isEmpty()) {
                Path certPath = Paths.get(doctor.getDoctorCertificate().replaceFirst("^/", ""));
                Files.deleteIfExists(certPath);
            }
        } catch (Exception e) {
            System.err.println("Failed to delete doctor files: " + e.getMessage());
        }

        // Finally, delete the doctor record itself
        doctorRepository.delete(doctor);
    }
}
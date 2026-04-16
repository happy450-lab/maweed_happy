package com.example.demo.DTO;

import com.example.demo.Doctor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ✅ DoctorResponseDTO — يُستخدم في كل الـ GET endpoints عوضاً عن كيان Doctor المباشر.
 * يضمن عدم تسريب البيانات الحساسة مثل: password, activeToken, nationalId.
 */
@Data
public class DoctorResponseDTO {

    private Long id;
    private String nameDoctor;
    private String specialization;
    private String location;
    private String doctorPhoto;
    private String coverPhoto;
    private String googleMapsLink;
    private String aboutDoctor;
    private String qualifications;
    private String clinicPhone;
    // 🔒 specialAccessCode و phoneNumberDoctor محذوفان من الـ public response
    // بيتوجدا في DoctorPrivateDTO بس، للمستخدمين المصادق عليهم
    private Double checkupPrice;
    private Double recheckPrice;
    private Double averageRating;
    private Integer totalReviews;

    /**
     * Factory method لتحويل كيان Doctor إلى DTO آمن
     */
    public static DoctorResponseDTO from(Doctor doctor) {
        DoctorResponseDTO dto = new DoctorResponseDTO();
        dto.setId(doctor.getId());
        dto.setNameDoctor(doctor.getNameDoctor());
        dto.setSpecialization(doctor.getSpecialization());
        dto.setLocation(doctor.getLocation());
        dto.setDoctorPhoto(doctor.getDoctorPhoto());
        dto.setCoverPhoto(doctor.getCoverPhoto());
        dto.setGoogleMapsLink(doctor.getGoogleMapsLink());
        dto.setAboutDoctor(doctor.getAboutDoctor());
        dto.setQualifications(doctor.getQualifications());
        dto.setClinicPhone(doctor.getClinicPhone());
        // 🔒 محذوفان عن عمد: specialAccessCode و phoneNumberDoctor
        dto.setCheckupPrice(doctor.getCheckupPrice());
        dto.setRecheckPrice(doctor.getRecheckPrice());
        dto.setAverageRating(doctor.getAverageRating());
        dto.setTotalReviews(doctor.getTotalReviews());
        return dto;
    }
}

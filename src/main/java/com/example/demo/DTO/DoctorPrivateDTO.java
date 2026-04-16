package com.example.demo.DTO;

import com.example.demo.Doctor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ✅ DoctorPrivateDTO — يُستخدم في الـ endpoints المحمية بتوكن (الطبيب / المساعد).
 * يحتوي على specialAccessCode و phoneNumberDoctor اللي يحتاجهم المساعد في لوحة التحكم.
 * يحجب فقط: password, activeToken, nationalId.
 */
@Data
public class DoctorPrivateDTO {

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
    private String specialAccessCode;    // ✅ مطلوب للمساعد (QR Code)
    private String phoneNumberDoctor;    // ✅ مطلوب للتواصل الداخلي
    private Double checkupPrice;
    private Double recheckPrice;
    private boolean enabled;
    private boolean approved;
    private LocalDateTime subscriptionEndDate;

    /**
     * Factory method لتحويل كيان Doctor إلى DTO للمستخدمين المصادق عليهم
     */
    public static DoctorPrivateDTO from(Doctor doctor) {
        DoctorPrivateDTO dto = new DoctorPrivateDTO();
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
        dto.setSpecialAccessCode(doctor.getSpecialAccessCode());
        dto.setPhoneNumberDoctor(doctor.getPhoneNumberDoctor());
        dto.setCheckupPrice(doctor.getCheckupPrice());
        dto.setRecheckPrice(doctor.getRecheckPrice());
        dto.setEnabled(doctor.isEnabled());
        dto.setApproved(doctor.isApproved());
        dto.setSubscriptionEndDate(doctor.getSubscriptionEndDate());
        return dto;
    }
}

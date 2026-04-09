package com.example.demo.DTO;

import com.example.demo.Doctor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ✅ DoctorAdminDTO — يُستخدم في لوحة تحكم الأدمن فقط.
 * يحتوي على nationalId (يحتاجه الأدمن للإدارة) لكن يحجب: password, activeToken.
 */
@Data
public class DoctorAdminDTO {

    private Long id;
    private String nationalId;        // الأدمن يحتاجه لعمليات الموافقة والحذف
    private String nameDoctor;
    private String phoneNumberDoctor;
    private String specialization;
    private String location;
    private String doctorPhoto;
    private String coverPhoto;
    private String doctorCertificate; // الأدمن قد يحتاج مراجعة الشهادة
    private String specialAccessCode;
    private String aboutDoctor;
    private String qualifications;
    private Double checkupPrice;
    private Double recheckPrice;
    private boolean enabled;
    private boolean approved;
    private LocalDateTime subscriptionEndDate;

    /**
     * Factory method لتحويل كيان Doctor إلى DTO مناسب للأدمن
     */
    public static DoctorAdminDTO from(Doctor doctor) {
        DoctorAdminDTO dto = new DoctorAdminDTO();
        dto.setId(doctor.getId());
        dto.setNationalId(doctor.getNationalId());
        dto.setNameDoctor(doctor.getNameDoctor());
        dto.setPhoneNumberDoctor(doctor.getPhoneNumberDoctor());
        dto.setSpecialization(doctor.getSpecialization());
        dto.setLocation(doctor.getLocation());
        dto.setDoctorPhoto(doctor.getDoctorPhoto());
        dto.setCoverPhoto(doctor.getCoverPhoto());
        dto.setDoctorCertificate(doctor.getDoctorCertificate());
        dto.setSpecialAccessCode(doctor.getSpecialAccessCode());
        dto.setAboutDoctor(doctor.getAboutDoctor());
        dto.setQualifications(doctor.getQualifications());
        dto.setCheckupPrice(doctor.getCheckupPrice());
        dto.setRecheckPrice(doctor.getRecheckPrice());
        dto.setEnabled(doctor.isEnabled());
        dto.setApproved(doctor.isApproved());
        dto.setSubscriptionEndDate(doctor.getSubscriptionEndDate());
        return dto;
    }
}

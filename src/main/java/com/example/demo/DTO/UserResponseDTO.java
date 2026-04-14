package com.example.demo.DTO;

import com.example.demo.Role;
import com.example.demo.User;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ✅ UserResponseDTO — يُستخدم في كل الـ responses المتعلقة بالمريض.
 * يحجب الحقول الحساسة: password, activeToken.
 */
@Data
public class UserResponseDTO {

    private Long id;
    private String fullName;
    private String nationalId;
    private String phoneNumber;
    private Role role;
    private boolean enabled;
    private LocalDateTime createDate;
    private int noShowCount;
    
    // Medical Profile
    private String bloodType;
    private Double weight;
    private Double height;
    private Integer age;
    private String chronicDiseases;
    private String allergies;

    /**
     * Factory method لتحويل كيان User إلى DTO آمن
     */
    public static UserResponseDTO from(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setNationalId(user.getNationalId());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRole(user.getRole());
        dto.setEnabled(user.isEnabled());
        dto.setCreateDate(user.getCreateDate());
        dto.setNoShowCount(user.getNoShowCount());
        
        dto.setBloodType(user.getBloodType());
        dto.setWeight(user.getWeight());
        dto.setHeight(user.getHeight());
        dto.setAge(user.getAge());
        dto.setChronicDiseases(user.getChronicDiseases());
        dto.setAllergies(user.getAllergies());
        
        return dto;
    }
}

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
        return dto;
    }
}

package com.example.demo.DTO;

import com.example.demo.Role;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserDTO {
 @NotBlank(message = "الاسم مينفعش يكون فاضي")
@Column(nullable = false)
    private String fullName;
    private String nationalId;
    private String phoneNumber;
    private String password;

    private Role role;




}

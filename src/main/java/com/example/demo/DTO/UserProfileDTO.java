package com.example.demo.DTO;

import lombok.Data;

@Data
public class UserProfileDTO {
    private String bloodType;
    private Double weight;
    private Double height;
    private Integer age;
    private String chronicDiseases;
    private String allergies;
}

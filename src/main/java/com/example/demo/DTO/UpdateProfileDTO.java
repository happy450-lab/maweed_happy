package com.example.demo.DTO;

import lombok.Data;

@Data
public class UpdateProfileDTO {
    private String location;
    private String specialization;
    private Double checkupPrice;
    private Double recheckPrice;
    private String googleMapsLink;
    private String aboutDoctor;
    private String qualifications;
    private String clinicPhone;
}

package com.example.demo.DTO;

import com.example.demo.domain.AssistantRequest;
import lombok.Data;

/**
 * ✅ AssistantResponseDTO — يُستخدم في عرض بيانات المساعدين.
 * يحجب الحقل الحساس: activeToken.
 */
@Data
public class AssistantResponseDTO {

    private Long id;
    private String doctorNationalId;
    private String assistantNationalId;
    private String doctorCode;
    private String doctorName;
    private String assistantName;
    private String status;

    /**
     * Factory method لتحويل كيان AssistantRequest إلى DTO آمن
     */
    public static AssistantResponseDTO from(AssistantRequest req) {
        AssistantResponseDTO dto = new AssistantResponseDTO();
        dto.setId(req.getId());
        dto.setDoctorNationalId(req.getDoctorNationalId());
        dto.setAssistantNationalId(req.getAssistantNationalId());
        dto.setDoctorCode(req.getDoctorCode());
        dto.setDoctorName(req.getDoctorName());
        dto.setAssistantName(req.getAssistantName());
        dto.setStatus(req.getStatus());
        return dto;
    }
}

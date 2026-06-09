package com.example.project_.ELECTRONIC_OFFICE.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequestDTO {
    private String token;
    private String companyId;
    private String title;
    private String description;
    private String requestType;
//    private String requestData; // JSON data (nếu cần)
    private String note;
}
package com.example.project_.ELECTRONIC_OFFICE.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateApprovalRequestDTO {
    private String title;
    private String description;
    private String requestType;
    private String note;
}
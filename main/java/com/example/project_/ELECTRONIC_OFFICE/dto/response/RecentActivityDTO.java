package com.example.project_.ELECTRONIC_OFFICE.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityDTO {
    private String id;
    private String type; // "REQUEST_CREATED", "REQUEST_APPROVED", "REQUEST_REJECTED", "USER_CREATED"
    private String title;
    private String userName;
    private String userRole;
    private String status;
    private OffsetDateTime createdAt;
    private String formattedTime;
}

package com.example.project_.ELECTRONIC_OFFICE.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ApprovalDetail {
    private String nodeId;
    private String action;        // "Đồng ý" hoặc "Từ chối"
    private String approvedBy;    // userId hoặc username
    private LocalDateTime approvedAt;
    private String note;          // Ghi chú duyệt
    private String ipAddress;     // Địa chỉ IP khi duyệt
    private Map<String, Object> additionalData;
}

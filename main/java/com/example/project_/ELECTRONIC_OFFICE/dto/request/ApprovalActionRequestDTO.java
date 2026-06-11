package com.example.project_.ELECTRONIC_OFFICE.dto.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalActionRequestDTO {
    private String actionId;           // ID của approval_action cần xử lý
    private String action;             // 'APPROVED', 'REJECTED', 'REQUEST_CHANGES'
    private String rejectionReason;    // Lý do từ chối (nếu REJECTED)
    private String changeRequestNote;  // Nội dung yêu cầu chỉnh sửa (nếu REQUEST_CHANGES)
    private String note;               // Ghi chú thêm
}
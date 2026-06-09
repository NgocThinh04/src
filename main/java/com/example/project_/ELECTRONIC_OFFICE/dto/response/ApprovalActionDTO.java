package com.example.project_.ELECTRONIC_OFFICE.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalActionDTO {
    private UUID id;
    private Integer stepOrder;
    private String stepName;
    private String approvalType; // SINGLE, ALL
    private String action; // PENDING, APPROVED, REJECTED

    // Thông tin người duyệt
    private UUID approverId;
    private String approverName;
    private String rejectionReason;
    private String note;
    private OffsetDateTime approvedAt;

    // Thông tin request (để FE biết đang xử lý request nào)
    private UUID requestId;
    private String requestCode;
    private String requestTitle;

    // Thêm trường này để FE biết có cần ẩn nút duyệt không
    private Boolean canApprove; // Người dùng hiện tại có quyền duyệt action này không
}
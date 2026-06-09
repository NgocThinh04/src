package com.example.project_.ELECTRONIC_OFFICE.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalResponseDTO {
    private UUID id;
    private String requestCode;
    private String title;
    private String description;
    private String requestType;
    private String status;
    private String note;

    // Thông tin người gửi
    private UUID requesterId;
    private String requesterName;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Chi tiết các bước duyệt
    private List<ApprovalActionDTO> actions;

    // Thông tin bước hiện tại (để FE biết đang ở bước nào)
    private Integer currentStepOrder;
    private String currentStepName;

    // Trạng thái tổng thể cho FE
    private Boolean isCompleted; // true nếu APPROVED hoặc REJECTED
    private Boolean isPending;   // true nếu đang chờ duyệt
}
package com.example.project_.ELECTRONIC_OFFICE.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowStep {
    private Integer stepOrder;      // Thứ tự bước (1, 2, 3...)
    private String stepName;        // Tên bước (Trưởng phòng, Giám đốc...)
    private String assignedRole;    // Vai trò được gán (TRUONG_PHONG, GIAM_DOC...)
    private String approvalType;    // Loại duyệt: SINGLE (xanh) hoặc ALL (đỏ)
}
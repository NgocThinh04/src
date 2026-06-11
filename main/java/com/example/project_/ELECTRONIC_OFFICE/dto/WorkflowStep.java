package com.example.project_.ELECTRONIC_OFFICE.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class WorkflowStep {
    private Integer stepOrder;           // Thứ tự bước (1, 2, 3...)
    private String stepName;             // Tên bước (Trưởng phòng, Giám đốc...)
    private String assignedRole;         // Vai trò được gán (TRUONG_PHONG, GIAM_DOC...)
    private String approvalType;         // Loại duyệt: SINGLE (1 người) hoặc ALL (nhiều người)
    private boolean waitingForAction;    // Đang chờ hành động duyệt/từ chối

    // ===== CÁC FIELD BỔ SUNG =====

    private String nodeId;               // ID của node trong workflow
    private Boolean approved;            // true: đã duyệt, false: đã từ chối, null: chưa xử lý
    private String actionTaken;          // Hành động đã thực hiện
    private List<String> possibleActions; // Danh sách hành động có thể
    private String approvedBy;           // ID người duyệt
    private LocalDateTime approvedAt;    // Thời gian duyệt
    private String approvalNote;         // Ghi chú duyệt

    // Node tiếp theo cho từng loại kết nối
    private String nextNodeIfConditional; // Node tiếp theo nếu là conditional (xanh)
    private String nextNodeIfParallel;    // Node tiếp theo nếu là parallel (đỏ)

    // Giữ lại cho tương thích cũ (có thể bỏ nếu không dùng)
    private String nextNodeIfApproved;    // Node tiếp theo nếu approve
    private String nextNodeIfRejected;    // Node tiếp theo nếu reject

    private LocalDateTime deadline;       // Thời gian tối đa chờ duyệt
    private boolean isEndStep;            // Có phải bước kết thúc
    private StepStatus status;            // Trạng thái của step
    private String connectionType;        // Loại kết nối: conditional, parallel
    private Map<String, Object> conditions; // Điều kiện kèm theo

    // Constructor mặc định
    public WorkflowStep() {
        this.possibleActions = new ArrayList<>();
        this.conditions = new HashMap<>();
        this.status = StepStatus.PENDING;
        this.waitingForAction = true;
    }

    // Helper methods
    public boolean isApproved() {
        return Boolean.TRUE.equals(approved);
    }

    public boolean isRejected() {
        return Boolean.FALSE.equals(approved);
    }

    public boolean isCompleted() {
        return status == StepStatus.APPROVED || status == StepStatus.REJECTED;
    }
}


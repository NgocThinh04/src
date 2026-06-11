package com.example.project_.ELECTRONIC_OFFICE.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;
@Entity
@Table(name = "approval_actions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalAction {

    @Id
    @Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;

    @Column(name = "approver_id")
    private UUID approverId;

    @Column(name = "approver_name", length = 255)
    private String approverName;

    @Column(name = "approval_type", nullable = false, length = 20)
    private String approvalType;

    @Column(name = "action", length = 20)
    @Builder.Default
    private String action = "PENDING";

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createAt;
    @Column(name = "node_id", length = 100)
    private String nodeId;

    @Column(name = "possible_actions", length = 255)
    private String possibleActions; // Lưu "Đồng ý,Từ chối"

    @Column(name = "action_status", length = 20)
    @Builder.Default
    private String actionStatus = "PENDING";
    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createAt == null) {
            createAt = OffsetDateTime.now();
        }
        if (action == null) {
            action = "PENDING";
        }
    }
}

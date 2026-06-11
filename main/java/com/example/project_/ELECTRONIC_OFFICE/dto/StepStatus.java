package com.example.project_.ELECTRONIC_OFFICE.dto;

// Enum cho trạng thái step
public enum StepStatus {
    PENDING,    // Chờ duyệt
    APPROVED,   // Đã duyệt
    REJECTED,   // Đã từ chối
    SKIPPED,    // Bị bỏ qua (do điều kiện)
    EXPIRED     // Hết hạn chờ duyệt
}

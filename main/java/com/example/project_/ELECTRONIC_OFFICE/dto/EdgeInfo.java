package com.example.project_.ELECTRONIC_OFFICE.dto;

import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class EdgeInfo {
    private String target;           // ID của node đích
    private String edgeType;         // APPROVE hoặc REJECT (hoặc SEQUENTIAL, CONDITIONAL)
    private String strokeColor;      // #3b82f6 (xanh - approve) hoặc #ef4444 (đỏ - reject)
    private String label;            // "Đồng ý", "Từ chối" (optional)

    // Constructor cho tiện
    public EdgeInfo(String target, String edgeType, String strokeColor) {
        this.target = target;
        this.edgeType = edgeType;
        this.strokeColor = strokeColor;
    }

    public EdgeInfo(String target, String edgeType, String strokeColor, String label) {
        this.target = target;
        this.edgeType = edgeType;
        this.strokeColor = strokeColor;
        this.label = label;
    }
}
package com.example.project_.ELECTRONIC_OFFICE.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EdgeInfo {
    private String target;
    private String edgeType; // APPROVE hoặc REJECT
    private String strokeColor; // #3b82f6 (xanh) hoặc #ef4444 (đỏ)
}
package com.example.project_.ELECTRONIC_OFFICE.dto.response;

import lombok.Data;

import java.util.UUID;

@Data
public class PositionResponse {
    private UUID positionId;
    private String positionName;
    private UUID companyId;
}
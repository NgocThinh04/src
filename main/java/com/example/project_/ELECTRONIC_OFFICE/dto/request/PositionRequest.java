package com.example.project_.ELECTRONIC_OFFICE.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class PositionRequest {
    private String positionName;
    private UUID companyId;
}
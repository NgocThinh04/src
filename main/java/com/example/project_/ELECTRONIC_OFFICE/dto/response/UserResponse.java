package com.example.project_.ELECTRONIC_OFFICE.dto.response;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class UserResponse {
    private UUID userId;
    private String name;
    private String userName;
    private String email;
    private String phone;
    private String address;
    private String role;
    private String position;
    private String status;
    private UUID companyId;
    private String companyCode;
    private OffsetDateTime createAt;
    private OffsetDateTime updateAt;
}
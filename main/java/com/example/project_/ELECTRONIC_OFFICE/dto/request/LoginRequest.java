package com.example.project_.ELECTRONIC_OFFICE.dto.request;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private String companyCode;
}
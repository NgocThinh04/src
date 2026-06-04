package com.example.project_.ELECTRONIC_OFFICE.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class UserRequest {
    private String name;
    private String userName;
    private String email;
    private String password;
    private String phone;
    private String address;
    private String position;
    private String status;
    private UUID companyId;
    private String companyCode;
}
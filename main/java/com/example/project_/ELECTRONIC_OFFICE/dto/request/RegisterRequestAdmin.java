package com.example.project_.ELECTRONIC_OFFICE.dto.request;

import lombok.Data;

@Data
public class RegisterRequestAdmin {
    private String username;
    private String email;
    private String password;
    private String nameCompany;
    private String address;
    private String numberPhone;
}

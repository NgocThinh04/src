package com.example.project_.ELECTRONIC_OFFICE.mapper;

import com.example.project_.ELECTRONIC_OFFICE.dto.request.UserRequest;
import com.example.project_.ELECTRONIC_OFFICE.dto.response.UserResponse;
import com.example.project_.ELECTRONIC_OFFICE.entity.Users;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserMapper {

    private final PasswordEncoder passwordEncoder;

    /**
     * Chuyển từ Request DTO sang Entity
     */
    public Users toEntity(UserRequest request) {
        if (request == null) {
            return null;
        }

        Users user = new Users();
        user.setName(request.getName());
        user.setUserName(request.getUserName() != null ? request.getUserName() : request.getEmail());
        user.setEmail(request.getEmail());
        user.setAddress(request.getAddress());
        user.setNumber(request.getPhone());
        user.setPosition(request.getPosition());
        user.setStatus(request.getStatus());
        user.setCompanyId(request.getCompanyId());
        user.setCompanyCode(request.getCompanyCode());

        // Mã hóa password nếu có
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassWord(passwordEncoder.encode(request.getPassword()));
        }

        return user;
    }

    /**
     * Chuyển từ Entity sang Response DTO
     */
    public UserResponse toResponse(Users user) {
        if (user == null) {
            return null;
        }

        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setName(user.getName());
        response.setUserName(user.getUserName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getNumber());
        response.setAddress(user.getAddress());
        response.setRole(user.getRole());
        response.setPosition(user.getPosition());
        response.setStatus(user.getStatus());
        response.setCompanyId(user.getCompanyId());
        response.setCompanyCode(user.getCompanyCode());
        response.setCreateAt(user.getCreateAt());
        response.setUpdateAt(user.getUpdateAt());

        return response;
    }

    /**
     * Cập nhật Entity từ Request DTO (dùng cho update)
     */
    public void updateEntity(UserRequest request, Users user) {
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getUserName() != null) {
            user.setUserName(request.getUserName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getPhone() != null) {
            user.setNumber(request.getPhone());
        }
        if (request.getPosition() != null) {
            user.setPosition(request.getPosition());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        if (request.getCompanyId() != null) {
            user.setCompanyId(request.getCompanyId());
        }
        if (request.getCompanyCode() != null) {
            user.setCompanyCode(request.getCompanyCode());
        }

        // Nếu có password mới, encode và cập nhật
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassWord(passwordEncoder.encode(request.getPassword()));
        }
    }
}
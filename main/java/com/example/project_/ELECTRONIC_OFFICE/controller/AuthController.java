package com.example.project_.ELECTRONIC_OFFICE.controller;

import com.example.project_.ELECTRONIC_OFFICE.dto.request.LoginRequest;
import com.example.project_.ELECTRONIC_OFFICE.dto.request.RegisterRequestAdmin;
import com.example.project_.ELECTRONIC_OFFICE.entity.Users;
import com.example.project_.ELECTRONIC_OFFICE.service.AuthService;
import com.example.project_.ELECTRONIC_OFFICE.service.JwtService;
import com.example.project_.ELECTRONIC_OFFICE.service.admin.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    final AuthService authService;
    final AdminService adminService;
    final JwtService jwtService;
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            Map<String, String> response = authService.login(loginRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid credentials"));
        }
    }
    @PostMapping("/register-company")
    public ResponseEntity<?> registerCompany(@RequestBody RegisterRequestAdmin registerRequestAdmin) {
        System.out.println(registerRequestAdmin.getPassword());
        adminService.registerAdmin(registerRequestAdmin);
        return ResponseEntity.ok("Đăng ký thành công");
    }
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Users user) {
        try {
            Map<String, String> response = authService.register(user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authorization) {
        try {
            String token = authorization.substring(7); // Bỏ "Bearer "
            String username = jwtService.extractUsername(token);
            authService.logout(username);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Logout successful");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(400).body(error);
        }
    }
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        try {
            Map<String, String> response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(401).body(error);
        }
    }

    @GetMapping("/user-info")
    public ResponseEntity<?> getUserInfo() {
        return ResponseEntity.ok(Map.of("message", "This is protected data"));
    }
}
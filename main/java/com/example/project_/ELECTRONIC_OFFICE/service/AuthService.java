// service/AuthService.java
package com.example.project_.ELECTRONIC_OFFICE.service;

import com.example.project_.ELECTRONIC_OFFICE.dto.request.LoginRequest;
import com.example.project_.ELECTRONIC_OFFICE.entity.Users;
import com.example.project_.ELECTRONIC_OFFICE.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final RedisService redisService;
    private PasswordEncoder passwordEncoder;

    // Đăng nhập và tạo token
    public Map<String, String> login(LoginRequest loginRequest) {
        // Xác thực username và password
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        Users user = userRepository.findByUserName(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Nếu xác thực thành công, tạo token
        UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getUsername());

        // Tạo access token và refresh token
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // Lưu refresh token vào Redis
        redisService.saveRefreshToken(loginRequest.getUsername(), refreshToken, jwtService.REFRESH_EXPIRATION_TIME);

        Map<String, String> response = new HashMap<>();
//        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("token", accessToken);
        response.put("username", loginRequest.getUsername());
        response.put("role", user.getRole());

        if(user.getCompanyId() != null) {
            response.put("companyId", user.getCompanyId().toString());
        }
        if (user.getCompanyCode() != null) {
            response.put("companyCode", user.getCompanyCode());
        }
        if (user.getCreateAt() != null) {
            response.put("create_at", user.getCreateAt().toString());
        }
        response.put("address", user.getAddress());
        response.put("number_phone", user.getNumber());
        response.put("email", user.getEmail());
        response.put("message", "Login successful");

        return response;
    }

    // Refresh token
    public Map<String, String> refreshToken(String refreshToken) {
        // Validate refresh token
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String username = jwtService.extractUsername(refreshToken);

        // Kiểm tra refresh token trong Redis
        String storedRefreshToken = redisService.getRefreshToken(username);
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new RuntimeException("Refresh token not found or mismatched");
        }

        // Tạo user details mới
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // Tạo access token mới
        String newAccessToken = jwtService.generateAccessToken(userDetails);

        Map<String, String> response = new HashMap<>();
        response.put("accessToken", newAccessToken);
        response.put("token", newAccessToken); // Giữ lại cho tương thích

        return response;
    }

    // Logout - xóa refresh token khỏi Redis
    public void logout(String username) {
        redisService.deleteRefreshToken(username);
        log.info("User logged out: {}", username);
    }

    // Đăng ký user mới
    public Map<String, String> register(Users user) {
        // Mã hóa password trước khi lưu
        user.setPassWord(passwordEncoder.encode(user.getPassWord()));
        user.setRole("ROLE_USER");
        userRepository.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "User registered successfully");
        return response;
    }
}